package org.group3.tutorlink.features.chatbot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.chatbot.ai.AssistantTurn;
import org.group3.tutorlink.features.chatbot.ai.PostSearchFilters;
import org.group3.tutorlink.features.chatbot.ai.AIClient;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.chatbot.dto.request.ChatMessageRequest;
import org.group3.tutorlink.features.chatbot.dto.request.UserLocation;
import org.group3.tutorlink.features.chatbot.dto.response.ChatMessageResponse;
import org.group3.tutorlink.features.chatbot.dto.response.ChatResponse;
import org.group3.tutorlink.features.chatbot.dto.response.ConversationResponse;
import org.group3.tutorlink.features.chatbot.dto.response.PostSummaryResponse;
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
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
        String history = isGuest ? messages.get(ChatbotMessageKey.HISTORY_EMPTY) : loadHistory(conversation);

        // Bước 1: nhờ AI hiểu người dùng muốn gì.
        AssistantTurn turn = aiClient.analyze(history, request.getMessage());

        // Bước 2: tùy ý định mà trả lời. Việc tìm bài đăng do code làm, không tin AI.
        ChatIntent intent = turn.getIntent();
        List<PostSummaryResponse> posts = List.of();
        String answer;

        if (intent != ChatIntent.SEARCH_POSTS) {
            answer = StringUtils.hasText(turn.getMessage())
                    ? turn.getMessage()
                    : messages.get(ChatbotMessageKey.NOT_UNDERSTOOD);
        } else if (!hasSubject(turn.getFilters())) {
            intent = ChatIntent.ASK_CLARIFICATION;
            answer = messages.get(ChatbotMessageKey.ASK_SUBJECT);
        } else {
            posts = searchPosts(turn.getFilters(), request, userId);
            answer = describeResult(posts.size(), isGuest);
        }

        // Bước 3: thành viên thì lưu lại cuộc trò chuyện.
        if (!isGuest) {
            saveExchange(conversation, request.getMessage(), answer);
        }

        return ChatResponse.builder()
                .conversationId(isGuest ? null : conversation.getId())
                .intent(intent)
                .message(answer)
                .posts(posts)
                .build();
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
        return messageRepository
                .findAllByConversation_IdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(chatbotMapper::toChatMessageResponse)
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

    private boolean hasSubject(PostSearchFilters filters) {
        return filters != null && StringUtils.hasText(filters.getSubjectName());
    }

    // Guest được xem ít bài hơn thành viên (giới hạn nằm trong application.properties).
    private List<PostSummaryResponse> searchPosts(PostSearchFilters filters, ChatMessageRequest request, UUID userId) {
        int limit = (userId == null) ? properties.getGuestPostLimit() : properties.getMemberPostLimit();
        log.info("Chatbot filters: {}", filters);
        return postSearcher.search(filters, resolveLocation(request, userId), limit);
    }

    private void saveExchange(ChatConversation conversation, String question, String answer) {
        saveMessage(conversation, ChatRole.USER, question);
        saveMessage(conversation, ChatRole.ASSISTANT, answer);
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
        List<ChatMessage> history = messageRepository.findByConversation_IdOrderByCreatedAtDesc(
                conversation.getId(), PageRequest.of(0, properties.getHistoryLimit()));
        if (history.isEmpty()) {
            return messages.get(ChatbotMessageKey.HISTORY_EMPTY);
        }
        Collections.reverse(history); // lấy mới nhất trước nên đảo lại cho đúng thứ tự cũ -> mới
        return history.stream()
                .map(m -> roleLabel(m.getRole()) + HISTORY_ROLE_SEPARATOR + m.getContent())
                .collect(Collectors.joining(HISTORY_LINE_SEPARATOR));
    }

    private String roleLabel(ChatRole role) {
        return messages.get(role == ChatRole.USER ? ChatbotMessageKey.HISTORY_USER : ChatbotMessageKey.HISTORY_ASSISTANT);
    }

    private void saveMessage(ChatConversation conversation, ChatRole role, String content) {
        messageRepository.save(ChatMessage.builder()
                .id(appUtil.generateUUID())
                .conversation(conversation)
                .role(role)
                .content(content)
                .build());
    }

    private String describeResult(int count, boolean isGuest) {
        if (count == 0) {
            return messages.get(ChatbotMessageKey.NO_RESULT);
        }
        return isGuest
                ? messages.get(ChatbotMessageKey.RESULT_FOR_GUEST)
                : messages.get(ChatbotMessageKey.RESULT_FOR_MEMBER, count);
    }
}
