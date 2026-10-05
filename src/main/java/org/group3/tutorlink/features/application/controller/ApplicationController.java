///*
// * @ (#) ApplicationController,java       1.0    29/09/2026
// *
// * Copyright (c) 2026 IUH. All righta reserved.
// */
//
//package org.group3.tutorlink.features.application.controller;
//
///*
// * @description:
// * @author: Ho Thi Kim Xuyen
// * @version:     1.0
// * @date: 29/09/2026 00
// */
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
//import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
//import org.group3.tutorlink.features.application.service.ApplicationService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/v1/applications")
//@RequiredArgsConstructor
//public class ApplicationController {
//
//    private final ApplicationService applicationService;
//
//    /**
//     * Tutor apply vào FIND_TUTOR
//     * hoặc Student apply vào FIND_STUDENT.
//     *
//     * POST /v1/applications
//     */
//    @PostMapping
//    public ResponseEntity<ApplicationResponse> createApplication(
//            @Valid @RequestBody CreateApplicationRequest request
//    ) {
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(
//                        applicationService.createApplication(request)
//                );
//    }
//
//    /**
//     * Xem chi tiết Application.
//     *
//     * GET /v1/applications/{applicationId}
//     */
//    @GetMapping("/{applicationId}")
//    public ResponseEntity<ApplicationResponse> getApplicationById(
//            @PathVariable UUID applicationId
//    ) {
//
//        return ResponseEntity.ok(
//                applicationService.getApplicationById(
//                        applicationId
//                )
//        );
//    }
//
//    /**
//     * Xem Application của chính mình.
//     *
//     * GET /v1/applications/mine?role=tutor
//     * GET /v1/applications/mine?role=student
//     */
//    @GetMapping("/mine")
//    public ResponseEntity<Page<ApplicationResponse>> getMyApplications(
//            @RequestParam String role,
//            Pageable pageable
//    ) {
//
//        return ResponseEntity.ok(
//                applicationService.getMyApplications(
//                        role,
//                        pageable
//                )
//        );
//    }
//
//    /**
//     * Admin xem danh sách Application.
//     *
//     * GET /v1/applications
//     *
//     * Có thể filter:
//     *
//     * ?status=PENDING
//     * ?status=ACCEPTED
//     * ?status=COMPLETED
//     * ?status=REJECTED
//     * ?status=CANCELLED
//     *
//     * Lưu ý:
//     * Admin xem xét nhưng không làm thay đổi status.
//     */
//    @GetMapping
//    public ResponseEntity<Page<ApplicationResponse>> getApplications(
//            @RequestParam(required = false) String status,
//            Pageable pageable
//    ) {
//
//        return ResponseEntity.ok(
//                applicationService.getApplications(
//                        status,
//                        pageable
//                )
//        );
//    }
//
//
//    @PostMapping("/{applicationId}/select")
//    public ResponseEntity<ApplicationResponse> selectApplication(
//            @PathVariable UUID applicationId
//    ) {
//
//        return ResponseEntity.ok(
//                applicationService.selectApplication(
//                        applicationId
//                )
//        );
//    }
//
//
//    @PostMapping("/{applicationId}/confirm")
//    public ResponseEntity<ApplicationResponse> confirmApplication(
//            @PathVariable UUID applicationId
//    ) {
//
//        return ResponseEntity.ok(
//                applicationService.confirmApplication(
//                        applicationId
//                )
//        );
//    }
//
//    @PostMapping("/{applicationId}/cancel")
//    public ResponseEntity<ApplicationResponse> cancelApplication(
//            @PathVariable UUID applicationId
//    ) {
//
//        return ResponseEntity.ok(
//                applicationService.cancelApplication(
//                        applicationId
//                )
//        );
//    }
//}