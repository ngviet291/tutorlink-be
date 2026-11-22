package org.group3.tutorlink.features.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.group3.tutorlink.features.post.enums.TeachingMode;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectProfileResponse {
    private SubjectResponse subject;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private TeachingMode teachingMode;
}
