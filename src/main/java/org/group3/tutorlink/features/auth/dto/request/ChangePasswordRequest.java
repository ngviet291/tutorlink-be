package org.group3.tutorlink.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.auth.validation.annotation.DifferentPassword;
import org.group3.tutorlink.features.auth.validation.annotation.ValidPassword;
@DifferentPassword
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    @NotBlank @ValidPassword
    private String currentPassword;
    @NotBlank @ValidPassword
    private String newPassword;
}