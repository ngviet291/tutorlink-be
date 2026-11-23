package org.group3.tutorlink.features.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.auth.validation.annotation.ValidPassword;
import org.group3.tutorlink.features.user.dto.request.AddressRequest;
import org.group3.tutorlink.features.user.enums.Gender;

import java.time.LocalDate;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegisterStudentRequest {
    @NotBlank(message = "FullName not be blank")
    private String fullName;
    @Email(message = "Email not valid")
    @NotBlank(message = "Email not be blank")
    private String email;
    @NotBlank(message = "Password not be blank")
    @ValidPassword
    private String password;
    @Pattern(
            regexp = "^(0[3|5|7|8|9])[0-9]{8}$",
            message = "Invalid Vietnamese phone number"
    )
    private String phone;

    @NotBlank(message = "Verification code must not be blank")
    @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
    private String verificationCode;
    private Gender gender;
    private LocalDate dateOfBirth;
    @NotNull(message = "Address must not be null")
    @Valid
    private AddressRequest address;

    private String grade;
    private String learningGoal;
}
