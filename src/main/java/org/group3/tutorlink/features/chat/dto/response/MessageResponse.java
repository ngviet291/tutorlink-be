package org.group3.tutorlink.features.chat.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private UUID id;
    private UUID chatChannelId;
    private UUID senderId;
    private String content;
    private Instant sentAt;
    private List<MediaResponse> mediaList;
}