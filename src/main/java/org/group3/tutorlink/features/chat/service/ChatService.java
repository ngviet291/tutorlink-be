package org.group3.tutorlink.features.chat.service;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.exception.AccessDeniedException;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.chat.dto.request.ChatCreateChanelRequest;
import org.group3.tutorlink.features.chat.dto.request.SendMessageRequest;
import org.group3.tutorlink.features.chat.dto.response.ChatChannelResponse;
import org.group3.tutorlink.features.chat.dto.response.MessageResponse;
import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.group3.tutorlink.features.chat.entity.Media;
import org.group3.tutorlink.features.chat.entity.Message;
import org.group3.tutorlink.features.chat.event.MessageSent;
import org.group3.tutorlink.features.chat.event.MessagesRead;
import org.group3.tutorlink.features.chat.exception.CannotMessageSelfException;
import org.group3.tutorlink.features.chat.exception.ChatChannelNotFound;
import org.group3.tutorlink.features.chat.exception.ChatErrorCode;
import org.group3.tutorlink.features.chat.mapper.ChatMapper;
import org.group3.tutorlink.features.chat.repository.ChatChannelRepository;
import org.group3.tutorlink.features.chat.repository.MessageRepository;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatChannelRepository chatChannelRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AppUtil appUtil;
    private final ChatMapper chatMapper;
    private static final int MAX_PAGE_SIZE = 50;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest req) {
        UUID senderId = appUtil.userIdFromAuthentication();

        if (senderId.equals(req.getReceiverId())) {
            throw new CannotMessageSelfException();
        }

        // yêu cầu bắt buộc application của sender và receiver phải tồn tại, và đã thanh toán

        User sender = userRepository.findById(senderId)
                .orElseThrow(UserNotFoundException::new);
        User receiver = userRepository.findById(req.getReceiverId())
                .orElseThrow(UserNotFoundException::new);

        validateUser(sender, receiver);

        ChatChannel channel = findChannelOrThrow(sender, receiver);
        Instant now = Instant.now();

        Message message = Message.builder()
                .id(appUtil.generateUUID())
                .content(req.getContent())
                .isRead(false)
                .sentAt(now)
                .chatChannel(channel)
                .sender(sender)
                .mediaList(new HashSet<>())
                .build();

        if (req.getMediaList() != null) {
            req.getMediaList().forEach(m -> {
                Media media = chatMapper.toMedia(m);
                media.setId(appUtil.generateUUID());
                media.setMessage(message);
                media.setUser(sender);
                message.getMediaList().add(media);
            });
        }

        messageRepository.save(message);

        channel.setLastMessageAt(now);
        chatChannelRepository.save(channel);

        MessageResponse response = chatMapper.toMessageResponse(message);
        eventPublisher.publishEvent(
                MessageSent.builder()
                        .senderId(senderId)
                        .receiverId(receiver.getId())
                        .message(response)
                        .build()
        );
        return response;
    }

    private void validateUser(User sender, User receiver) {
        boolean valid = (sender instanceof Tutor && receiver instanceof Student)
                || (sender instanceof Student && receiver instanceof Tutor);

        if (!valid) {
            throw new AccessDeniedException(ChatErrorCode.CHAT_ONLY_TUTOR_STUDENT);
        }
    }

    private ChatChannel findChannelOrThrow(User sender, User receiver) {
        return chatChannelRepository.findBetween(sender.getId(), receiver.getId())
                .orElseThrow(ChatChannelNotFound::new);
    }


    public ChatChannelResponse createNewChannel(ChatCreateChanelRequest request) {
        UUID userId = appUtil.userIdFromAuthentication();
        User sender = userRepository.getReferenceById(userId);

        //

        User receiver = userRepository.findById(request.getPartnerId()).orElseThrow(UserNotFoundException::new);

        return chatMapper.toChannelResponse(createChannel(sender, receiver), userId);
    }

    public ChatChannel createChannel(User sender, User receiver) {
        ChatChannel channel = ChatChannel.builder()
                .id(appUtil.generateUUID())
                .sender(sender)     // người khởi tạo cuộc trò chuyện
                .receiver(receiver)
                .build();
        try {
            return chatChannelRepository.saveAndFlush(channel);
        } catch (DataIntegrityViolationException e) {
            return chatChannelRepository.findBetween(sender.getId(), receiver.getId())
                    .orElseThrow(() -> e);
        }
    }


    @Transactional(readOnly = true)
    public CursorResponse<ChatChannelResponse> getAllChatChannels(UUID cursor, int size) {
        UUID userId = appUtil.userIdFromAuthentication();
        int limit = normalizeSize(size);
        Pageable page = PageRequest.of(0, limit + 1);

        List<ChatChannel> channels;
        if (cursor == null) {
            channels = chatChannelRepository.findFirstPage(userId, page);
        } else {
            ChatChannel c = getParticipatingChannel(cursor, userId);
            channels = chatChannelRepository.findNextPage(userId, c.getLastMessageAt(), c.getId(), page);
        }
        return appUtil.buildCursorResponse(channels, limit, ChatChannel::getId,
                withSummary(channels, userId));
    }

    @Transactional(readOnly = true)
    public ChatChannelResponse getChatChannelById(UUID channelId) {
        UUID userId = appUtil.userIdFromAuthentication();
        ChatChannel channel = getParticipatingChannel(channelId, userId);
        return withSummary(List.of(channel), userId).apply(channel);
    }

    // Mở chat từ profile: chưa có channel thì 404, channel sẽ được tạo khi gửi tin đầu tiên
    @Transactional(readOnly = true)
    public ChatChannelResponse getChatChannelWithUser(UUID partnerId) {
        UUID userId = appUtil.userIdFromAuthentication();
        ChatChannel channel = chatChannelRepository.findBetween(userId, partnerId)
                .orElseThrow(ChatChannelNotFound::new);
        return withSummary(List.of(channel), userId).apply(channel);
    }

    @Transactional(readOnly = true)
    public CursorResponse<MessageResponse> getAllMessages(UUID channelId, UUID cursor, int size) {
        UUID userId = appUtil.userIdFromAuthentication();
        getParticipatingChannel(channelId, userId);
        int limit = normalizeSize(size);

        List<Message> messages = messageRepository.findPage(
                channelId, cursor, PageRequest.of(0, limit + 1));

        return appUtil.buildCursorResponse(messages, limit, Message::getId, chatMapper::toMessageResponse);
    }

    @Transactional
    public void markChannelAsRead(UUID channelId) {
        UUID userId = appUtil.userIdFromAuthentication();
        ChatChannel channel = getParticipatingChannel(channelId, userId);
        int updated = messageRepository.markAllAsRead(channelId, userId);
        if (updated > 0) {
            UUID partnerId = channel.getSender().getId().equals(userId)
                    ? channel.getReceiver().getId()
                    : channel.getSender().getId();
            eventPublisher.publishEvent(
                    MessagesRead.builder()
                            .channelId(channelId)
                            .readerId(userId)
                            .partnerId(partnerId)
                            .readAt(Instant.now())
                            .build()
            );
        }
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return messageRepository.countUnreadByUser(appUtil.userIdFromAuthentication());
    }

    private ChatChannel getParticipatingChannel(UUID channelId, UUID userId) {
        ChatChannel channel = chatChannelRepository.findWithParticipantsById(channelId)
                .orElseThrow(ChatChannelNotFound::new);
        if (!channel.getSender().getId().equals(userId) && !channel.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException(ChatErrorCode.NOT_PARTICIPATING);
        }
        return channel;
    }

    /** Map channel -> response kèm tin cuối và số chưa đọc, lấy theo lô (không N+1). */
    private Function<ChatChannel, ChatChannelResponse> withSummary(List<ChatChannel> channels, UUID userId) {
        if (channels.isEmpty()) {
            return c -> chatMapper.toChannelResponse(c, userId);
        }
        List<UUID> ids = channels.stream().map(ChatChannel::getId).toList();

        Map<UUID, Long> unreadByChannel = new HashMap<>();
        messageRepository.countUnreadByChannels(ids, userId)
                .forEach(v -> unreadByChannel.put(v.getChannelId(), v.getTotal()));

        Map<UUID, Message> latestByChannel = new HashMap<>();
        for (Message m : messageRepository.findLatestByChannels(ids)) {
            latestByChannel.putIfAbsent(m.getChatChannel().getId(), m); // trùng sentAt thì lấy 1 tin
        }

        return channel -> {
            ChatChannelResponse res = chatMapper.toChannelResponse(channel, userId);
            res.setUnreadCount(unreadByChannel.getOrDefault(channel.getId(), 0L));

            Message last = latestByChannel.get(channel.getId());
            if (last != null) {
                res.setLastMessageContent(last.getContent());
                res.setLastMessageSenderId(last.getSender().getId());
                last.getMediaList().stream().findFirst()
                        .ifPresent(media -> res.setLastMessageMediaType(media.getType()));
            }
            return res;
        };
    }

    private int normalizeSize(int size) {
        return Math.clamp(size, 1, MAX_PAGE_SIZE);
    }
}