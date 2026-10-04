package org.group3.tutorlink.features.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.enums.ApplicationResponseCode;
import org.group3.tutorlink.features.application.service.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // Tutor apply vào FIND_TUTOR, Student apply vào FIND_STUDENT
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest req) {

        return ApiResponse.<ApplicationResponse>builder()
                .code(ApplicationResponseCode.CREATE_SUCCESS.getCode())
                .message(ApplicationResponseCode.CREATE_SUCCESS.getMessage())
                .data(applicationService.createApplication(req))
                .build();
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<ApplicationResponse> getApplicationById(
            @PathVariable UUID applicationId) {

        return ApiResponse.<ApplicationResponse>builder()
                .code(ApplicationResponseCode.GET_SUCCESS.getCode())
                .message(ApplicationResponseCode.GET_SUCCESS.getMessage())
                .data(applicationService.getApplicationById(applicationId))
                .build();
    }

    @GetMapping("/mine")
    public ApiResponse<Page<ApplicationResponse>> getMyApplications(
            @RequestParam String role,
            Pageable pageable) {

        return ApiResponse.<Page<ApplicationResponse>>builder()
                .code(ApplicationResponseCode.GET_SUCCESS.getCode())
                .message(ApplicationResponseCode.GET_SUCCESS.getMessage())
                .data(applicationService.getMyApplications(role, pageable))
                .build();
    }

    // Admin xem tất cả
    @GetMapping
    public ApiResponse<Page<ApplicationResponse>> getApplications(
            @RequestParam(required = false) String status,
            Pageable pageable) {

        return ApiResponse.<Page<ApplicationResponse>>builder()
                .code(ApplicationResponseCode.GET_SUCCESS.getCode())
                .message(ApplicationResponseCode.GET_SUCCESS.getMessage())
                .data(applicationService.getApplications(status, pageable))
                .build();
    }

    @PostMapping("/{applicationId}/select")
    public ApiResponse<ApplicationResponse> selectApplication(
            @PathVariable UUID applicationId) {

        return ApiResponse.<ApplicationResponse>builder()
                .code(ApplicationResponseCode.SELECT_SUCCESS.getCode())
                .message(ApplicationResponseCode.SELECT_SUCCESS.getMessage())
                .data(applicationService.selectApplication(applicationId))
                .build();
    }

    @PostMapping("/{applicationId}/confirm")
    public ApiResponse<ApplicationResponse> confirmApplication(
            @PathVariable UUID applicationId) {

        return ApiResponse.<ApplicationResponse>builder()
                .code(ApplicationResponseCode.CONFIRM_SUCCESS.getCode())
                .message(ApplicationResponseCode.CONFIRM_SUCCESS.getMessage())
                .data(applicationService.confirmApplication(applicationId))
                .build();
    }

    @PostMapping("/{applicationId}/cancel")
    public ApiResponse<ApplicationResponse> cancelApplication(
            @PathVariable UUID applicationId) {

        return ApiResponse.<ApplicationResponse>builder()
                .code(ApplicationResponseCode.CANCEL_SUCCESS.getCode())
                .message(ApplicationResponseCode.CANCEL_SUCCESS.getMessage())
                .data(applicationService.cancelApplication(applicationId))
                .build();
    }
}