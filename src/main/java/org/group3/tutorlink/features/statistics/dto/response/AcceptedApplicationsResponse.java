package org.group3.tutorlink.features.statistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptedApplicationsResponse {
    private long totalAccepted;
}
