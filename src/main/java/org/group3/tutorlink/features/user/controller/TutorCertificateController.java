package org.group3.tutorlink.features.user.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.auth.validation.annotation.ValidImageFiles;
import org.group3.tutorlink.features.user.dto.response.CertificateResponse;
import org.group3.tutorlink.features.user.enums.UserResponseCode;
import org.group3.tutorlink.features.user.service.TutorCertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users/me/certificates")
@RequiredArgsConstructor
@Validated
public class TutorCertificateController {

    private final TutorCertificateService certificateService;

    // GET /v1/users/me/certificates — TUTOR
    @GetMapping
    public ApiResponse<List<CertificateResponse>> getMyCertificates() {
        return respond(UserResponseCode.CERTIFICATES_FOUND, certificateService.getMyCertificates());
    }

    // POST /v1/users/me/certificates — TUTOR, multipart part "certificates" (ảnh, tối đa 5 file x 5MB)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<CertificateResponse>> uploadCertificates(
            @RequestPart("certificates")
            @ValidImageFiles(maxFiles = 5, maxSizeBytes = 5L * 1024 * 1024)
            List<MultipartFile> certificates) {
        return respond(UserResponseCode.CERTIFICATES_UPLOADED, certificateService.uploadCertificates(certificates));
    }

    // DELETE /v1/users/me/certificates/{id} — TUTOR
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteCertificate(@PathVariable UUID id) {
        certificateService.deleteCertificate(id);
        return respond(UserResponseCode.CERTIFICATE_DELETED, null);
    }

    private <T> ApiResponse<T> respond(UserResponseCode code, T data) {
        return ApiResponse.<T>builder()
                .code(code.getCode())
                .message(code.getMessage())
                .data(data)
                .build();
    }
}