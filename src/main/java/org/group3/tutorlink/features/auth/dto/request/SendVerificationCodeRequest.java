package org.group3.tutorlink.features.auth.dto.request;

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
public class SendVerificationCodeRequest {
    @Email(message = "Email is not valid")
    @NotBlank(message = "Email not be blank")
    private String email;
}
