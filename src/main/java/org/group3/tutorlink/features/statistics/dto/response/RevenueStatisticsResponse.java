package org.group3.tutorlink.features.statistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsResponse {
    private UUID reportId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalRevenue;
    private long totalTransactions;
    private UUID generatedByAdminId;
    private Instant generatedAt;
}
