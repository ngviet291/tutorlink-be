package org.group3.tutorlink.features.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.chat.dto.request.SendMessageRequest;
import org.group3.tutorlink.features.chat.dto.response.MessageResponse;
import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.group3.tutorlink.features.chat.entity.Media;
import org.group3.tutorlink.features.chat.entity.Message;
import org.group3.tutorlink.features.chat.mapper.ChatMapper;
import org.group3.tutorlink.features.chat.repository.ChatChannelRepository;
import org.group3.tutorlink.features.chat.repository.MessageRepository;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatChannelRepository chatChannelRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AppUtil appUtil;
    private final ChatMapper chatMapper;

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest req) {
        UUID senderId = appUtil.userIdFromAuthentication();
        if (senderId.equals(req.getReceiverId())) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(UserNotFoundException::new);
        User receiver = userRepository.findById(req.getReceiverId())
                .orElseThrow(UserNotFoundException::new);

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

        return chatMapper.toMessageResponse(message);
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
}