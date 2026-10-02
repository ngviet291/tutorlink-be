package org.group3.tutorlink.features.subject.mapper;

import org.group3.tutorlink.features.subject.dto.request.SubjectRequest;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SubjectMapper {

    Subject toSubject(SubjectRequest request);

    org.group3.tutorlink.features.user.dto.response.SubjectResponse toResponse(Subject subject);

    // 2. Hàm này DÀNH RIÊNG cho module subject
    org.group3.tutorlink.features.subject.dto.response.SubjectResponse toSubjectResponse(Subject subject);

    void updateSubjectFromRequest(SubjectRequest request, @MappingTarget Subject subject);
}