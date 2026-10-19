package org.group3.tutorlink.features.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.review.dto.request.CreateReviewRequest;
import org.group3.tutorlink.features.review.dto.response.ReviewResponse;
import org.group3.tutorlink.features.review.entity.Review;
import org.group3.tutorlink.features.review.exception.ReviewAccessDeniedException;
import org.group3.tutorlink.features.review.exception.ReviewAlreadyExistsException;
import org.group3.tutorlink.features.review.exception.ReviewApplicationNotFoundException;
import org.group3.tutorlink.features.review.exception.ReviewInvalidApplicationStatusException;
import org.group3.tutorlink.features.review.exception.ReviewInvalidApplicationException;
import org.group3.tutorlink.features.review.exception.ReviewNotFoundException;
import org.group3.tutorlink.features.review.mapper.ReviewMapper;
import org.group3.tutorlink.features.review.repository.ReviewRepository;
import org.group3.tutorlink.features.review.service.ReviewService;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final AppUtil appUtil;
    private final ReviewMapper reviewMapper;

    @Override
    @PreAuthorize("hasAuthority('STUDENT')")
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        UUID userId = appUtil.userIdFromAuthentication();
        User user = userRepository.findById(userId)
                .orElseThrow(ReviewAccessDeniedException::new);
        if (!(user instanceof Student student)) {
            throw new ReviewAccessDeniedException();
        }
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(ReviewApplicationNotFoundException::new);
        if (application.getPost() == null) {
            throw new ReviewInvalidApplicationException();
        }
        Student applicationStudent = application.getStudent();
        Tutor applicationTutor = application.getTutor();
        if (application.getPost().getAuthor() instanceof Student postStudent) {
            applicationStudent = postStudent;
        }
        if (application.getPost().getAuthor() instanceof Tutor postTutor) {
            applicationTutor = postTutor;
        }
        if (application.getApplicationStatus() != ApplicationStatus.FINISHED) {
            throw new ReviewInvalidApplicationStatusException();
        }
        if (applicationStudent == null || applicationTutor == null) {
            throw new ReviewInvalidApplicationException();
        }
        if (!applicationStudent.getId().equals(student.getId())) {
            throw new ReviewAccessDeniedException();
        }
        if (reviewRepository.existsByApplicationId(application.getId())) {
            throw new ReviewAlreadyExistsException();
        }
        Review review = Review.builder()
                .id(appUtil.generateUUID())
                .application(application)
                .student(student)
                .tutor(applicationTutor)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<ReviewResponse> getReviewsByTutor(UUID tutorId, UUID cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        return appUtil.buildCursorResponse(
                reviewRepository.findByTutorIdAfterCursor(tutorId, cursor, pageable),
                limit,
                Review::getId,
                reviewMapper::toResponse
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(UUID id) {
        return reviewRepository.findById(id)
                .map(reviewMapper::toResponse)
                .orElseThrow(ReviewNotFoundException::new);
    }
}
