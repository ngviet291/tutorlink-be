package org.group3.tutorlink.features.user.dto.response;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.group3.tutorlink.features.user.enums.Gender;
import org.group3.tutorlink.features.user.enums.UserStatus;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseUserResponse {
    private UUID id;
    private String fullname;
    private String email;
    private String phone;
    private String avatarUrl;
    private Gender gender;
    private LocalDate dateOfBirth;
    private AddressResponse address;
    private UserStatus userStatus;
    private String role;
}