package org.group3.tutorlink.features.payment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.utils.AfterCommit;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.scheduler.ApplicationExpiryQueue;
import org.group3.tutorlink.features.application.util.ApplicationParties;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.payment.dto.request.CreateFeeConfigRequest;
import org.group3.tutorlink.features.payment.dto.request.TopUpRequest;
import org.group3.tutorlink.features.payment.dto.response.*;
import org.group3.tutorlink.features.payment.entity.MatchingFeeConfig;
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
import org.group3.tutorlink.features.payment.mapper.FeeConfigMapper;
import org.group3.tutorlink.features.payment.mapper.RevenueStatisticsMapper;
import org.group3.tutorlink.features.payment.mapper.TransactionMapper;
import org.group3.tutorlink.features.payment.mapper.WalletMapper;
import org.group3.tutorlink.features.payment.repository.MatchingFeeConfigRepository;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.group3.tutorlink.features.payment.dto.projection.RevenueStatisticsRow;
import org.group3.tutorlink.features.payment.repository.WalletRepository;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.BudgetUnit;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.repository.AdminRepository;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int COMMISSION_AMOUNT_SCALE = 2;
    private static final int FEE_CONFIG_LOOKAHEAD_YEARS = 100;
    private static final String TOP_UP_DESCRIPTION = "Nap tiền vào ví";

    @Value("${payment.pagination.default-limit:10}")
    private int defaultLimit;

    @Value("${payment.pagination.max-limit:50}")
    private int maxLimit;

    @Value("${payment.commission-rate:0.20}")
    private BigDecimal commissionRate;

    // [APPLICATION-EXPIRY] Cửa sổ chờ thanh toán (W1) — phải khớp
    // ApplicationExpiryScheduler/ApplicationMapper (module application).
    // Quá hạn này đơn sẽ bị scheduler CANCELLED, không nhận thanh toán nữa.
    @Value("${application.expiry.payment-window-minutes:1440}")
    private long paymentWindowMinutes;

    // [APPLICATION-EXPIRY] Cửa sổ chờ applicant xác nhận (W2) — phải khớp
    // ApplicationExpiryService/ApplicationMapper (module application).
    @Value("${application.expiry.confirm-window-minutes:1440}")
    private long confirmWindowMinutes;

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TutorRepository tutorRepository;
    private final ApplicationRepository applicationRepository;
    private final MatchingFeeConfigRepository feeConfigRepository;
    private final AdminRepository adminRepository;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final RevenueStatisticsMapper revenueStatisticsMapper;
    private final FeeConfigMapper feeConfigMapper;
    private final List<PaymentGateway> paymentGateways;
    private final AppUtil appUtil;
    private final org.group3.tutorlink.features.chat.service.ChatService chatService;
    private final ApplicationExpiryQueue expiryQueue;
    private final AfterCommit afterCommit;
    private final NotificationHelper notificationHelper;

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
        // Nạp ví qua MoMo (mặc định) hoặc VietQR.
        PaymentGateway gateway = findGateway(request.getPaymentMethod());
        BigDecimal amount = request.getAmount().setScale(0, RoundingMode.HALF_UP);

        Wallet wallet = walletRepository.findByTutorId(tutorId)
                .orElseGet(() -> createWallet(tutorId));

        // Tạo đơn bên cổng thanh toán trước; lỗi cổng -> ném exception, không lưu giao dịch rác
        String providerOrderId = gateway.newProviderOrderId();
        GatewayPaymentResult payment = gateway.createPayment(new GatewayPaymentRequest(
                providerOrderId, amount, TOP_UP_DESCRIPTION, tutorId));

        // Giao dịch ở trạng thái chờ; chỉ cộng tiền khi cổng thanh toán xác nhận qua IPN/callback
        Transaction transaction = Transaction.builder()
                .id(appUtil.generateUUID())
                .type(TransactionType.TOP_UP)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .providerOrderId(providerOrderId)
                .transactionDate(Instant.now())
                .tutor(wallet.getTutor())
                .build();

        TransactionResponse response = transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
        response.setPaymentLink(payment.getPayUrl());
        response.setQrCodeUrl(payment.getQrCodeUrl());
        response.setDeeplink(payment.getDeeplink());
        return response;
    }

    @Override
    @Transactional
    public void handleGatewayCallback(PaymentMethod method, String rawBody) {
        // Sai chữ ký -> AppException(INVALID_GATEWAY_SIGNATURE), không đụng tới ví
        GatewayCallbackResult result = findGateway(method).verifyCallback(rawBody);

        Transaction transaction = transactionRepository.findForUpdateByProviderOrderId(result.getProviderOrderId())
                .orElse(null);
        if (transaction == null) {
            // Yêu cầu cổng gửi lại thông báo nếu giao dịch tạo đơn chưa được ghi nhận.
            throw new AppException(PaymentErrorCode.TRANSACTION_NOT_FOUND);
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

    // Chốt kết quả giao dịch nạp tiền. Chỉ xử lý khi giao dịch còn PENDING để không cộng ví lặp.
    private void settleTopUp(Transaction transaction, PaymentMethod method, GatewayCallbackResult result) {
        if (!java.util.Objects.equals(transaction.getProviderOrderId(), result.getProviderOrderId())
                || transaction.getType() != TransactionType.TOP_UP || transaction.getPaymentMethod() != method) {
            log.warn("Ignore {} result for transaction {}: type/method mismatch", method, transaction.getId());
            return;
        }
        if (transaction.getStatus() != TransactionStatus.PENDING || result.getState() == GatewayPaymentState.PENDING) {
            return;
        }

        if (result.getState() == GatewayPaymentState.SUCCESS) {
            if (result.getAmount() == null || transaction.getAmount().compareTo(result.getAmount()) != 0
                    || result.getProviderTransId() == null || result.getProviderTransId().isBlank()) {
                // Chữ ký hợp lệ nhưng số tiền lệch với đơn đã tạo -> không cộng ví, cần đối soát thủ công
                log.error("{} amount mismatch for transaction {}: expected {}, gateway {}, providerTransId {}",
                        method, transaction.getId(), transaction.getAmount(), result.getAmount(),
                        result.getProviderTransId());
                if (method == PaymentMethod.VIETQR) {
                    // Chuyển khoản thật đã bị trừ tiền: giữ PENDING để đối soát thủ công, không đóng giao dịch.
                    return;
                }
                transaction.setStatus(TransactionStatus.FAILED);
                notificationHelper.notify(transaction.getTutor(), NotificationMessage.TOP_UP_MISMATCH,
                        NotificationTarget.WALLET, null, notificationHelper.money(transaction.getAmount()));
                notificationHelper.notifyAdmins(NotificationMessage.TOP_UP_MISMATCH_ADMIN,
                        NotificationTarget.NONE, transaction.getId(), transaction.getTutor().getFullName());
            } else {
                Wallet wallet = getOrCreateWalletForUpdate(transaction.getTutor().getId());
                wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
                walletRepository.save(wallet);
                transaction.setStatus(TransactionStatus.SUCCESS);
                notificationHelper.notify(transaction.getTutor(), NotificationMessage.TOP_UP_SUCCESS,
                        NotificationTarget.WALLET, null, notificationHelper.money(transaction.getAmount()));
            }
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            notificationHelper.notify(transaction.getTutor(), NotificationMessage.TOP_UP_FAILED,
                    NotificationTarget.WALLET, null, notificationHelper.money(transaction.getAmount()));
        }

        if (result.getProviderTransId() != null && !result.getProviderTransId().isBlank()) {
            transaction.setProviderTransId(result.getProviderTransId());
        }
        transaction.setTransactionDate(Instant.now());
        transactionRepository.save(transaction);
    }

    private PaymentMethod resolveTopUpMethod(PaymentMethod requested) {
        PaymentMethod method = requested == null ? PaymentMethod.MOMO : requested;
        if (method != PaymentMethod.MOMO && method != PaymentMethod.VIETQR) {
            throw new AppException(PaymentErrorCode.UNSUPPORTED_TOP_UP_METHOD);
        }
        return method;
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

        // Chỉ cho thanh toán lại khi lần trước bị FAILED (không đủ số dư) hoặc REFUNDED (đã hoàn tiền)
        if (transaction != null
                && transaction.getStatus() != TransactionStatus.FAILED
                && transaction.getStatus() != TransactionStatus.REFUNDED) {
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
            notificationHelper.notify(wallet.getTutor(), NotificationMessage.BALANCE_LOW,
                    NotificationTarget.WALLET, null,
                    notificationHelper.money(amount), notificationHelper.money(wallet.getBalance()));
            throw new PaymentInsufficientBalanceException();
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        transaction.setStatus(TransactionStatus.SUCCESS);
        Transaction saved = transactionRepository.save(transaction);
        String postTitle = application.getPost().getTitle();
        notificationHelper.notify(wallet.getTutor(), NotificationMessage.FEE_PAID_TUTOR,
                NotificationTarget.WALLET, null, notificationHelper.money(amount), postTitle);
        // Học viên của đơn: kênh chat vừa được mở
        notificationHelper.notify(application.getStudent(), NotificationMessage.FEE_PAID_STUDENT,
                NotificationTarget.CHAT, null, postTitle);
        // Người ứng tuyển cần xác nhận lớp
        notificationHelper.notify(ApplicationParties.applicantOf(application),
                NotificationMessage.APPLICATION_CONFIRM_REQUIRED,
                NotificationTarget.MY_APPLICATIONS, null, postTitle);
        // Gia sư được chọn đã trả phí -> lúc này các ứng viên còn lại mới bị loại
        if (applicationStatus == ApplicationStatus.ACCEPTED) {
            rejectOtherPendingApplications(application);
        }
        createChatChannelForApplication(application);

        return transactionMapper.toTransactionResponse(saved);
    }

    // Chỉ khi gia sư được chọn đã trả phí thì các đơn còn lại mới bị loại
    private void rejectOtherPendingApplications(Application paid) {
        Post post = paid.getPost();
        Page<Application> pending = applicationRepository.findByPostIdAndApplicationStatus(
                post.getId(), ApplicationStatus.PENDING, Pageable.unpaged());
        for (Application other : pending.getContent()) {
            if (other.getId().equals(paid.getId())) {
                continue;
            }
            other.setApplicationStatus(ApplicationStatus.REJECTED);
            notificationHelper.notify(ApplicationParties.applicantOf(other),
                    NotificationMessage.APPLICATION_REJECTED_OTHER_CHOSEN,
                    NotificationTarget.POST, post.getId(), post.getTitle());
        }
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
    public CursorResponse<TransactionResponse> getAllTransactions(
            TransactionStatus transactionStatus,
            TransactionType transactionType,
            UUID cursor,
            int limit
    ) {
        int size = normalizeLimit(limit);
        Pageable pageable = buildPageable(size);

        List<Transaction> transactions;

        if (transactionStatus == null && transactionType == null) {
            transactions = cursor == null
                    ? transactionRepository.findAllBy(pageable)
                    : transactionRepository.findByIdLessThan(cursor, pageable);
        } else if (transactionStatus == null) {
            transactions = cursor == null
                    ? transactionRepository.findByType(transactionType, pageable)
                    : transactionRepository.findByTypeAndIdLessThan(transactionType, cursor, pageable);
        } else if (transactionType == null) {
            transactions = cursor == null
                    ? transactionRepository.findByStatus(transactionStatus, pageable)
                    : transactionRepository.findByStatusAndIdLessThan(transactionStatus, cursor, pageable);
        } else {
            transactions = cursor == null
                    ? transactionRepository.findByStatusAndType(transactionStatus, transactionType, pageable)
                    : transactionRepository.findByStatusAndTypeAndIdLessThan(transactionStatus, transactionType, cursor, pageable);
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
        Transaction saved = transactionRepository.save(transaction);
        notificationHelper.notify(saved.getTutor(), NotificationMessage.FEE_REFUNDED,
                NotificationTarget.WALLET, null, notificationHelper.money(saved.getAmount()));
        return transactionMapper.toTransactionResponse(saved);
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

        // Gộp số liệu bằng SQL (tổng hợp tại cơ sở dữ liệu), dùng bộ chuyển đổi để tạo dữ liệu trả về
        RevenueStatisticsRow row = transactionRepository.summarizeRevenue(start, end);
        return revenueStatisticsMapper.toResponse(row, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN') or @paymentAccess.isOwner(#p0)")
    public TransactionResponse getTransactionDetail(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        return transactionMapper.toTransactionResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public CursorResponse<FeeConfigResponse> getFeeConfigs(UUID cursor, int limit) {
        int size = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, size + 1, Sort.by(Sort.Order.desc("effectiveFrom")));

        List<MatchingFeeConfig> configs = cursor == null
                ? feeConfigRepository.findAllByOrderByEffectiveFromDesc(pageable)
                : feeConfigRepository.findByIdLessThanOrderByEffectiveFromDesc(cursor, pageable);

        return appUtil.buildCursorResponse(
                configs,
                size,
                MatchingFeeConfig::getId,
                feeConfigMapper::toFeeConfigResponse
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public FeeConfigResponse createFeeConfig(CreateFeeConfigRequest request) {
        UUID adminId = appUtil.userIdFromAuthentication();
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.TUTOR_NOT_FOUND)); // Tái sử dụng mã lỗi hiện có.

        LocalDate effectiveFromDate = request.getEffectiveFrom() != null
                ? request.getEffectiveFrom()
                : LocalDate.now();

        Instant effectiveFrom = effectiveFromDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant lookAheadEnd = effectiveFromDate
                .plusYears(FEE_CONFIG_LOOKAHEAD_YEARS)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<MatchingFeeConfig> overlapping =
                feeConfigRepository.findOverlappingConfigs(effectiveFrom, lookAheadEnd);
        if (!overlapping.isEmpty()) {
            for (MatchingFeeConfig old : overlapping) {
                if (old.getEffectiveTo() == null
                        || old.getEffectiveTo().isAfter(effectiveFrom)) {

                    old.setEffectiveTo(
                            effectiveFrom.minus(1, ChronoUnit.DAYS)
                    );

                    feeConfigRepository.save(old);
                }
            }
        }

        MatchingFeeConfig config = MatchingFeeConfig.builder()
                .id(appUtil.generateUUID())
                .feePercentage(request.getFeePercentage())
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .createdByAdmin(admin)
                .build();

        return feeConfigMapper.toFeeConfigResponse(
                feeConfigRepository.save(config)
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TUTOR')")
    public FeeQuoteResponse getFeeQuote(UUID applicationId) {
        UUID tutorId = appUtil.userIdFromAuthentication();
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.APPLICATION_NOT_FOUND));
        if (!tutorId.equals(resolveTutorId(application))) {
            throw new AppException(PaymentErrorCode.NOT_APPLICATION_TUTOR);
        }
        Post post = application.getPost();
        BigDecimal baseBudget = post.getMaxBudget() != null ? post.getMaxBudget() : post.getMinBudget();
        BigDecimal pricePerSession = convertToPerSession(baseBudget, post);

        return FeeQuoteResponse.builder()
                .applicationId(applicationId)
                .baseAmount(pricePerSession)
                .feeRate(commissionRate)
                .feeAmount(calculateCommission(post))
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
        BigDecimal baseBudget = post.getMaxBudget() != null ? post.getMaxBudget() : post.getMinBudget();

        if (baseBudget == null || baseBudget.signum() <= 0) {
            throw new AppException(PaymentErrorCode.POST_BUDGET_MISSING);
        }

        BigDecimal pricePerSession = convertToPerSession(baseBudget, post);

        return pricePerSession.multiply(commissionRate).setScale(COMMISSION_AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal convertToPerSession(BigDecimal budget, Post post) {
        BudgetUnit unit = post.getBudgetUnit();

        if (unit == null) {
            unit = BudgetUnit.PER_SESSION;
        }

        switch (unit) {
            case PER_SESSION:
                return budget;

            case PER_HOUR:
                Integer durationMinutes = post.getDurationMinutes();
                if (durationMinutes == null || durationMinutes <= 0) {
                    throw new AppException(PaymentErrorCode.POST_BUDGET_MISSING);
                }
                BigDecimal hours = BigDecimal.valueOf(durationMinutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
                return budget.multiply(hours);

            case PER_MONTH:
                Integer sessionsPerWeek = post.getSessionsPerWeek();
                if (sessionsPerWeek == null || sessionsPerWeek <= 0) {
                    throw new AppException(PaymentErrorCode.POST_BUDGET_MISSING);
                }
                BigDecimal sessionsPerMonth = BigDecimal.valueOf(sessionsPerWeek).multiply(BigDecimal.valueOf(4));
                return budget.divide(sessionsPerMonth, COMMISSION_AMOUNT_SCALE, RoundingMode.HALF_UP);

            default:
                return budget;
        }
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? defaultLimit : Math.min(limit, maxLimit);
    }

    private Pageable buildPageable(int size) {
        return PageRequest.of(0, size + 1, Sort.by(Sort.Order.desc("id")));
    }

    private void createChatChannelForApplication(Application application) {
        try {
            org.group3.tutorlink.features.user.entity.Student student = application.getStudent();
            org.group3.tutorlink.features.user.entity.Tutor tutor;

            if (application.getTutor() != null) {
                tutor = application.getTutor();
            } else {
                tutor = (org.group3.tutorlink.features.user.entity.Tutor) application.getPost().getAuthor();
            }

            if (student != null && tutor != null) {
                chatService.createChannel(tutor, student);
                log.info("Created chat channel for application {}: tutor={}, student={}",
                        application.getId(), tutor.getId(), student.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to create chat channel for application {}: {}",
                    application.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('TUTOR')")
    public TransactionResponse withdrawFromWallet(
            org.group3.tutorlink.features.payment.dto.request.WithdrawRequest request) {
        UUID tutorId = appUtil.userIdFromAuthentication();

        BigDecimal amount = request.getAmount().setScale(0, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
            throw new AppException(PaymentErrorCode.WITHDRAW_AMOUNT_TOO_LOW);
        }

        Wallet wallet = getOrCreateWalletForUpdate(tutorId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new AppException(PaymentErrorCode.INSUFFICIENT_BALANCE_FOR_WITHDRAWAL);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .id(appUtil.generateUUID())
                .type(TransactionType.WITHDRAW)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.WALLET)
                .transactionDate(Instant.now())
                .tutor(wallet.getTutor())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .accountHolderName(request.getAccountHolderName())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Withdrawal request created: transactionId={}, tutorId={}, amount={}",
                saved.getId(), tutorId, amount);

        return transactionMapper.toTransactionResponse(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public TransactionResponse approveWithdrawal(UUID transactionId) {
        Transaction transaction = transactionRepository.findForUpdateById(transactionId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getType() != TransactionType.WITHDRAW
                || transaction.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(PaymentErrorCode.WITHDRAW_NOT_PENDING);
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setTransactionDate(Instant.now());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Withdrawal approved: transactionId={}, tutorId={}, amount={}",
                saved.getId(), saved.getTutor().getId(), saved.getAmount());

        return transactionMapper.toTransactionResponse(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public TransactionResponse rejectWithdrawal(UUID transactionId) {
        Transaction transaction = transactionRepository.findForUpdateById(transactionId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getType() != TransactionType.WITHDRAW
                || transaction.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(PaymentErrorCode.WITHDRAW_NOT_PENDING);
        }

        Wallet wallet = getOrCreateWalletForUpdate(transaction.getTutor().getId());
        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(wallet);

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setTransactionDate(Instant.now());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Withdrawal rejected and refunded: transactionId={}, tutorId={}, amount={}",
                saved.getId(), saved.getTutor().getId(), saved.getAmount());

        return transactionMapper.toTransactionResponse(saved);
    }
}