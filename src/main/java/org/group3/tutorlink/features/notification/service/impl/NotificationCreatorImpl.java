package org.group3.tutorlink.features.notification.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.notification.dto.response.NotificationResponse;
import org.group3.tutorlink.features.notification.entity.Notification;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.notification.event.NotificationCreated;
import org.group3.tutorlink.features.notification.mapper.NotificationMapper;
import org.group3.tutorlink.features.notification.repository.NotificationRepository;
import org.group3.tutorlink.features.notification.service.NotificationCreator;
import org.group3.tutorlink.features.user.entity.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreatorImpl implements NotificationCreator {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public NotificationResponse createNotification(
            UUID recipientId,
            NotificationType notificationType,
            String title,
            String content) {
        return createNotification(recipientId, notificationType, title, content, null, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public NotificationResponse createNotification(
            UUID recipientId,
            NotificationType notificationType,
            String title,
            String content,
            NotificationTarget targetType,
            UUID targetId) {

        User recipient = entityManager.getReference(User.class, recipientId);

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipient(recipient)
                .notificationType(notificationType)
                .title(title)
                .content(content)
                .targetType(targetType)
                .targetId(targetId)
                .build();

        Notification saved = notificationRepository.save(notification);
        // Map trong transaction, vì handler @Async không còn Hibernate session
        NotificationResponse response = notificationMapper.toNotificationResponse(saved);

        log.info("[CREATE_NOTIFICATION] recipientId={}, notificationId={}, notificationType={}",
                recipientId, saved.getId(), notificationType);
        eventPublisher.publishEvent(
                NotificationCreated.builder()
                        .recipientId(recipientId)
                        .notification(response)
                        .build()
        );
        return response;
    }
}