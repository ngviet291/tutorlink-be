package org.group3.tutorlink.features.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.group3.tutorlink.features.review.enums.ComplaintStatus;

@Getter
@Setter
@Schema(description = "Request used to resolve a review complaint")
public class ResolveReviewComplaintRequest {
    @NotNull(message = "Complaint status is required")
    @Schema(description = "Resolution status", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
    private ComplaintStatus status;
}
