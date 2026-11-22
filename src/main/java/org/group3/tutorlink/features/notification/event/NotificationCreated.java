package org.group3.tutorlink.features.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.common.ws.event.DomainEvent;
import org.group3.tutorlink.features.notification.dto.response.NotificationResponse;

import java.util.UUID;

@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class NotificationCreated implements DomainEvent {

    private UUID recipientId;
    private NotificationResponse notification;
}