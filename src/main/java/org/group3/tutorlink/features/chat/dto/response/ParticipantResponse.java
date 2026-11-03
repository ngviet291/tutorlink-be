package org.group3.tutorlink.features.chat.dto.response;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantResponse {
    private UUID id;
    private String fullName;
    private String avatarUrl;
}