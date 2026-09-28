package org.group3.tutorlink.features.user.mapper;

import org.group3.tutorlink.features.auth.dto.request.RegisterStudentRequest;
import org.group3.tutorlink.features.auth.dto.request.RegisterTutorRequest;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    Student toStudent(RegisterStudentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    Tutor toTutor(RegisterTutorRequest request);
}
