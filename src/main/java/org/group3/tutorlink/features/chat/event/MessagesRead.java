package org.group3.tutorlink.features.chat.event;

import lombok.*;
import org.group3.tutorlink.common.ws.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class MessagesRead implements DomainEvent {

    private UUID channelId;
    private UUID readerId;
    private UUID partnerId;
    private Instant readAt;
}