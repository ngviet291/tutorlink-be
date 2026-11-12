package org.group3.tutorlink.features.application.mapper;

import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "processedByAdminId", source = "processedByAdmin.id")
    @Mapping(target = "transactionId", source = "transaction.id")
    ApplicationResponse toApplicationResponse(Application application);


}