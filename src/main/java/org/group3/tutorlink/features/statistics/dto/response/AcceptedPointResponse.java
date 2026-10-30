package org.group3.tutorlink.features.statistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AcceptedPointResponse {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private long totalAccepted;
}