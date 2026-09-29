        package org.group3.tutorlink.features.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request used to update an existing post")
public class UpdatePostRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(
            description = "Title of the post",
            example = "Tìm gia sư Toán lớp 10 tại Quận 7",
            maxLength = 255
    )
    private String title;

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
            example = "1"
    )
    private Long subjectId;

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

    @Schema(
            description = "Learning location",
            implementation = Address.class
    )
    private Address address;

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

    @Schema(
            description = "Status of the post",
            example = "OPEN"
    )
    private PostStatus status;
}
