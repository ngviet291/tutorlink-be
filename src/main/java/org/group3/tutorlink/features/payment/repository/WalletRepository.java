package org.group3.tutorlink.features.payment.repository;

import jakarta.persistence.LockModeType;
import org.group3.tutorlink.features.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByTutorId(UUID tutorId);

    // Khóa dòng ví khi cộng/trừ tiền để tránh 2 request đồng thời làm sai số dư
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findForUpdateByTutorId(UUID tutorId);
}
