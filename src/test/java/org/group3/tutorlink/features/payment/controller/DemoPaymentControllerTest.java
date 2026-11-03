package org.group3.tutorlink.features.payment.controller;

import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.gateway.mock.MockPaymentGateway;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DemoPaymentControllerTest {
    @Test
    void ownershipRejectionPreventsGatewayMutation() {
        var service = mock(PaymentService.class);
        var gateway = mock(MockPaymentGateway.class);
        var id = UUID.randomUUID();
        when(service.syncTopUpStatus(id)).thenThrow(new AppException(PaymentErrorCode.NOT_TRANSACTION_OWNER));
        var controller = new DemoPaymentController(service, gateway);
        assertThrows(AppException.class, () -> controller.confirm(id));
        verifyNoInteractions(gateway);
    }
}
