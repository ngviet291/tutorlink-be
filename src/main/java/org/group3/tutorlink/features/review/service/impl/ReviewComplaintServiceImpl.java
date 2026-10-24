package org.group3.tutorlink.features.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.review.dto.request.CreateReviewComplaintRequest;
import org.group3.tutorlink.features.review.dto.response.ReviewComplaintResponse;
import org.group3.tutorlink.features.review.entity.Review;
import org.group3.tutorlink.features.review.entity.ReviewComplaint;
import org.group3.tutorlink.features.review.enums.ComplaintStatus;
import org.group3.tutorlink.features.review.exception.ReviewComplaintNotFoundException;
import org.group3.tutorlink.features.review.exception.ReviewInvalidComplaintStatusException;
import org.group3.tutorlink.features.review.exception.ReviewNotFoundException;
import org.group3.tutorlink.features.review.repository.ReviewComplaintRepository;
import org.group3.tutorlink.features.review.repository.ReviewRepository;
import org.group3.tutorlink.features.review.service.ReviewComplaintService;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.repository.AdminRepository;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewComplaintServiceImpl implements ReviewComplaintService {
    private final ReviewComplaintRepository complaintRepository;
    private final ReviewRepository reviewRepository;
    private final TutorRepository tutorRepository;
    private final AdminRepository adminRepository;
    private final AppUtil appUtil;

    @Override
    @PreAuthorize("hasAuthority('TUTOR')")
    @Transactional
    public ReviewComplaintResponse create(UUID reviewId, CreateReviewComplaintRequest request) {
        UUID userId = appUtil.userIdFromAuthentication();
        Tutor tutor = tutorRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Review review = reviewRepository.findById(reviewId).orElseThrow(ReviewNotFoundException::new);
        if (!review.getTutor().getId().equals(tutor.getId())) {
            throw new org.group3.tutorlink.features.review.exception.ReviewAccessDeniedException();
        }
        ReviewComplaint complaint = ReviewComplaint.builder()
                .id(appUtil.generateUUID()).reason(request.getReason().trim())
                .status(ComplaintStatus.PENDING).review(review).tutor(tutor).build();
        return toResponse(complaintRepository.save(complaint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewComplaintResponse> findAll(ComplaintStatus status) {
        List<ReviewComplaint> complaints = status == null
                ? complaintRepository.findAll()
                : complaintRepository.findByStatus(status);
        return complaints.stream().map(this::toResponse).toList();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public ReviewComplaintResponse resolve(UUID id, ComplaintStatus status) {
        if (status != ComplaintStatus.APPROVED && status != ComplaintStatus.REJECTED) {
            throw new ReviewInvalidComplaintStatusException();
        }
        UUID userId = appUtil.userIdFromAuthentication();
        Admin admin = adminRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        ReviewComplaint complaint = complaintRepository.findById(id)
                .orElseThrow(ReviewComplaintNotFoundException::new);
        complaint.setStatus(status);
        complaint.setAdmin(admin);
        return toResponse(complaintRepository.save(complaint));
    }

    private ReviewComplaintResponse toResponse(ReviewComplaint complaint) {
        return ReviewComplaintResponse.builder().id(complaint.getId()).reason(complaint.getReason())
                .status(complaint.getStatus()).reviewId(complaint.getReview().getId())
                .tutorId(complaint.getTutor().getId())
                .adminId(complaint.getAdmin() == null ? null : complaint.getAdmin().getId()).build();
    }
}
