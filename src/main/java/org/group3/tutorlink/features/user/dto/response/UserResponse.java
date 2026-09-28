package org.group3.tutorlink.features.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.user.enums.Gender;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserResponse {
    private UUID id;
    private String fullname;
    private String email;
    private String phone;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String avatarUrl;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Address address;
    private UserStatus userStatus;
    private String role;

    private String grade;
    private String school;
    private String learningGoal;


    private Integer experienceYears;
    private String education;
    private UUID subjectId;
    private String subjectName;
    private Double averageRating;
    private VerificationStatus verificationStatus;

}
