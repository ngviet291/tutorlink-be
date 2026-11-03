package org.group3.tutorlink.features.subject.mapper;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.UploadMediaResponse;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring")
public abstract class CertificateMapper {
    @Autowired
    protected AppUtil appUtil;


    @Mapping(target = "id", expression = "java(appUtil.generateUUID())")
    @Mapping(target = "fileUrl", source = "uploaded.url")
    @Mapping(target = "cloudinaryPublicId", source = "uploaded.publicId")
    @Mapping(target = "tutor", source = "tutor")
    @Mapping(target = "verified", constant = "false")
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "issuedBy", ignore = true)
    @Mapping(target = "issuedDate", ignore = true)
    @Mapping(target = "expiryDate", ignore = true)
    public abstract Certificate toCertificate(UploadMediaResponse uploaded, Tutor tutor);

    public List<Certificate> toCertificates(List<UploadMediaResponse> uploaded, Tutor tutor) {
        return uploaded.stream()
                .map(u -> toCertificate(u, tutor))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
