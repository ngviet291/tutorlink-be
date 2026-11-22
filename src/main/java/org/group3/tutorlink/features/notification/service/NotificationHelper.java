package org.group3.tutorlink.features.notification.service;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.AdminRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationHelper {

    private final NotificationCreator notificationCreator;
    private final NotificationMessages notificationMessages;
    private final AdminRepository adminRepository;

    public void notify(User recipient, NotificationMessage message, NotificationTarget target,
                       UUID targetId, Object... args) {
        if (recipient == null) {
            return;
        }
        notificationCreator.createNotification(recipient.getId(), message.getType(),
                notificationMessages.title(message), notificationMessages.content(message, args),
                target, targetId);
    }

    public void notifyAdmins(NotificationMessage message, NotificationTarget target, Object... args) {
        adminRepository.findAll().forEach(admin -> notify(admin, message, target, null, args));
    }

    public String money(BigDecimal amount) {
        return notificationMessages.money(amount);
    }
}