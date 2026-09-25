package org.group3.tutorlink.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
        private String refreshToken;
}
