package org.group3.tutorlink.features.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.notification.dto.response.NotificationResponse;
import org.group3.tutorlink.features.notification.entity.Notification;
import org.group3.tutorlink.features.notification.exception.NotificationAccessDeniedException;
import org.group3.tutorlink.features.notification.exception.NotificationNotFoundException;
import org.group3.tutorlink.features.notification.mapper.NotificationMapper;
import org.group3.tutorlink.features.notification.repository.NotificationRepository;
import org.group3.tutorlink.features.notification.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final AppUtil appUtil;

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Override
    public CursorResponse<NotificationResponse> getMyNotifications(
            UUID cursor, int limit) {
        UUID currentUserId = appUtil.userIdFromAuthentication();
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Notification> notifications = notificationRepository.findMyNotifications(currentUserId, cursor, pageable);
        return appUtil.buildCursorResponse(
                notifications,
                limit,
                Notification::getId,
                notificationMapper::toNotificationResponse
        );
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Override
    public NotificationResponse markAsRead(UUID notificationId) {
        UUID currentUserId = appUtil.userIdFromAuthentication();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationNotFoundException::new);

        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new NotificationAccessDeniedException();
        }

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return notificationMapper.toNotificationResponse(updated);
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Override
    public int markAllAsRead() {
        UUID currentUserId = appUtil.userIdFromAuthentication();
        return notificationRepository.updateIsRead(currentUserId);
    }
}