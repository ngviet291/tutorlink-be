package org.group3.tutorlink.features.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request used to complain about a review")
public class CreateReviewComplaintRequest {
    @NotBlank(message = "Complaint reason is required")
    @Schema(description = "Reason for the complaint", example = "This review contains inappropriate content",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
