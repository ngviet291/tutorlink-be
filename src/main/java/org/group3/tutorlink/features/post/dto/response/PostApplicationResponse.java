package org.group3.tutorlink.features.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PostApplicationResponse {
    private UUID applicationId;
    private String userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String status;
    private String appliedAt;
}
