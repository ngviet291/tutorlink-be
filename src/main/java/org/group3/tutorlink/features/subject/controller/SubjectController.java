package org.group3.tutorlink.features.subject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.subject.dto.request.SubjectRequest;
import org.group3.tutorlink.features.subject.dto.response.SubjectResponse;
import org.group3.tutorlink.features.subject.enums.SubjectResponseCode;
import org.group3.tutorlink.features.subject.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    // 1. Xem danh sách môn học
    @GetMapping
    public ApiResponse<CursorResponse<SubjectResponse>> getAllSubjects(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "10") int limit) {

        return ApiResponse.<CursorResponse<SubjectResponse>>builder()
                .code(SubjectResponseCode.GET_SUBJECTS.getCode())
                .message(SubjectResponseCode.GET_SUBJECTS.getMessage())
                .data(subjectService.getAllSubjects(cursor, limit))
                .build();
    }

    // 2. Xem chi tiết danh sách môn học
    @GetMapping("/{id}")
    public ApiResponse<SubjectResponse> getSubjectById(@PathVariable UUID id) {
        return ApiResponse.<SubjectResponse>builder()
                .code(SubjectResponseCode.SUBJECT_FOUND.getCode())
                .message(SubjectResponseCode.SUBJECT_FOUND.getMessage())
                .data(subjectService.getSubjectById(id))
                .build();
    }

    // 3. Tạo mới môn học - chỉ có Admin làm
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubjectResponse> createSubject(@RequestBody @Valid SubjectRequest request) {
        return ApiResponse.<SubjectResponse>builder()
                .code(SubjectResponseCode.SUBJECT_CREATED.getCode())
                .message(SubjectResponseCode.SUBJECT_CREATED.getMessage())
                .data(subjectService.createSubject(request))
                .build();
    }

    // 4. Update môn học - chỉ có Admin làm
    @PutMapping("/{id}")
    public ApiResponse<SubjectResponse> updateSubject(
            @PathVariable UUID id,
            @RequestBody @Valid SubjectRequest request) {

        return ApiResponse.<SubjectResponse>builder()
                .code(SubjectResponseCode.SUBJECT_UPDATED.getCode())
                .message(SubjectResponseCode.SUBJECT_UPDATED.getMessage())
                .data(subjectService.updateSubject(id, request))
                .build();
    }

    // 5. Xóa môn học - chỉ có Admin làm
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable UUID id) {
        subjectService.deleteSubject(id);
    }
}
