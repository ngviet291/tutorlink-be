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

    //Thống kê
    @Query("""
            SELECT t.type AS transactionType,
                   t.status AS transactionStatus,
                   COUNT(t) AS transactionCount,
                   SUM(t.amount) AS totalAmount
            FROM Transaction t
            WHERE t.transactionDate >= :start AND t.transactionDate < :end
            GROUP BY t.type, t.status
            """)
    List<TransactionStatRow> summarize(@Param("start") Instant start, @Param("end") Instant end);
}
