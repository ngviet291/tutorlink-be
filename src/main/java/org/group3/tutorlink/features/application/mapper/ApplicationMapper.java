package org.group3.tutorlink.features.application.mapper;

import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    // Đã sửa
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "processedByAdminId", source = "processedByAdmin.id")
    @Mapping(target = "transactionId", source = "transaction.id")
    ApplicationResponse toApplicationResponse(Application application);
}