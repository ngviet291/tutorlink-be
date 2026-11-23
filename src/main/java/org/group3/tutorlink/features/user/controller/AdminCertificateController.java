package org.group3.tutorlink.features.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.user.dto.request.ApproveTutorRequest;
import org.group3.tutorlink.features.user.dto.response.AdminCertificateResponse;
import org.group3.tutorlink.features.user.dto.response.CertificateResponse;
import org.group3.tutorlink.features.user.enums.UserResponseCode;
import org.group3.tutorlink.features.user.service.AdminCertificateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/certificates")
@RequiredArgsConstructor
public class AdminCertificateController {

    private final AdminCertificateService adminCertificateService;

    // GET /v1/admin/certificates/pending — ADMIN: chứng chỉ AI chưa kiểm tra được (gia sư đã duyệt cập nhật hồ sơ)
    @GetMapping("/pending")
    public ApiResponse<List<AdminCertificateResponse>> getPending() {
        return ApiResponse.<List<AdminCertificateResponse>>builder()
                .code(UserResponseCode.PENDING_CERTIFICATES_FOUND.getCode())
                .message(UserResponseCode.PENDING_CERTIFICATES_FOUND.getMessage())
                .data(adminCertificateService.getPending())
                .build();
    }

    // GET /v1/admin/certificates/tutor/{tutorId} — ADMIN: mọi chứng chỉ của một gia sư (modal duyệt hồ sơ)
    @GetMapping("/tutor/{tutorId}")
    public ApiResponse<List<CertificateResponse>> getByTutor(@PathVariable UUID tutorId) {
        return ApiResponse.<List<CertificateResponse>>builder()
                .code(UserResponseCode.TUTOR_CERTIFICATES_FOUND.getCode())
                .message(UserResponseCode.TUTOR_CERTIFICATES_FOUND.getMessage())
                .data(adminCertificateService.getByTutor(tutorId))
                .build();
    }

    // PATCH /v1/admin/certificates/{id}/review — ADMIN { approved, reason }
    @PatchMapping("/{id}/review")
    public ApiResponse<Void> review(@PathVariable UUID id,
                                    @Valid @RequestBody ApproveTutorRequest request) {
        adminCertificateService.review(id, request);
        return ApiResponse.<Void>builder()
                .code(UserResponseCode.CERTIFICATE_REVIEWED.getCode())
                .message(UserResponseCode.CERTIFICATE_REVIEWED.getMessage())
                .build();
    }
}