package org.group3.tutorlink.features.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request object for forgot password")
public class ForgotPasswordRequest {
    @Schema(description = "Email of the user who forgot their password", example = "youremail@gmail.com")
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email must not be blank")
    private String email;
}
