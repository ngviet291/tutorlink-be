package org.group3.tutorlink.features.payment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.group3.tutorlink.features.payment.gateway.GatewayCallbackResult;
import org.group3.tutorlink.features.payment.gateway.GatewayPaymentRequest;
import org.group3.tutorlink.features.payment.gateway.GatewayPaymentResult;
import org.group3.tutorlink.features.payment.gateway.GatewayPaymentState;
import org.group3.tutorlink.features.payment.gateway.PaymentGateway;
import org.group3.tutorlink.features.payment.mapper.RevenueStatisticsMapper;
import org.group3.tutorlink.features.payment.mapper.TransactionMapper;
import org.group3.tutorlink.features.payment.mapper.WalletMapper;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.group3.tutorlink.features.payment.repository.RevenueStatisticsRow;
import org.group3.tutorlink.features.payment.repository.WalletRepository;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${payment.pagination.default-limit:10}")
    private int defaultLimit;

    @Value("${payment.pagination.max-limit:50}")
    private int maxLimit;

    @Value("${payment.commission-rate:0.20}")
    private BigDecimal commissionRate;

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TutorRepository tutorRepository;
    private final ApplicationRepository applicationRepository;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final RevenueStatisticsMapper revenueStatisticsMapper;
    // Spring tự inject tất cả cổng thanh toán (MomoGateway, ZaloPayGateway, ...)
    private final List<PaymentGateway> paymentGateways;
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
        PaymentMethod method = request.getPaymentMethod();
        PaymentGateway gateway = findGateway(method);
        BigDecimal amount = request.getAmount().setScale(0, RoundingMode.HALF_UP);

        Wallet wallet = walletRepository.findByTutorId(tutorId)
                .orElseGet(() -> createWallet(tutorId));

        // Tạo đơn bên MoMo/ZaloPay trước; lỗi cổng -> ném exception, không lưu giao dịch rác
        String providerOrderId = gateway.newProviderOrderId();
        GatewayPaymentResult payment = gateway.createPayment(new GatewayPaymentRequest(
                providerOrderId, amount, "Nap tien vao vi TutorLink", tutorId));

        // Giao dịch ở trạng thái chờ; chỉ cộng tiền khi cổng thanh toán xác nhận qua IPN/callback
        Transaction transaction = Transaction.builder()
                .id(appUtil.generateUUID())
                .type(TransactionType.TOP_UP)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .paymentMethod(method)
                .providerOrderId(providerOrderId)
                .transactionDate(Instant.now())
                .tutor(wallet.getTutor())
                .build();

        TransactionResponse response = transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
        response.setPaymentLink(payment.payUrl());
        response.setQrCodeUrl(payment.qrCodeUrl());
        response.setDeeplink(payment.deeplink());
        return response;
    }

    @Override
    @Transactional
    public void handleGatewayCallback(PaymentMethod method, String rawBody) {
        // Sai chữ ký -> AppException(INVALID_GATEWAY_SIGNATURE), không đụng tới ví
        GatewayCallbackResult result = findGateway(method).verifyCallback(rawBody);

        Transaction transaction = transactionRepository.findForUpdateByProviderOrderId(result.providerOrderId())
                .orElse(null);
        if (transaction == null) {
            log.warn("{} callback for unknown order {}", method, result.providerOrderId());
            return;
        }
        settleTopUp(transaction, method, result);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('TUTOR')")
    public TransactionResponse syncTopUpStatus(UUID transactionId) {
        UUID tutorId = appUtil.userIdFromAuthentication();

        Transaction transaction = transactionRepository.findForUpdateById(transactionId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        if (!tutorId.equals(transaction.getTutor().getId())) {
            throw new AppException(PaymentErrorCode.NOT_TRANSACTION_OWNER);
        }
        if (transaction.getType() != TransactionType.TOP_UP || transaction.getProviderOrderId() == null) {
            throw new AppException(PaymentErrorCode.TRANSACTION_NOT_TOP_UP);
        }

        // Chưa có kết quả cuối -> chủ động hỏi cổng (dùng khi IPN chưa tới, vd chạy localhost)
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            PaymentMethod method = transaction.getPaymentMethod();
            settleTopUp(transaction, method, findGateway(method).queryOrder(transaction.getProviderOrderId()));
        }

        return transactionMapper.toTransactionResponse(transaction);
    }

    // Chốt kết quả giao dịch nạp tiền. Idempotent: chỉ xử lý khi giao dịch còn PENDING.
    private void settleTopUp(Transaction transaction, PaymentMethod method, GatewayCallbackResult result) {
        if (transaction.getType() != TransactionType.TOP_UP || transaction.getPaymentMethod() != method) {
            log.warn("Ignore {} result for transaction {}: type/method mismatch", method, transaction.getId());
            return;
        }
        if (transaction.getStatus() != TransactionStatus.PENDING || result.state() == GatewayPaymentState.PENDING) {
            return;
        }

        if (result.state() == GatewayPaymentState.SUCCESS) {
            if (result.amount() != null && transaction.getAmount().compareTo(result.amount()) != 0) {
                // Chữ ký hợp lệ nhưng số tiền lệch với đơn đã tạo -> không cộng ví, cần đối soát thủ công
                log.error("{} amount mismatch for transaction {}: expected {}, gateway {}",
                        method, transaction.getId(), transaction.getAmount(), result.amount());
                transaction.setStatus(TransactionStatus.FAILED);
            } else {
                Wallet wallet = getOrCreateWalletForUpdate(transaction.getTutor().getId());
                wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
                walletRepository.save(wallet);
                transaction.setStatus(TransactionStatus.SUCCESS);
            }
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
        }

        if (result.providerTransId() != null && !result.providerTransId().isBlank()) {
            transaction.setProviderTransId(result.providerTransId());
        }
        transaction.setTransactionDate(Instant.now());
        transactionRepository.save(transaction);
    }

    private PaymentGateway findGateway(PaymentMethod method) {
        return paymentGateways.stream()
                .filter(gateway -> gateway.method() == method)
                .findFirst()
                .orElseThrow(() -> new AppException(PaymentErrorCode.UNSUPPORTED_TOP_UP_METHOD));
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
    public CursorResponse<TransactionResponse> getAllTransactions(TransactionStatus status, UUID cursor, int limit) {
        TransactionStatus transactionStatus = status;
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

        // Gộp số liệu bằng SQL (aggregate ở DB), dùng mapper để build response
        RevenueStatisticsRow row = transactionRepository.summarizeRevenue(start, end);
        return revenueStatisticsMapper.toResponse(row, from, to);
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

        return base.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? defaultLimit : Math.min(limit, maxLimit);
    }

    private Pageable buildPageable(int size) {
        return PageRequest.of(0, size + 1, Sort.by(Sort.Order.desc("id")));
    }
}
