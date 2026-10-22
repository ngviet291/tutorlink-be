package org.group3.tutorlink.features.payment.repository;

import java.math.BigDecimal;

/**
 * Một dòng kết quả thống kê doanh thu (tên getter khớp alias trong câu truy vấn summarizeRevenue).
 * Các tổng tiền là null khi không có giao dịch phù hợp.
 */
public interface RevenueStatisticsRow {

    BigDecimal getTotalRevenue();

    BigDecimal getTotalRefunded();

    BigDecimal getTotalTopUp();

    long getSuccessCount();

    long getFailedCount();

    long getRefundedCount();

    long getTopUpCount();

    long getTotalTransactions();
}
