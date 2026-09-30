package org.group3.tutorlink.features.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.post.validation.ValidPostRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@ValidPostRequest
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request used to update an existing post")
public class UpdatePostRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(
            max = 255,
            message = "Title must not exceed 255 characters"
    )
    @Schema(
            description = "Title of the post",
            example = "Tìm gia sư Toán lớp 10 tại Quận 7",
            maxLength = 255
    )
    private String title;

    @Size(
            max = 5000,
            message = "Content must not exceed 5000 characters"
    )
    @Schema(
            description = "Detailed description of the tutoring request",
            example = "Cần tìm gia sư Toán lớp 10, học 3 buổi mỗi tuần."
    )
    private String content;

    @Schema(
            description = "Type of the post",
            example = "FIND_TUTOR"
    )
    private PostType type;

    @Schema(
            description = "ID of the subject",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID subjectId;

    @Schema(
            description = "Education level of the student",
            example = "HIGH_SCHOOL"
    )
    private EducationLevel educationLevel;

    @Schema(
            description = "Preferred teaching mode",
            example = "OFFLINE"
    )
    private TeachingMode teachingMode;

    @Valid
    @Schema(
            description = "Learning location"
    )
    private AddressRequest address;

    @DecimalMin(
            value = "0.0",
            message = "Minimum budget must be greater than or equal to 0"
    )
    private BigDecimal minBudget;

    @DecimalMin(
            value = "0.0",
            message = "Maximum budget must be greater than or equal to 0"
    )
    private BigDecimal maxBudget;

    @Min(
            value = 1,
            message = "Sessions per week must be at least 1"
    )
    private Integer sessionsPerWeek;

    @Min(
            value = 15,
            message = "Duration must be at least 15 minutes"
    )
    private Integer durationMinutes;

    @Future(message = "Deadline must be in the future")
    private Instant deadline;

    @Schema(
            description = "Status of the post",
            example = "OPEN"
    )
    private PostStatus status;
}