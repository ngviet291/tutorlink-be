package org.group3.tutorlink.features.statistics.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RevenuePointResponse {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalRevenue;
    private long totalTransactions;
}