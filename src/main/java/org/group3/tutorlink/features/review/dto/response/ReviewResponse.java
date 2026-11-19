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
    // Tên hiển thị 2 bên — FE khỏi phải fetch public-profile từng review
    String studentName;
    String tutorName;
    int rating;
    String comment;
    Instant createdAt;
    // NONE / PENDING / APPROVED / REJECTED — null khi chưa có khiếu nại
    String complaintStatus;
}
