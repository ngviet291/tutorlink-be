package org.group3.tutorlink.features.subject.mapper;

import org.group3.tutorlink.features.user.dto.response.SubjectResponse;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubjectMapper {
    SubjectResponse toResponse(Subject subject);
}