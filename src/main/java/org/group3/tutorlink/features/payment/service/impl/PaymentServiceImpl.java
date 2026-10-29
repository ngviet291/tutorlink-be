package org.group3.tutorlink.features.payment.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.payment.dto.request.TopUpRequest;
import org.group3.tutorlink.features.payment.dto.response.RevenueStatisticsResponse;
import org.group3.tutorlink.features.payment.dto.response.TransactionResponse;
import org.group3.tutorlink.features.payment.dto.response.WalletBalanceResponse;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.entity.Wallet;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;
import org.group3.tutorlink.features.payment.exception.PaymentInsufficientBalanceException;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.exception.PaymentTutorNotFoundException;
import org.group3.tutorlink.features.payment.mapper.TransactionMapper;
import org.group3.tutorlink.features.payment.mapper.WalletMapper;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.group3.tutorlink.features.payment.repository.TransactionStatRow;
import org.group3.tutorlink.features.payment.repository.WalletRepository;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    // Phí hoa hồng = 20% giá của bài đăng
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.20");

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TutorRepository tutorRepository;
    private final ApplicationRepository applicationRepository;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final AppUtil appUtil;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('TUTOR')")
    public WalletBalanceResponse getWalletBalance() {
        UUID tutorId = appUtil.userIdFromAuthentication();

        Wallet wallet = walletRepository.findByTutorId(tutorId)
                .orElseGet(() -> createWallet(tutorId));

        return walletMapper.toWalletBalanceResponse(wallet);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('TUTOR')")
    public TransactionResponse topUp(TopUpRequest request) {
        UUID tutorId = appUtil.userIdFromAuthentication();
        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        Wallet wallet = getOrCreateWalletForUpdate(tutorId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .id(appUtil.generateUUID())
                .type(TransactionType.TOP_UP)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .transactionDate(Instant.now())
                .tutor(wallet.getTutor())
                .build();

        return transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    // Khi số dư không đủ vẫn phải lưu giao dịch FAILED nên không rollback với exception này
    @Override
    @Transactional(noRollbackFor = PaymentInsufficientBalanceException.class)
    @PreAuthorize("hasAuthority('TUTOR')")
    public TransactionResponse payCommission(UUID applicationId) {
        UUID tutorId = appUtil.userIdFromAuthentication();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.APPLICATION_NOT_FOUND));

        if (!tutorId.equals(resolveTutorId(application))) {
            throw new AppException(PaymentErrorCode.NOT_APPLICATION_TUTOR);
        }

        ApplicationStatus applicationStatus = application.getApplicationStatus();
        if (applicationStatus != ApplicationStatus.ACCEPTED
                && applicationStatus != ApplicationStatus.FINISHED) {
            throw new AppException(PaymentErrorCode.APPLICATION_NOT_PAYABLE);
        }

        BigDecimal amount = calculateCommission(application.getPost());

        // Khóa ví trước để các lần thanh toán của cùng 1 tutor được xử lý tuần tự
        Wallet wallet = getOrCreateWalletForUpdate(tutorId);

        Transaction transaction = transactionRepository.findByApplicationId(applicationId).orElse(null);

        // Chỉ cho thanh toán lại khi lần trước bị FAILED (không đủ số dư)
        if (transaction != null && transaction.getStatus() != TransactionStatus.FAILED) {
            throw new AppException(PaymentErrorCode.APPLICATION_ALREADY_PAID);
        }

        if (transaction == null) {
            transaction = Transaction.builder()
                    .id(appUtil.generateUUID())
                    .type(TransactionType.COMMISSION)
                    .tutor(wallet.getTutor())
                    .application(application)
                    .build();
        }
        transaction.setAmount(amount);
        transaction.setPaymentMethod(PaymentMethod.WALLET);
        transaction.setTransactionDate(Instant.now());

        if (wallet.getBalance().compareTo(amount) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new PaymentInsufficientBalanceException();
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        transaction.setStatus(TransactionStatus.SUCCESS);

        return transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TUTOR')")
    public CursorResponse<TransactionResponse> getMyTransactions(UUID cursor, int limit) {
        UUID tutorId = appUtil.userIdFromAuthentication();
        int size = normalizeLimit(limit);
        Pageable pageable = buildPageable(size);

        List<Transaction> transactions = cursor == null
                ? transactionRepository.findByTutorId(tutorId, pageable)
                : transactionRepository.findByTutorIdAndIdLessThan(tutorId, cursor, pageable);

        return appUtil.buildCursorResponse(
                transactions,
                size,
                Transaction::getId,
                transactionMapper::toTransactionResponse
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public CursorResponse<TransactionResponse> getAllTransactions(String status, UUID cursor, int limit) {
        TransactionStatus transactionStatus = parseStatusFilter(status);
        int size = normalizeLimit(limit);
        Pageable pageable = buildPageable(size);

        List<Transaction> transactions;
        if (transactionStatus == null) {
            transactions = cursor == null
                    ? transactionRepository.findAllBy(pageable)
                    : transactionRepository.findByIdLessThan(cursor, pageable);
        } else {
            transactions = cursor == null
                    ? transactionRepository.findByStatus(transactionStatus, pageable)
                    : transactionRepository.findByStatusAndIdLessThan(transactionStatus, cursor, pageable);
        }

        return appUtil.buildCursorResponse(
                transactions,
                size,
                Transaction::getId,
                transactionMapper::toTransactionResponse
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public TransactionResponse refundTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findForUpdateById(transactionId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getType() != TransactionType.COMMISSION
                || transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new AppException(PaymentErrorCode.TRANSACTION_NOT_REFUNDABLE);
        }

        Wallet wallet = getOrCreateWalletForUpdate(transaction.getTutor().getId());
        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(wallet);

        transaction.setStatus(TransactionStatus.REFUNDED);

        return transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RevenueStatisticsResponse getRevenueStatistics(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new AppException(PaymentErrorCode.INVALID_DATE_RANGE);
        }

        // [from 00:00 UTC, to + 1 ngày 00:00 UTC) để lấy trọn ngày "to"
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        BigDecimal totalTopUp = BigDecimal.ZERO;
        long successCount = 0;
        long failedCount = 0;
        long refundedCount = 0;
        long topUpCount = 0;
        long totalTransactions = 0;

        for (TransactionStatRow row : transactionRepository.summarize(start, end)) {
            BigDecimal rowTotal = row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount();
            totalTransactions += row.getTransactionCount();

            if (row.getTransactionType() == TransactionType.TOP_UP) {
                if (row.getTransactionStatus() == TransactionStatus.SUCCESS) {
                    totalTopUp = totalTopUp.add(rowTotal);
                    topUpCount += row.getTransactionCount();
                }
                continue;
            }

            switch (row.getTransactionStatus()) {
                case SUCCESS -> {
                    totalRevenue = totalRevenue.add(rowTotal);
                    successCount += row.getTransactionCount();
                }
                case FAILED -> failedCount += row.getTransactionCount();
                case REFUNDED -> {
                    totalRefunded = totalRefunded.add(rowTotal);
                    refundedCount += row.getTransactionCount();
                }
                default -> {
                    // PENDING / CANCELLED chỉ tính vào totalTransactions
                }
            }
        }

        return RevenueStatisticsResponse.builder()
                .from(from)
                .to(to)
                .totalRevenue(totalRevenue)
                .totalRefunded(totalRefunded)
                .totalTopUp(totalTopUp)
                .successCount(successCount)
                .failedCount(failedCount)
                .refundedCount(refundedCount)
                .topUpCount(topUpCount)
                .totalTransactions(totalTransactions)
                .build();
    }

    private Wallet getOrCreateWalletForUpdate(UUID tutorId) {
        return walletRepository.findForUpdateByTutorId(tutorId)
                .orElseGet(() -> createWallet(tutorId));
    }

    private Wallet createWallet(UUID tutorId) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(PaymentTutorNotFoundException::new);

        Wallet wallet = Wallet.builder()
                .id(appUtil.generateUUID())
                .balance(BigDecimal.ZERO)
                .tutor(tutor)
                .build();

        return walletRepository.save(wallet);
    }

    // Bài FIND_TUTOR: tutor là người ứng tuyển (application.tutor)
    // Bài FIND_STUDENT: tutor là tác giả bài đăng, student mới là người ứng tuyển
    private UUID resolveTutorId(Application application) {
        if (application.getTutor() != null) {
            return application.getTutor().getId();
        }
        return application.getPost().getAuthor().getId();
    }

    private BigDecimal calculateCommission(Post post) {
        BigDecimal base = post.getMaxBudget() != null ? post.getMaxBudget() : post.getMinBudget();

        if (base == null || base.signum() <= 0) {
            throw new AppException(PaymentErrorCode.POST_BUDGET_MISSING);
        }

        return base.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    // Query param nhận success | failed | refund (không phân biệt hoa thường), bỏ trống = tất cả
    private TransactionStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return switch (status.trim().toLowerCase()) {
            case "success" -> TransactionStatus.SUCCESS;
            case "failed" -> TransactionStatus.FAILED;
            case "refund" -> TransactionStatus.REFUNDED;
            default -> throw new AppException(PaymentErrorCode.INVALID_STATUS_FILTER);
        };
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    }

    private Pageable buildPageable(int size) {
        return PageRequest.of(0, size + 1, Sort.by(Sort.Order.desc("id")));
    }
}
