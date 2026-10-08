package org.group3.tutorlink.features.user.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ApproveTutorRequest {
    @NotNull(message = "APPROVAL_STATUS_REQUIRED")
    private Boolean approved;

    @Size(max = 500, message = "REASON_TOO_LONG")
    private String reason;
}
