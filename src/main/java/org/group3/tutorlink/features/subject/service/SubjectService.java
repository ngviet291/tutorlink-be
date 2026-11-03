package org.group3.tutorlink.features.subject.service;

import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.subject.dto.request.SubjectRequest;
import org.group3.tutorlink.features.subject.dto.response.SubjectResponse;
import java.util.UUID;

public interface SubjectService {
    CursorResponse<SubjectResponse> getAllSubjects(UUID cursor, int limit);
    SubjectResponse getSubjectById(UUID id);
    SubjectResponse createSubject(SubjectRequest request);
    SubjectResponse updateSubject(UUID id, SubjectRequest request);
    void deleteSubject(UUID id);
}