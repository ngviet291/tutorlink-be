package org.group3.tutorlink.features.user.mapper;

import org.group3.tutorlink.features.auth.dto.request.RegisterStudentRequest;
import org.group3.tutorlink.features.auth.dto.request.RegisterTutorRequest;
import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.user.dto.response.UserResponse;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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

    @Mapping(target = "role", source = "role", qualifiedByName = "roleToName")
    UserResponse toBaseUserResponse(User user);

    @Mapping(target = "role", source = "role", qualifiedByName = "roleToName")
    UserResponse toStudentResponse(Student student);

    @Mapping(target = "role", source = "role", qualifiedByName = "roleToName")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    UserResponse toTutorResponse(Tutor tutor);

    default UserResponse toUserResponse(User user) {
        if (user instanceof Student student) {
            return toStudentResponse(student);
        }
        if (user instanceof Tutor tutor) {
            return toTutorResponse(tutor);
        }
        return toBaseUserResponse(user);
    }
    @Named("roleToName")
    default String roleToName(Role role) {
        return role != null ? role.getName() : null;
    }
}