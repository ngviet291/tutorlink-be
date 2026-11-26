package org.group3.tutorlink.features.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.auth.validation.annotation.ValidPassword;
import org.group3.tutorlink.features.user.dto.request.AddressRequest;
import org.group3.tutorlink.features.user.enums.Gender;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegisterTutorRequest {
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

    @Min(value = 0, message = "Years of experience must not be negative")
    private int experienceYears;
    private String education;
    // Cấp học nhận dạy: bắt buộc khi đăng ký; các thông tin còn lại bổ sung sau khi hồ sơ được duyệt.
    @NotEmpty(message = "Select at least one teaching level")
    private java.util.Set<org.group3.tutorlink.features.post.enums.EducationLevel> teachingLevels;
    @NotEmpty(message = "Select at least one subject")
    private List<
            @NotNull(message = "Invalid subject")
                    UUID> subjectIds;

    // Phần tử i = môn của file chứng chỉ thứ i. Bỏ trống được nếu chỉ chọn 1 môn.
    private List<UUID> certificateSubjectIds;
}
