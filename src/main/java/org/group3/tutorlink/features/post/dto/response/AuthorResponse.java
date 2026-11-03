package org.group3.tutorlink.features.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import org.group3.tutorlink.features.auth.enums.RoleName;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthorResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private RoleName role;
}
