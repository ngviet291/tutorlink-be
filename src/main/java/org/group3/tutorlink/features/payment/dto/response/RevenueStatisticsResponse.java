package org.group3.tutorlink.features.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Revenue and operation statistics of the platform!")
public class RevenueStatisticsResponse {

    @Schema(description = "Start date of the period (inclusive)!", example = "2026-10-01")
    private LocalDate from;

    @Schema(description = "End date of the period (inclusive)!", example = "2026-10-31")
    private LocalDate to;

    @Schema(description = "Net revenue: total of successful commission payments, refunded ones excluded (VND)!", example = "1500000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "Total of refunded commission payments (VND)!", example = "30000.00")
    private BigDecimal totalRefunded;

    @Schema(description = "Total of successful wallet top-ups (VND)!", example = "5000000.00")
    private BigDecimal totalTopUp;

    @Schema(description = "Number of successful commission payments!", example = "50")
    private long successCount;

    @Schema(description = "Number of failed commission payments!", example = "3")
    private long failedCount;

    @Schema(description = "Number of refunded commission payments!", example = "1")
    private long refundedCount;

    @Schema(description = "Number of successful wallet top-ups!", example = "20")
    private long topUpCount;

    @Schema(description = "Number of all transactions in the period!", example = "74")
    private long totalTransactions;
}
