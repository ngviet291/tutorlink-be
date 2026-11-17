package org.group3.tutorlink.features.notification.entity;

import org.group3.tutorlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.user.entity.User;

import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    private UUID id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;
    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;

    /** Đích đến khi click thông báo. Null với thông báo cũ (tạo trước khi có tính năng này). */
    @Enumerated(EnumType.STRING)
    private NotificationTarget targetType;

    /** Id thực thể đích (vd: postId). Có thể null nếu đích không cần id. */
    private UUID targetId;
    // Notification n -- 1 User (receives)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
}
