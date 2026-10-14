package org.group3.tutorlink.features.notification.specification;

import org.group3.tutorlink.features.notification.entity.Notification;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class NotificationSpecification {

    private NotificationSpecification() {
    }

    public static Specification<Notification> hasRecipient(UUID recipientId) {
        return (root, query, cb) -> cb.equal(root.get("recipient").get("id"), recipientId);
    }

    public static Specification<Notification> cursor(UUID cursor) {
        if (cursor == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.lessThan(root.get("id"), cursor);
    }
}