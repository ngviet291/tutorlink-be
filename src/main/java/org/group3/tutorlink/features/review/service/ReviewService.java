package org.group3.tutorlink.features.review.service;

import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.review.dto.request.CreateReviewRequest;
import org.group3.tutorlink.features.review.dto.response.ReviewResponse;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(CreateReviewRequest request);
    CursorResponse<ReviewResponse> getReviewsByTutor(UUID tutorId, UUID cursor, int limit);
    ReviewResponse getReviewById(UUID id);
}
