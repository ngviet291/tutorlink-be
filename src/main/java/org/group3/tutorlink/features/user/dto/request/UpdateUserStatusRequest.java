package org.group3.tutorlink.features.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.group3.tutorlink.features.user.enums.UserStatus;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateUserStatusRequest {
    @NotNull(message = "USER_STATUS_REQUIRED")
    private UserStatus userStatus;
}
