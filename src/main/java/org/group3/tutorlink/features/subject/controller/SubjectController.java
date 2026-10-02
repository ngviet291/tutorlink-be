package org.group3.tutorlink.features.subject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.subject.dto.request.SubjectRequest;
import org.group3.tutorlink.features.subject.dto.response.SubjectResponse;
import org.group3.tutorlink.features.subject.service.SubjectService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    // 1. Xem danh sách (Public)
    @GetMapping
    public ApiResponse<List<SubjectResponse>> getAllSubjects() {
        return ApiResponse.<List<SubjectResponse>>builder()
                .data(subjectService.getAllSubjects())
                .build();
    }

    // 2. Xem chi tiết (Public)
    @GetMapping("/{id}")
    public ApiResponse<SubjectResponse> getSubjectById(@PathVariable UUID id) {
        return ApiResponse.<SubjectResponse>builder()
                .data(subjectService.getSubjectById(id))
                .build();
    }

    // 3. Tạo mới (ROLE_ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> createSubject(@RequestBody @Valid SubjectRequest request) {
        return ApiResponse.<SubjectResponse>builder()
                .data(subjectService.createSubject(request))
                .build();
    }

    // 4. Cập nhật (ROLE_ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> updateSubject(
            @PathVariable UUID id,
            @RequestBody @Valid SubjectRequest request) {

        return ApiResponse.<SubjectResponse>builder()
                .data(subjectService.updateSubject(id, request))
                .build();
    }

    // 5. Xóa (ROLE_ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSubject(@PathVariable UUID id) {
        subjectService.deleteSubject(id);
        return ApiResponse.<Void>builder()
                .message("Xóa môn học thành công")
                .build();
    }
}