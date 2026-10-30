package org.group3.tutorlink.features.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.user.dto.request.AddressRequest;
import org.group3.tutorlink.features.user.enums.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegisterTutorRequest {
    @NotBlank(message = "FullName not be blank")
    private String fullName;
    @Email(message = "Email is not valid")
    @NotBlank(message = "Email not be blank")
    private String email;
    @NotBlank(message = "Password not be blank")
    private String password;
    private String phone;
    private Gender gender;
    private LocalDate dateOfBirth;
    @NotNull(message = "Address must not be null")
    @Valid
    private AddressRequest address;

    @Min(value = 0, message = "experienceYears must be greater than or equal to 0")
    private int experienceYears;
    private String education;
    @NotEmpty(message = "subjectIds must not be empty")
    private List<
            @NotNull(message = "subjectId must not be null")
                    UUID> subjectIds;
}

