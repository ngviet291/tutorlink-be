package org.group3.tutorlink.features.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntrospectResponse {
    private boolean active;
    private String role;
    private String userId;
    private Long exp;
    private Long iat;
    private String sub;
}