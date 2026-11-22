package org.group3.tutorlink.features.user.mapper;

import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.user.dto.response.CertificateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CertificateResponseMapper {
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    CertificateResponse toResponse(Certificate certificate);

    List<CertificateResponse> toResponses(List<Certificate> certificates);
}