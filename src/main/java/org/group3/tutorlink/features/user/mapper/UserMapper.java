package org.group3.tutorlink.features.user.mapper;

import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.auth.dto.request.RegisterStudentRequest;
import org.group3.tutorlink.features.auth.dto.request.RegisterTutorRequest;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.user.dto.request.UpdateProfileRequest;
import org.group3.tutorlink.features.user.dto.response.AddressResponse;
import org.group3.tutorlink.features.user.dto.response.BaseUserResponse;
import org.group3.tutorlink.features.user.dto.response.StudentResponse;
import org.group3.tutorlink.features.user.dto.response.SubjectResponse;
import org.group3.tutorlink.features.user.dto.response.TutorResponse;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.SubclassMapping;

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
    @Mapping(target = "verificationNote", ignore = true)
    Tutor toTutor(RegisterTutorRequest request);

    AddressResponse toAddressResponse(Address address);

    SubjectResponse toSubjectResponse(Subject subject);

    @Mapping(target = "role", source = "role.name")
    StudentResponse toStudentResponse(Student student);

    @Mapping(target = "role", source = "role.name")
    TutorResponse toTutorResponse(Tutor tutor);

    @SubclassMapping(source = Student.class, target = StudentResponse.class)
    @SubclassMapping(source = Tutor.class, target = TutorResponse.class)
    @Mapping(target = "role", source = "role.name")
    BaseUserResponse toBaseUserResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(@MappingTarget User user, UpdateProfileRequest request);
}
