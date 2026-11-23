package org.group3.tutorlink.features.user.mapper;

import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.user.dto.response.AdminCertificateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminCertificateMapper {

    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorName", source = "tutor.fullName")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    @Mapping(target = "submittedAt", source = "createdAt")
    AdminCertificateResponse toResponse(Certificate certificate);

    List<AdminCertificateResponse> toResponses(List<Certificate> certificates);
}