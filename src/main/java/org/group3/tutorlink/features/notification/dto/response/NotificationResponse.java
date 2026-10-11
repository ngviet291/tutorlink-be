package org.group3.tutorlink.features.notification.dto.response;

import lombok.*;
import org.group3.tutorlink.features.notification.enums.NotificationType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String title;
    private String content;
    private NotificationType notificationType;
    private boolean isRead;
    private Instant createdAt;
}
