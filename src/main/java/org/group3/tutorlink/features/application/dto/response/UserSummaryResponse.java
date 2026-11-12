package org.group3.tutorlink.features.application.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private UUID id;
    private String fullName;
    private String avatarUrl;
}