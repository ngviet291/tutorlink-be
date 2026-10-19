package org.group3.tutorlink.features.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateReviewRequest {
    @NotNull
    private UUID applicationId;

    @Min(1)
    @Max(5)
    private int rating;

    @Size(max = 2000)
    private String comment;
}
