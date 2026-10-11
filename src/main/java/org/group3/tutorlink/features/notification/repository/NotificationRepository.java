package org.group3.tutorlink.features.notification.repository;

import org.group3.tutorlink.features.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {
    List<Notification> findAllByRecipientIdAndIsReadFalse(UUID recipientId);
}
