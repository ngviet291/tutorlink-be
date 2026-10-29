package org.group3.tutorlink.features.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.exception.AccessDeniedException;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.chat.dto.request.SendMessageRequest;
import org.group3.tutorlink.features.chat.dto.response.ChatChannelResponse;
import org.group3.tutorlink.features.chat.dto.response.MessageResponse;
import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.group3.tutorlink.features.chat.entity.Media;
import org.group3.tutorlink.features.chat.entity.Message;
import org.group3.tutorlink.features.chat.expection.CannotMessageSelfException;
import org.group3.tutorlink.features.chat.expection.ChatChannelNotFound;
import org.group3.tutorlink.features.chat.expection.ChatErrorCode;
import org.group3.tutorlink.features.chat.mapper.ChatMapper;
import org.group3.tutorlink.features.chat.repository.ChatChannelRepository;
import org.group3.tutorlink.features.chat.repository.MessageRepository;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatChannelRepository chatChannelRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AppUtil appUtil;
    private final ChatMapper chatMapper;
    private static final int MAX_PAGE_SIZE = 50;

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest req) {
        UUID senderId = appUtil.userIdFromAuthentication();

        if (senderId.equals(req.getReceiverId())) {
            throw new CannotMessageSelfException();
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(UserNotFoundException::new);
        User receiver = userRepository.findById(req.getReceiverId())
                .orElseThrow(UserNotFoundException::new);

        validateUser(sender, receiver);

        ChatChannel channel = findOrCreateChannel(sender, receiver);
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
//        TODO(WS): push "new message" event cho đối phương (partner) của channel này
        return chatMapper.toMessageResponse(message);
    }

    private void validateUser(User sender, User receiver) {
        boolean valid = (sender instanceof Tutor && receiver instanceof Student)
                || (sender instanceof Student && receiver instanceof Tutor);

        if (!valid) {
            throw new AccessDeniedException(ChatErrorCode.CHAT_ONLY_TUTOR_STUDENT);
        }
    }

    private ChatChannel findOrCreateChannel(User sender, User receiver) {
        return chatChannelRepository.findBetween(sender.getId(), receiver.getId())
                .orElseGet(() -> createChannel(sender, receiver));
    }

    private ChatChannel createChannel(User sender, User receiver) {
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
                c -> chatMapper.toChannelResponse(c, userId));
    }

    @Transactional(readOnly = true)
    public ChatChannelResponse getChatChannelById(UUID channelId) {
        UUID userId = appUtil.userIdFromAuthentication();
        return chatMapper.toChannelResponse(getParticipatingChannel(channelId, userId), userId);
    }

    // Mở chat từ profile: chưa có channel thì 404, channel sẽ được tạo khi gửi tin đầu tiên
    @Transactional(readOnly = true)
    public ChatChannelResponse getChatChannelWithUser(UUID partnerId) {
        UUID userId = appUtil.userIdFromAuthentication();
        ChatChannel channel = chatChannelRepository.findBetween(userId, partnerId)
                .orElseThrow(ChatChannelNotFound::new);
        return chatMapper.toChannelResponse(channel, userId);
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
        getParticipatingChannel(channelId, userId);
        int updated = messageRepository.markAllAsRead(channelId, userId);
        if (updated > 0) {
            // TODO(WS): push "messages read" event cho đối phương (partner) của channel này
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

    private int normalizeSize(int size) {
        return Math.clamp(size, 1, MAX_PAGE_SIZE);
    }
}