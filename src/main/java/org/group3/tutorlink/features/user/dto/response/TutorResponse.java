package org.group3.tutorlink.features.user.dto.response;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.user.enums.VerificationStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TutorResponse extends BaseUserResponse {
    private Integer experienceYears;
    private String education;
    private List<SubjectResponse> subjects;
    private Double averageRating;
    private VerificationStatus verificationStatus;
    private String verificationNote;
    private String bio;
    private BigDecimal hourlyRate;
    private Set<EducationLevel> teachingLevels;
    private TeachingMode teachingMode;
    private List<String> teachingAreas;
    private List<AvailableSlotResponse> availableSlots;
}