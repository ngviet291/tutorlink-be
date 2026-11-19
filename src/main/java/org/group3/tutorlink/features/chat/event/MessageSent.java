package org.group3.tutorlink.features.chat.event;

import lombok.*;
import org.group3.tutorlink.common.ws.event.DomainEvent;
import org.group3.tutorlink.features.chat.dto.response.MessageResponse;

import java.util.UUID;

@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class MessageSent implements DomainEvent {

    private UUID senderId;
    private UUID receiverId;
    private MessageResponse message;
}