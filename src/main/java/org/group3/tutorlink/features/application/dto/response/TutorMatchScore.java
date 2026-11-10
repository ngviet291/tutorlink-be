package org.group3.tutorlink.features.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorMatchScore {
    private String applicationId;
    private Integer score;
}