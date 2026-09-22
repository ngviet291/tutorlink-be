package org.group3.tutorlink.features.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.user.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegisterTutorRequest {
    @NotBlank(message = "Fullname not be blank")
    private String fullname;
    @Email(message = "Email is not valid")
    @NotBlank(message = "Email not be blank")
    private String email;
    @NotBlank(message = "Password not be blank")
    private String password;
    private String phone;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;

    @Min(value = 0, message = "experienceYears must be greater than or equal to 0")
    private int experienceYears;
    private String education;
    @NotNull(message = "subjectId must not be null")
    private UUID subjectId;
}
