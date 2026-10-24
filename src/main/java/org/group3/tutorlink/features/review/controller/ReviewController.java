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
import org.group3.tutorlink.features.review.enums.ReviewResponseCode;
import org.group3.tutorlink.features.review.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/reviews")
@Validated
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewComplaintService complaintService;

    @PostMapping("/{id}/complaints")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewComplaintResponse> createComplaint(
            @PathVariable UUID id, @Valid @RequestBody CreateReviewComplaintRequest request) {
        return ApiResponse.<ReviewComplaintResponse>builder()
                .code("REVIEW_COMPLAINT_201").message("Review complaint created successfully")
                .data(complaintService.create(id, request)).build();
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

    @GetMapping("/{id}")
    public ApiResponse<ReviewResponse> getReview(@PathVariable UUID id) {
        return ApiResponse.<ReviewResponse>builder()
                .code(ReviewResponseCode.REVIEW_FOUND.getCode())
                .message(ReviewResponseCode.REVIEW_FOUND.getMessage())
                .data(reviewService.getReviewById(id))
                .build();
    }
}
