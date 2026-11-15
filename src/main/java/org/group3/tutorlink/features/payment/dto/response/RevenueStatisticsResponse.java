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
@Schema(description = "Thống kê doanh thu và hoạt động của nền tảng.")
public class RevenueStatisticsResponse {

    @Schema(description = "Ngày bắt đầu kỳ thống kê, tính cả ngày này.", example = "2026-10-01")
    private LocalDate from;

    @Schema(description = "Ngày kết thúc kỳ thống kê, tính cả ngày này.", example = "2026-10-31")
    private LocalDate to;

    @Schema(description = "Doanh thu phí nhận lớp thành công, không gồm khoản đã hoàn; đơn vị VND.", example = "1500000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "Tổng phí nhận lớp đã hoàn, đơn vị VND.", example = "30000.00")
    private BigDecimal totalRefunded;

    @Schema(description = "Tổng tiền nạp ví thành công, đơn vị VND.", example = "5000000.00")
    private BigDecimal totalTopUp;

    @Schema(description = "Số giao dịch phí nhận lớp thành công.", example = "50")
    private long successCount;

    @Schema(description = "Số giao dịch phí nhận lớp thất bại.", example = "3")
    private long failedCount;

    @Schema(description = "Số giao dịch phí nhận lớp đã hoàn tiền.", example = "1")
    private long refundedCount;

    @Schema(description = "Số giao dịch nạp ví thành công.", example = "20")
    private long topUpCount;

    @Schema(description = "Tổng số giao dịch trong kỳ.", example = "74")
    private long totalTransactions;
}
