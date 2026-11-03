package org.group3.tutorlink.features.review.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ReviewResponse {
    UUID id;
    UUID applicationId;
    UUID studentId;
    UUID tutorId;
    int rating;
    String comment;
    Instant createdAt;
}
