package org.group3.tutorlink.features.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.auth.validation.annotation.ValidPassword;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@Schema(description = "Request object for resetting password")
public class ResetPasswordRequest {
    @Schema(description = "Token for resetting password", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "Token must not be blank")
    private String token;
    @NotBlank(message = "New password must not be blank")
    @ValidPassword(message = "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, a special character and no spaces")
    @Schema(description = "New password for the user", example = "newSecurePassword123!")
    private String newPassword;
}
