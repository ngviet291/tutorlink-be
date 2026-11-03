package org.group3.tutorlink.features.payment.gateway.mock;

import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.gateway.GatewayCallbackResult;
import org.group3.tutorlink.features.payment.gateway.GatewayPaymentRequest;
import org.group3.tutorlink.features.payment.gateway.GatewayPaymentResult;
import org.group3.tutorlink.features.payment.gateway.GatewayPaymentState;
import org.group3.tutorlink.features.payment.gateway.PaymentGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cổng giả lập phát triển, bật bằng payment.demo.enabled=true.
 * Đơn được giữ trong bộ nhớ và chỉ thành công sau khi chủ đơn xác nhận qua controller.
 * Gateway không cộng ví; service thanh toán xử lý số dư và lưu giao dịch.
 */
@Component
@ConditionalOnProperty(name = "payment.demo.enabled", havingValue = "true")
public class MockPaymentGateway implements PaymentGateway {
    private final Map<String, MockOrder> orders = new ConcurrentHashMap<>();

    @Override
    public PaymentMethod method() {
        // Thay thế gateway MoMo trong chế độ demo, giữ nguyên hợp đồng API nạp ví.
        return PaymentMethod.MOMO;
    }

    @Override
    public String newProviderOrderId() {
        return "MOCK" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public GatewayPaymentResult createPayment(GatewayPaymentRequest request) {
        orders.put(request.providerOrderId(), new MockOrder(
                "MOCK_TRANS_" + UUID.randomUUID(), request.amount(), GatewayPaymentState.PENDING));
        // FE xác nhận tại chỗ, không tạo liên kết hoặc QR giả.
        return new GatewayPaymentResult(null, null, null);
    }

    @Override
    public GatewayCallbackResult verifyCallback(String rawBody) {
        // IPN công khai không được dùng để xác nhận đơn demo.
        throw new AppException(PaymentErrorCode.INVALID_GATEWAY_SIGNATURE);
    }

    /** Chỉ gọi sau khi service đã kiểm tra quyền sở hữu giao dịch. */
    public void confirmDemo(String providerOrderId) {
        orders.compute(providerOrderId, (id, order) -> {
            if (order == null) {
                throw new AppException(PaymentErrorCode.TRANSACTION_NOT_FOUND);
            }
            return new MockOrder(order.transId(), order.amount(), GatewayPaymentState.SUCCESS);
        });
    }

    @Override
    public GatewayCallbackResult queryOrder(String providerOrderId) {
        MockOrder order = orders.get(providerOrderId);
        if (order == null) {
            // Đơn không còn trong bộ nhớ (ví dụ sau restart) không được cộng tiền.
            return new GatewayCallbackResult(providerOrderId, null, null, GatewayPaymentState.FAILED);
        }
        return new GatewayCallbackResult(providerOrderId, order.transId(), order.amount(), order.state());
    }

    private record MockOrder(String transId, BigDecimal amount, GatewayPaymentState state) {}
}
