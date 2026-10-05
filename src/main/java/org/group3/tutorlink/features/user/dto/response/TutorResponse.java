package org.group3.tutorlink.features.user.dto.response;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.group3.tutorlink.features.user.enums.VerificationStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TutorResponse extends BaseUserResponse {
    private Integer experienceYears;
    private String education;
    private SubjectResponse subject;
    private Double averageRating;
    private VerificationStatus verificationStatus;
    private String verificationNote;
}