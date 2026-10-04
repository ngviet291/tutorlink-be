package org.group3.tutorlink.features.application.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.service.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * Tutor apply vào FIND_TUTOR
     * Student apply vào FIND_STUDENT.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        applicationService.createApplication(request)
                );
    }

    /**
     * Xem chi tiết Application.
     *
     * Admin / Student / Tutor
     */
    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STUDENT', 'TUTOR')")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable UUID applicationId
    ) {

        return ResponseEntity.ok(
                applicationService.getApplicationById(applicationId)
        );
    }

    /**
     * Xem Application của chính mình.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ResponseEntity<Page<ApplicationResponse>> getMyApplications(
            @RequestParam String role,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                applicationService.getMyApplications(
                        role,
                        pageable
                )
        );
    }

    /**
     * Admin xem tất cả Application.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ApplicationResponse>> getApplications(
            @RequestParam(required = false) String status,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                applicationService.getApplications(
                        status,
                        pageable
                )
        );
    }


    @PostMapping("/{applicationId}/select")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ResponseEntity<ApplicationResponse> selectApplication(
            @PathVariable UUID applicationId
    ) {

        return ResponseEntity.ok(
                applicationService.selectApplication(
                        applicationId
                )
        );
    }


    @PostMapping("/{applicationId}/confirm")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ResponseEntity<ApplicationResponse> confirmApplication(
            @PathVariable UUID applicationId
    ) {

        return ResponseEntity.ok(
                applicationService.confirmApplication(
                        applicationId
                )
        );
    }


    @PostMapping("/{applicationId}/cancel")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ResponseEntity<ApplicationResponse> cancelApplication(
            @PathVariable UUID applicationId
    ) {

        return ResponseEntity.ok(
                applicationService.cancelApplication(
                        applicationId
                )
        );
    }
}