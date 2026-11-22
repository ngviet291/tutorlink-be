package org.group3.tutorlink.features.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request used to create a review for a completed application")
public class CreateReviewRequest {
    @NotNull(message = "Application ID is required")
    @Schema(
            description = "ID of the completed application being reviewed",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID applicationId;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    @Schema(
            description = "Review rating",
            example = "5",
            minimum = "1",
            maximum = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int rating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    @Schema(
            description = "Optional review comment",
            example = "Gia su day rat nhiet tinh va de hieu.",
            maxLength = 2000
    )
    private String comment;
}
