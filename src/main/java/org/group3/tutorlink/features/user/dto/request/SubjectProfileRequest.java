package org.group3.tutorlink.features.user.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.post.enums.TeachingMode;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectProfileRequest {
    @NotNull
    private UUID subjectId;
    @Min(0)
    @Max(50)
    private Integer experienceYears;
    @DecimalMin("0")
    private BigDecimal hourlyRate;
    private TeachingMode teachingMode;
}
