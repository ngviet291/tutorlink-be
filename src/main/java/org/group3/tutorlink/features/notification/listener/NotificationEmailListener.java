package org.group3.tutorlink.features.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.auth.service.EmailService;
import org.group3.tutorlink.features.notification.dto.response.NotificationResponse;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.notification.event.NotificationCreated;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEmailListener {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.mail.notification.types}")
    private Set<NotificationType> emailTypes;

    // Sau commit: rollback thì không gửi mail. @Async: SMTP chậm không làm chậm API.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreated event) {
        NotificationResponse notification = event.getNotification();
        if (!emailTypes.contains(notification.getNotificationType())) {
            return;
        }
        userRepository.findById(event.getRecipientId())
                .map(User::getEmail)
                .filter(email -> !email.isBlank())
                .ifPresent(email -> send(email, notification));
    }

    private void send(String email, NotificationResponse notification) {
        try {
            emailService.sendNotificationEmail(email, notification.getTitle(), notification.getContent());
        } catch (Exception e) {
            // Mail lỗi không được ảnh hưởng thông báo trong app
            log.warn("Failed to send notification email, notificationId={}: {}",
                    notification.getId(), e.getMessage());
        }
    }
}