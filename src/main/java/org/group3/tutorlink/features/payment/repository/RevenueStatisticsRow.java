package org.group3.tutorlink.features.payment.repository;

import java.math.BigDecimal;

public interface RevenueStatisticsRow {
    BigDecimal getTotalRevenue();
    BigDecimal getTotalRefunded();
    BigDecimal getTotalTopUp();
    Long getSuccessCount();
    Long getFailedCount();
    Long getRefundedCount();
    Long getTopUpCount();
    Long getTotalTransactions();
}