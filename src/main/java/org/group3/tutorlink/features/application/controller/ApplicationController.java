/*
 * @ (#) ApplicationController,java       1.0    29/09/2026
 *
 * Copyright (c) 2026 IUH. All righta reserved.
 */

package org.group3.tutorlink.features.application.controller;

/*
 * @description:
 * @author: Ho Thi Kim Xuyen
 * @version:     1.0
 * @date: 29/09/2026 00
 */
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.service.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

//    private final ApplicationService applicationService;
//
//    /**
//     * Student apply vào Post FIND_TUTOR
//     * hoặc Tutor apply vào Post FIND_STUDENT
//     */
//    @PostMapping
//    public ResponseEntity<ApplicationResponse> createApplication(
//            @Valid @RequestBody CreateApplicationRequest request
//    ) {
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(applicationService.createApplication(request));
//    }
//
//    /**
//     * Xem chi tiết Application
//     */
//    @GetMapping("/{applicationId}")
//    public ResponseEntity<ApplicationResponse> getApplicationById(
//            @PathVariable UUID applicationId
//    ) {
//        return ResponseEntity.ok(
//                applicationService.getApplicationById(applicationId)
//        );
//    }
//
//    /**
//     * Xem Application của chính mình
//     *
//     * role = student | tutor
//     */
//    @GetMapping("/mine")
//    public ResponseEntity<Page<ApplicationResponse>> getMyApplications(
//            @RequestParam String role,
//            Pageable pageable
//    ) {
//        return ResponseEntity.ok(
//                applicationService.getMyApplications(
//                        role,
//                        pageable
//                )
//        );
//    }
//
//    /**
//     * Admin xem danh sách Application
//     *
//     * Có thể lọc:
//     * ?status=PENDING
//     * ?status=ACCEPTED
//     * ?status=REJECTED
//     * ?status=CANCELLED
//     */
//    @GetMapping
//    public ResponseEntity<Page<ApplicationResponse>> getApplications(
//            @RequestParam(required = false) String status,
//            Pageable pageable
//    ) {
//        return ResponseEntity.ok(
//                applicationService.getApplications(
//                        status,
//                        pageable
//                )
//        );
//    }
//
//    /**
//     * Admin approve Application
//     */
//    @PostMapping("/{applicationId}/approve")
//    public ResponseEntity<ApplicationResponse> approveApplication(
//            @PathVariable UUID applicationId
//    ) {
//        return ResponseEntity.ok(
//                applicationService.approveApplication(
//                        applicationId
//                )
//        );
//    }
//
//    /**
//     * Admin / Student reject Application
//     */
//    @PostMapping("/{applicationId}/reject")
//    public ResponseEntity<ApplicationResponse> rejectApplication(
//            @PathVariable UUID applicationId
//    ) {
//        return ResponseEntity.ok(
//                applicationService.rejectApplication(
//                        applicationId
//                )
//        );
//    }
//
//    /**
//     * Student xác nhận Tutor đã được ACCEPTED
//     */
//    @PostMapping("/{applicationId}/confirm")
//    public ResponseEntity<ApplicationResponse> confirmApplication(
//            @PathVariable UUID applicationId
//    ) {
//        return ResponseEntity.ok(
//                applicationService.confirmApplication(
//                        applicationId
//                )
//        );
//    }
//
//    /**
//     * Student hoặc Tutor đánh dấu quá trình đã hoàn tất.
//     *
//     * Lưu ý:
//     * ApplicationStatus hiện tại chưa có FINISHED,
//     * nên Service hiện tại chưa thay đổi status.
//     */
//    @PostMapping("/{applicationId}/finish")
//    public ResponseEntity<ApplicationResponse> finishApplication(
//            @PathVariable UUID applicationId
//    ) {
//        return ResponseEntity.ok(
//                applicationService.finishApplication(
//                        applicationId
//                )
//        );
//    }
}