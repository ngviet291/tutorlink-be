package org.group3.tutorlink.features.review.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.review.dto.request.CreateReviewRequest;
import org.group3.tutorlink.features.review.dto.request.CreateReviewComplaintRequest;
import org.group3.tutorlink.features.review.dto.response.ReviewComplaintResponse;
import org.group3.tutorlink.features.review.service.ReviewComplaintService;
import org.group3.tutorlink.features.review.dto.response.ReviewResponse;
import org.group3.tutorlink.features.review.enums.ReviewComplaintResponseCode;
import org.group3.tutorlink.features.review.enums.ReviewResponseCode;
import org.group3.tutorlink.features.review.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/reviews")
@Validated
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewComplaintService complaintService;

    @PostMapping("/{reviewId}/complaints")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewComplaintResponse> createComplaint(
            @PathVariable UUID reviewId,
            @Valid @RequestBody CreateReviewComplaintRequest request) {
        return ApiResponse.<ReviewComplaintResponse>builder()
                .code(ReviewComplaintResponseCode.CREATE_SUCCESS.getCode())
                .message(ReviewComplaintResponseCode.CREATE_SUCCESS.getMessage())
                .data(complaintService.create(reviewId, request))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .code(ReviewResponseCode.REVIEW_CREATED.getCode())
                .message(ReviewResponseCode.REVIEW_CREATED.getMessage())
                .data(reviewService.createReview(request))
                .build();
    }

    @GetMapping
    public ApiResponse<CursorResponse<ReviewResponse>> getReviews(
            @RequestParam @NotNull UUID tutorId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100) int limit) {
        return ApiResponse.<CursorResponse<ReviewResponse>>builder()
                .code(ReviewResponseCode.REVIEWS_RETRIEVED.getCode())
                .message(ReviewResponseCode.REVIEWS_RETRIEVED.getMessage())
                .data(reviewService.getReviewsByTutor(tutorId, cursor, limit))
                .build();
    }

    @GetMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> getReview(@PathVariable UUID reviewId) {
        return ApiResponse.<ReviewResponse>builder()
                .code(ReviewResponseCode.REVIEW_FOUND.getCode())
                .message(ReviewResponseCode.REVIEW_FOUND.getMessage())
                .data(reviewService.getReviewById(reviewId))
                .build();
    }
}
