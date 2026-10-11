package org.group3.tutorlink.features.notification.repository;

import org.group3.tutorlink.features.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipient.id = :recipientId
          AND (:cursor IS NULL OR n.id < :cursor)
        ORDER BY n.id DESC
    """)
    List<Notification> findMyNotifications(
            @Param("recipientId") UUID recipientId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.recipient.id = :recipientId
          AND n.isRead = false
    """)
    int updateIsRead(@Param("recipientId") UUID recipientId);
}
