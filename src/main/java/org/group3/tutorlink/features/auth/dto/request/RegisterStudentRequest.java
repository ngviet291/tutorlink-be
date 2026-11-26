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
    @NotBlank(message = "Full name must not be blank")
    private String fullName;
    @Email(message = "Email is not valid")
    @NotBlank(message = "Email must not be blank")
    private String email;
    @NotBlank(message = "Password must not be blank")
    @ValidPassword
    private String password;
    @Pattern(
            regexp = "^0[35789][0-9]{8}$",
            message = "Phone number must have 10 digits and start with 03, 05, 07, 08 or 09"
    )
    private String phone;

    @NotBlank(message = "Verification code must not be blank")
    @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
    private String verificationCode;
    private Gender gender;
    private LocalDate dateOfBirth;
    @NotNull(message = "Address is required")
    @Valid
    private AddressRequest address;

    private String grade;
    private String learningGoal;
}
