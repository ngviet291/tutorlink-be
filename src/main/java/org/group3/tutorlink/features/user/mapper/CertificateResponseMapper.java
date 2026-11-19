package org.group3.tutorlink.features.user.mapper;

import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.user.dto.response.CertificateResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CertificateResponseMapper {
    CertificateResponse toResponse(Certificate certificate);

    List<CertificateResponse> toResponses(List<Certificate> certificates);
}