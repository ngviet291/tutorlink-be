package org.group3.tutorlink.features.subject.mapper;

import org.group3.tutorlink.features.subject.dto.request.SubjectRequest;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SubjectMapper {

    // Mã định danh và các liên kết do lớp nghiệp vụ quản lý.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tutors", ignore = true)
    @Mapping(target = "posts", ignore = true)
    Subject toSubject(SubjectRequest request);

    org.group3.tutorlink.features.user.dto.response.SubjectResponse toUserSubjectResponse(Subject subject);

    org.group3.tutorlink.features.subject.dto.response.SubjectResponse toSubjectResponse(Subject subject);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tutors", ignore = true)
    @Mapping(target = "posts", ignore = true)
    void updateSubjectFromRequest(SubjectRequest request, @MappingTarget Subject subject);
}
