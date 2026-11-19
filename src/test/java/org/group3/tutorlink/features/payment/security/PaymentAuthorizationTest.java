package org.group3.tutorlink.features.payment.security;

import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.payment.dto.response.TransactionResponse;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.mapper.FeeConfigMapper;
import org.group3.tutorlink.features.payment.mapper.RevenueStatisticsMapper;
import org.group3.tutorlink.features.payment.mapper.TransactionMapper;
import org.group3.tutorlink.features.payment.mapper.WalletMapper;
import org.group3.tutorlink.features.payment.repository.MatchingFeeConfigRepository;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.group3.tutorlink.features.payment.repository.WalletRepository;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.group3.tutorlink.features.payment.service.impl.PaymentServiceImpl;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.repository.AdminRepository;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Kiểm tra annotation qua proxy Spring, không khởi động ứng dụng hay kết nối CSDL. */
class PaymentAuthorizationTest {
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final TransactionMapper mapper = mock(TransactionMapper.class);
    private final AppUtil appUtil = mock(AppUtil.class);
    private final UUID transactionId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private AnnotationConfigApplicationContext context;
    private PaymentService service;
    private TransactionResponse response;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
                new org.springframework.core.env.MapPropertySource("payment-test", java.util.Map.of(
                        "payment.pagination.default-limit", 10,
                        "payment.pagination.max-limit", 50,
                        "payment.commission-rate", "0.2")));
        context.register(SecurityConfiguration.class);
        context.registerBean(TransactionRepository.class, () -> transactions);
        context.registerBean(TransactionMapper.class, () -> mapper);
        context.registerBean(AppUtil.class, () -> appUtil);
        context.registerBean(WalletRepository.class, () -> mock(WalletRepository.class));
        context.registerBean(TutorRepository.class, () -> mock(TutorRepository.class));
        context.registerBean(ApplicationRepository.class, () -> mock(ApplicationRepository.class));
        context.registerBean(MatchingFeeConfigRepository.class, () -> mock(MatchingFeeConfigRepository.class));
        context.registerBean(AdminRepository.class, () -> mock(AdminRepository.class));
        context.registerBean(WalletMapper.class, () -> mock(WalletMapper.class));
        context.registerBean(RevenueStatisticsMapper.class, () -> mock(RevenueStatisticsMapper.class));
        context.registerBean(FeeConfigMapper.class, () -> mock(FeeConfigMapper.class));
        context.registerBean("paymentAccess", PaymentAccess.class);
        context.registerBean(PaymentServiceImpl.class);
        context.refresh();
        service = context.getBean(PaymentService.class);

        Tutor tutor = mock(Tutor.class);
        when(tutor.getId()).thenReturn(ownerId);
        Transaction transaction = mock(Transaction.class);
        when(transaction.getTutor()).thenReturn(tutor);
        when(transactions.findById(transactionId)).thenReturn(Optional.of(transaction));
        response = mock(TransactionResponse.class);
        when(mapper.toTransactionResponse(transaction)).thenReturn(response);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void adminCanReadWithoutOwningTransaction() {
        authenticate("ADMIN", UUID.randomUUID());
        assertSame(response, service.getTransactionDetail(transactionId));
        verifyNoInteractions(appUtil);
    }

    @Test
    void ownerCanReadOwnTransaction() {
        authenticate("TUTOR", ownerId);
        assertSame(response, service.getTransactionDetail(transactionId));
    }

    @Test
    void anotherTutorCannotReadTransaction() {
        authenticate("TUTOR", UUID.randomUUID());
        Throwable failure = assertThrows(RuntimeException.class,
                () -> service.getTransactionDetail(transactionId));
        while (!(failure instanceof AppException) && failure.getCause() != null) {
            failure = failure.getCause();
        }
        AppException denied = assertInstanceOf(AppException.class, failure);
        assertEquals(PaymentErrorCode.NOT_TRANSACTION_OWNER, denied.getErrorCode());
        verifyNoInteractions(mapper);
    }

    @Test
    void tutorCannotRefundTransaction() {
        authenticate("TUTOR", ownerId);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.refundTransaction(transactionId));
        verify(transactions, never()).findForUpdateById(any());
    }

    private void authenticate(String authority, UUID userId) {
        when(appUtil.userIdFromAuthentication()).thenReturn(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, List.of(new SimpleGrantedAuthority(authority))));
    }

    @Configuration
    @EnableMethodSecurity
    static class SecurityConfiguration {
    }
}

