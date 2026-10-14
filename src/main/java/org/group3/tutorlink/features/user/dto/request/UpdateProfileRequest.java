package org.group3.tutorlink.features.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.group3.tutorlink.features.user.enums.Gender;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullname;

    @Pattern(regexp = "^0\\d{9}$", message = "Phone number must start with 0 and have 10 digits")
    private String phone;

    private String avatarUrl;
    private Gender gender;
    private LocalDate dateOfBirth;
    private AddressRequest address;
}