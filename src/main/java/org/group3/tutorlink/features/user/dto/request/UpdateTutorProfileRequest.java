package org.group3.tutorlink.features.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.TeachingMode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateTutorProfileRequest {
    private String bio;
    @Min(0) private Integer experienceYears;
    private String education;
    @Min(0) private BigDecimal hourlyRate;
    private Set<EducationLevel> teachingLevels;
    private TeachingMode teachingMode;
    private List<String> teachingAreas;
    @Valid
    private List<AvailableSlotRequest> availableSlots;
    private List<UUID> subjectIds;
}
