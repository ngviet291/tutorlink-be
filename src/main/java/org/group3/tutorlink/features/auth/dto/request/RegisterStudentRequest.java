package org.group3.tutorlink.features.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.user.enums.Gender;

import java.time.LocalDate;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegisterStudentRequest {
    @NotBlank(message = "Fullname not be blank")
    private String fullname;
    @Email(message = "Email not valid")
    @NotBlank(message = "Email not be blank")
    private String email;
    @NotBlank(message = "Password not be blank")
    private String password;
    private String phone;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;

    private String grade;
    private String school;
    private String learningGoal;
}
