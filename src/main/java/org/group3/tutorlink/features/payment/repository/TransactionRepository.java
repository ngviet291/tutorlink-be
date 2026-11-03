package org.group3.tutorlink.features.payment.repository;

import jakarta.persistence.LockModeType;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByApplicationId(UUID applicationId);

    // Khóa dòng giao dịch --> tránh hoàn tiền 2 lần
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Transaction> findForUpdateById(UUID id);

    List<Transaction> findByTutorId(UUID tutorId, Pageable pageable);

    List<Transaction> findByTutorIdAndIdLessThan(UUID tutorId, UUID cursor, Pageable pageable);

    //Admin
    List<Transaction> findAllBy(Pageable pageable);

    List<Transaction> findByIdLessThan(UUID cursor, Pageable pageable);

    List<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    List<Transaction> findByStatusAndIdLessThan(TransactionStatus status, UUID cursor, Pageable pageable);

    // Khóa giao dịch nạp tiền khi xử lý callback --> tránh cộng tiền 2 lần nếu cổng gọi lặp
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Transaction> findForUpdateByProviderOrderId(String providerOrderId);

    // Thống kê doanh thu: gộp toàn bộ số liệu bằng 1 câu truy vấn (aggregate ở DB), luôn trả đúng 1 dòng
    @Query("""
            SELECT
                SUM(CASE WHEN t.type = org.group3.tutorlink.features.payment.enums.TransactionType.COMMISSION
                          AND t.status = org.group3.tutorlink.features.payment.enums.TransactionStatus.SUCCESS
                         THEN t.amount END) AS totalRevenue,
                SUM(CASE WHEN t.type = org.group3.tutorlink.features.payment.enums.TransactionType.COMMISSION
                          AND t.status = org.group3.tutorlink.features.payment.enums.TransactionStatus.REFUNDED
                         THEN t.amount END) AS totalRefunded,
                SUM(CASE WHEN t.type = org.group3.tutorlink.features.payment.enums.TransactionType.TOP_UP
                          AND t.status = org.group3.tutorlink.features.payment.enums.TransactionStatus.SUCCESS
                         THEN t.amount END) AS totalTopUp,
                COUNT(CASE WHEN t.type = org.group3.tutorlink.features.payment.enums.TransactionType.COMMISSION
                            AND t.status = org.group3.tutorlink.features.payment.enums.TransactionStatus.SUCCESS
                           THEN 1 END) AS successCount,
                COUNT(CASE WHEN t.type = org.group3.tutorlink.features.payment.enums.TransactionType.COMMISSION
                            AND t.status = org.group3.tutorlink.features.payment.enums.TransactionStatus.FAILED
                           THEN 1 END) AS failedCount,
                COUNT(CASE WHEN t.type = org.group3.tutorlink.features.payment.enums.TransactionType.COMMISSION
                            AND t.status = org.group3.tutorlink.features.payment.enums.TransactionStatus.REFUNDED
                           THEN 1 END) AS refundedCount,
                COUNT(CASE WHEN t.type = org.group3.tutorlink.features.payment.enums.TransactionType.TOP_UP
                            AND t.status = org.group3.tutorlink.features.payment.enums.TransactionStatus.SUCCESS
                           THEN 1 END) AS topUpCount,
                COUNT(t) AS totalTransactions
            FROM Transaction t
            WHERE t.transactionDate >= :start AND t.transactionDate < :end
            """)
    RevenueStatisticsRow summarizeRevenue(@Param("start") Instant start, @Param("end") Instant end);
}
