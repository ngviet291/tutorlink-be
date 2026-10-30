package org.group3.tutorlink.features.review.service;

import org.group3.tutorlink.features.review.dto.request.CreateReviewComplaintRequest;
import org.group3.tutorlink.features.review.dto.response.ReviewComplaintResponse;
import org.group3.tutorlink.features.review.enums.ComplaintStatus;

import java.util.List;
import java.util.UUID;

public interface ReviewComplaintService {
    ReviewComplaintResponse create(UUID reviewId, CreateReviewComplaintRequest request);
    List<ReviewComplaintResponse> findAll(ComplaintStatus status);
    ReviewComplaintResponse resolve(UUID id, ComplaintStatus status);
}
