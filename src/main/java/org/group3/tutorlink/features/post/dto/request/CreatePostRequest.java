package org.group3.tutorlink.features.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request used to create a new post")
/*
 * Validate budget range: minBudget should not exceed maxBudget if both are provided.
 * Validate address: address is required if teachingMode is OFFLINE or BOTH.
 *
 */
public class CreatePostRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(
            description = "Title of the post",
            example = "Tìm gia sư Toán lớp 10 tại Quận 7",
            maxLength = 255,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String title;

    @NotBlank(message = "Content must not be blank")
    @Schema(
            description = "Detailed description of the tutoring request",
            example = "Cần tìm gia sư Toán lớp 10, học 3 buổi mỗi tuần tại Quận 7."
    )
    private String content;

    @NotNull(message = "Post type is required")
    @Schema(
            description = "Type of the post. FIND_TUTOR means a student is looking for a tutor. FIND_STUDENT means a tutor is looking for a student.",
            example = "FIND_TUTOR",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PostType type;

    @NotNull(message = "Subject is required")
    @Schema(
            description = "ID of the subject, ID is UUID of the subject entity",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID subjectId;

    @NotNull(message = "Education level is required")
    @Schema(
            description = "Education level of the student",
            example = "HIGH_SCHOOL",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private EducationLevel educationLevel;

    @NotNull(message = "Teaching mode is required")
    @Schema(
            description = "Preferred teaching mode",
            example = "OFFLINE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private TeachingMode teachingMode;

    @Schema(
            description = "Learning location. Required when teaching mode is OFFLINE or BOTH."
    )
    private AddressRequest address;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Minimum budget must be greater than or equal to 0"
    )
    @Schema(
            description = "Minimum budget per tutoring session",
            example = "100000",
            minimum = "0"
    )
    private BigDecimal minBudget;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Maximum budget must be greater than or equal to 0"
    )
    @Schema(
            description = "Maximum budget per tutoring session",
            example = "150000",
            minimum = "0"
    )
    private BigDecimal maxBudget;

    @Min(
            value = 1,
            message = "Sessions per week must be at least 1"
    )
    @Schema(
            description = "Number of tutoring sessions per week",
            example = "3",
            minimum = "1"
    )
    private Integer sessionsPerWeek;

    @Min(
            value = 15,
            message = "Duration must be at least 15 minutes"
    )
    @Schema(
            description = "Duration of each tutoring session in minutes",
            example = "90",
            minimum = "15"
    )
    private Integer durationMinutes;

    @Future(message = "Deadline must be in the future")
    @Schema(
            description = "Deadline for accepting applications",
            example = "2026-10-15T23:59:59Z"
    )
    private Instant deadline;
}

