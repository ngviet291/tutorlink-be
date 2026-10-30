package org.group3.tutorlink.features.review.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.review.dto.request.ResolveReviewComplaintRequest;
import org.group3.tutorlink.features.review.dto.response.ReviewComplaintResponse;
import org.group3.tutorlink.features.review.enums.ComplaintStatus;
import org.group3.tutorlink.features.review.enums.ReviewComplaintResponseCode;
import org.group3.tutorlink.features.review.service.ReviewComplaintService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/review-complaints")
@RequiredArgsConstructor
@Validated
public class ReviewComplaintController {
    private final ReviewComplaintService reviewComplaintService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<List<ReviewComplaintResponse>> getReviewComplaints(
            @RequestParam(required = false) ComplaintStatus status) {
        return ApiResponse.<List<ReviewComplaintResponse>>builder()
                .code(ReviewComplaintResponseCode.GET_SUCCESS.getCode())
                .message(ReviewComplaintResponseCode.GET_SUCCESS.getMessage())
                .data(reviewComplaintService.findAll(status))
                .build();
    }

    @PatchMapping("/{complaintId}/resolve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<ReviewComplaintResponse> resolveReviewComplaint(
            @PathVariable UUID complaintId,
            @Valid @RequestBody ResolveReviewComplaintRequest request) {
        return ApiResponse.<ReviewComplaintResponse>builder()
                .code(ReviewComplaintResponseCode.RESOLVE_SUCCESS.getCode())
                .message(ReviewComplaintResponseCode.RESOLVE_SUCCESS.getMessage())
                .data(reviewComplaintService.resolve(complaintId, request.getStatus()))
                .build();
    }
}
