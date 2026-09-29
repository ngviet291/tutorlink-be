package org.group3.tutorlink.features.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private UUID id;

    private String title;

    private String content;

    private PostType type;

    private PostStatus status;

    private SubjectSummaryResponse subject;

    private EducationLevel educationLevel;

    private TeachingMode teachingMode;

    private AddressResponse address;

    private BigDecimal minBudget;

    private BigDecimal maxBudget;

    private Integer sessionsPerWeek;

    private Integer durationMinutes;

    private Instant deadline;

    private AuthorResponse author;

    private Instant createdAt;

    private Instant updatedAt;
}