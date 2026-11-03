package org.group3.tutorlink.features.chatbot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.chatbot.ai.AIClient;
import org.group3.tutorlink.features.chatbot.ai.AssistantTurn;
import org.group3.tutorlink.features.chatbot.ai.PostSearchFilters;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.chatbot.dto.request.ChatMessageRequest;
import org.group3.tutorlink.features.chatbot.dto.request.UserLocation;
import org.group3.tutorlink.features.chatbot.dto.response.*;
import org.group3.tutorlink.features.chatbot.entity.ChatConversation;
import org.group3.tutorlink.features.chatbot.entity.ChatMessage;
import org.group3.tutorlink.features.chatbot.enums.ChatIntent;
import org.group3.tutorlink.features.chatbot.enums.ChatRole;
import org.group3.tutorlink.features.chatbot.enums.ChatbotMessageKey;
import org.group3.tutorlink.features.chatbot.exception.ConversationNotFoundException;
import org.group3.tutorlink.features.chatbot.mapper.ChatbotMapper;
import org.group3.tutorlink.features.chatbot.messages.ChatbotMessages;
import org.group3.tutorlink.features.chatbot.repository.ChatConversationRepository;
import org.group3.tutorlink.features.chatbot.repository.ChatMessageRepository;
import org.group3.tutorlink.features.chatbot.service.ChatbotService;
import org.group3.tutorlink.features.chatbot.service.PostSearcher;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final String HISTORY_LINE_SEPARATOR = "\n";
    private static final String HISTORY_ROLE_SEPARATOR = ": ";

    private final AIClient aiClient;
    private final PostSearcher postSearcher;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatbotMapper chatbotMapper;
    private final AppUtil appUtil;
    private final ChatbotProperties properties;
    private final ChatbotMessages messages;

    @Override
    public ChatResponse sendMessage(ChatMessageRequest request, UUID userId) {
        boolean isGuest = (userId == null);

        // Guest không lưu hội thoại, mỗi câu hỏi độc lập.
        ChatConversation conversation = isGuest ? null : findOrCreateConversation(request, userId);
        UUID conversationId = isGuest ? null : conversation.getId();
        String history = isGuest ? messages.get(ChatbotMessageKey.HISTORY_EMPTY) : loadHistory(conversation);

        // Bước 1: nhờ AI hiểu người dùng muốn gì.
        AssistantTurn turn = aiClient.analyze(history, request.getMessage());

        // Bước 2: tùy ý định mà trả lời. Việc tìm bài đăng do code làm, không tin AI.
        ChatResponse reply = replyTo(turn, request, userId, conversationId);

        // Bước 3: thành viên thì lưu lại cuộc trò chuyện (kèm các bài đã gợi ý để tải lại lịch sử vẫn thấy thẻ bài).
        if (!isGuest) {
            saveExchange(conversation, request.getMessage(), reply.getMessage(), reply.getPosts());
        }
        return reply;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations() {
        return conversationRepository
                .findAllByUser_IdOrderByLastMessageAtDesc(appUtil.userIdFromAuthentication())
                .stream()
                .map(chatbotMapper::toConversationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID conversationId) {
        ChatConversation conversation = findOwnedConversation(conversationId, appUtil.userIdFromAuthentication());
        List<ChatMessage> history = messageRepository.findAllByConversation_IdOrderByCreatedAtAscIdAsc(conversation.getId());

        // Lấy 1 lần toàn bộ bài đăng được nhắc tới trong hội thoại, tránh query từng tin nhắn
        Set<UUID> postIds = history.stream()
                .flatMap(message -> message.getPostIds().stream())
                .collect(Collectors.toSet());
        Map<UUID, PostSummaryResponse> postsById = postSearcher.findByIds(postIds);

        return history.stream()
                .map(message -> toMessageResponse(message, postsById))
                .toList();
    }

    @Override
    @Transactional
    public void deleteConversation(UUID conversationId) {
        ChatConversation conversation = findOwnedConversation(conversationId, appUtil.userIdFromAuthentication());
        messageRepository.deleteAllByConversation_Id(conversation.getId());
        conversationRepository.delete(conversation);
    }

    // ---------- helpers ----------

    private ChatResponse replyTo(AssistantTurn turn, ChatMessageRequest request, UUID userId, UUID conversationId) {
        if (turn.getIntent() != ChatIntent.SEARCH_POSTS) {
            String message = StringUtils.hasText(turn.getMessage())
                    ? turn.getMessage()
                    : messages.get(ChatbotMessageKey.NOT_UNDERSTOOD);
            return buildResponse(conversationId, turn.getIntent(), message, List.of());
        }
        if (!hasSubject(turn.getFilters())) {
            return buildResponse(conversationId, ChatIntent.ASK_CLARIFICATION,
                    messages.get(ChatbotMessageKey.ASK_SUBJECT), List.of());
        }

        PostSearchResult result = searchPosts(turn.getFilters(), request, userId);
        return buildResponse(conversationId, ChatIntent.SEARCH_POSTS,
                describeResult(result, userId == null), result.getPosts());
    }

    private ChatResponse buildResponse(UUID conversationId, ChatIntent intent, String message,
                                       List<PostSummaryResponse> posts) {
        return ChatResponse.builder()
                .conversationId(conversationId)
                .intent(intent)
                .message(message)
                .posts(posts)
                .build();
    }

    private boolean hasSubject(PostSearchFilters filters) {
        return filters != null && StringUtils.hasText(filters.getSubjectName());
    }

    // Guest được xem ít bài hơn thành viên (giới hạn nằm trong application.properties).
    private PostSearchResult searchPosts(PostSearchFilters filters, ChatMessageRequest request, UUID userId) {
        int limit = (userId == null) ? properties.getGuestPostLimit() : properties.getMemberPostLimit();
        long startedAt = System.currentTimeMillis();

        PostSearchResult result = postSearcher.search(filters, resolveLocation(request, userId), limit);

        log.info("Chatbot tìm bài với {} -> {} bài (nới điều kiện: {}), mất {} ms",
                filters, result.getPosts().size(), result.isRelaxed(), System.currentTimeMillis() - startedAt);
        return result;
    }

    private void saveExchange(ChatConversation conversation, String question, String answer,
                              List<PostSummaryResponse> suggestedPosts) {
        saveMessage(conversation, ChatRole.USER, question, List.of());
        saveMessage(conversation, ChatRole.ASSISTANT, answer,
                suggestedPosts.stream().map(PostSummaryResponse::getId).toList());
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);
    }

    // Ưu tiên vị trí user gửi lên, không có thì thành viên dùng địa chỉ hồ sơ, guest thì không biết.
    private UserLocation resolveLocation(ChatMessageRequest request, UUID userId) {
        if (request.getLocation() != null && request.getLocation().isKnown()) {
            return request.getLocation();
        }
        if (userId == null) {
            return UserLocation.unknown();
        }
        return userRepository.findById(userId)
                .map(user -> UserLocation.fromAddress(user.getAddress()))
                .orElse(UserLocation.unknown());
    }

    private ChatConversation findOrCreateConversation(ChatMessageRequest request, UUID userId) {
        if (request.getConversationId() != null) {
            return findOwnedConversation(request.getConversationId(), userId);
        }
        String message = request.getMessage();
        int maxLength = properties.getConversationTitleMaxLength();
        String title = message.length() > maxLength ? message.substring(0, maxLength) : message;

        return conversationRepository.save(ChatConversation.builder()
                .id(appUtil.generateUUID())
                .user(userRepository.findById(userId).orElseThrow(UserNotFoundException::new))
                .title(title)
                .lastMessageAt(Instant.now())
                .build());
    }

    //Chỉ lấy được hội thoại của chính mình, người khác sẽ nhận 404.
    private ChatConversation findOwnedConversation(UUID conversationId, UUID userId) {
        return conversationRepository.findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private String loadHistory(ChatConversation conversation) {
        List<ChatMessage> history = messageRepository.findByConversation_IdOrderByCreatedAtDescIdDesc(
                conversation.getId(), PageRequest.of(0, properties.getHistoryLimit()));
        if (history.isEmpty()) {
            return messages.get(ChatbotMessageKey.HISTORY_EMPTY);
        }
        Collections.reverse(history); // lấy mới nhất trước nên đảo lại cho đúng thứ tự cũ -> mới
        return history.stream()
                .map(m -> roleLabel(m.getRole()) + HISTORY_ROLE_SEPARATOR + m.getContent())
                .collect(Collectors.joining(HISTORY_LINE_SEPARATOR));
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message, Map<UUID, PostSummaryResponse> postsById) {
        List<PostSummaryResponse> posts = message.getPostIds().stream()
                .map(postsById::get)        // bài đã đóng / bị gỡ sẽ là null
                .filter(Objects::nonNull)
                .toList();
        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .posts(posts)
                .build();
    }

    private String roleLabel(ChatRole role) {
        return messages.get(role == ChatRole.USER ? ChatbotMessageKey.HISTORY_USER : ChatbotMessageKey.HISTORY_ASSISTANT);
    }

    private void saveMessage(ChatConversation conversation, ChatRole role, String content, List<UUID> postIds) {
        messageRepository.save(ChatMessage.builder()
                .id(appUtil.generateUUID())
                .conversation(conversation)
                .role(role)
                .content(content)
                .postIds(new ArrayList<>(postIds))
                .build());
    }

    private String describeResult(PostSearchResult result, boolean isGuest) {
        int count = result.getPosts().size();
        if (count == 0) {
            return messages.get(ChatbotMessageKey.NO_RESULT);
        }
        if (result.isRelaxed()) {
            return messages.get(ChatbotMessageKey.RESULT_RELAXED);
        }
        return isGuest
                ? messages.get(ChatbotMessageKey.RESULT_FOR_GUEST)
                : messages.get(ChatbotMessageKey.RESULT_FOR_MEMBER, count);
    }
}