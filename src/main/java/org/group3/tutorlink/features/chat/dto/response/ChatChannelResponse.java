package org.group3.tutorlink.features.chat.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatChannelResponse {
    private UUID id;
    private Instant lastMessageAt;
    private ParticipantResponse partner; // người còn lại so với user hiện tại
}