package org.group3.tutorlink.features.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder

public class TutorMatchScore {
    private String applicationId;
    private Integer score;


}