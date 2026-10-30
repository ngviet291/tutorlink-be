package org.group3.tutorlink.features.review.dto.response;

import lombok.Builder;
import lombok.Value;
import org.group3.tutorlink.features.review.enums.ComplaintStatus;

import java.util.UUID;

@Value
@Builder
public class ReviewComplaintResponse {
    UUID id;
    String reason;
    ComplaintStatus status;
    UUID reviewId;
    UUID tutorId;
    UUID adminId;
}
