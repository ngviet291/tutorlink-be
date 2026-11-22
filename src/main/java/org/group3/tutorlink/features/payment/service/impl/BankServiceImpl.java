package org.group3.tutorlink.features.payment.service.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.dto.response.BankResponse;
import org.group3.tutorlink.features.payment.gateway.bank.BankGateway;
import org.group3.tutorlink.features.payment.service.BankService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Danh sách ngân hàng ít thay đổi nên giữ trong bộ nhớ một thời gian, tránh gọi bên ngoài mỗi lần.
 * Khi nhà cung cấp lỗi mà còn bản cũ thì trả bản cũ thay vì làm hỏng màn hình của người dùng.
 */
@Slf4j
@Service
public class BankServiceImpl implements BankService {

    @Getter
    @AllArgsConstructor
    private static final class Snapshot {
        private final List<BankResponse> banks;
        private final Instant fetchedAt;
    }

    private final BankGateway bankGateway;
    private final Duration cacheTtl;
    private volatile Snapshot snapshot;

    public BankServiceImpl(BankGateway bankGateway,
                           @Value("${payment.vietqr.banks-cache-hours:24}") long cacheHours) {
        this.bankGateway = bankGateway;
        this.cacheTtl = Duration.ofHours(cacheHours);
    }

    @Override
    public List<BankResponse> getBanks() {
        Snapshot current = snapshot;
        if (isFresh(current)) {
            return current.getBanks();
        }
        synchronized (this) {
            current = snapshot;
            if (isFresh(current)) {
                return current.getBanks();
            }
            try {
                List<BankResponse> banks = bankGateway.fetchBanks().stream()
                        .map(BankResponse::from)
                        .toList();
                snapshot = new Snapshot(banks, Instant.now());
                return banks;
            } catch (AppException ex) {
                if (current != null) {
                    log.warn("Bank list refresh failed, serving cached list from {}", current.getFetchedAt());
                    return current.getBanks();
                }
                throw ex;
            }
        }
    }

    private boolean isFresh(Snapshot value) {
        return value != null && value.getFetchedAt().plus(cacheTtl).isAfter(Instant.now());
    }
}