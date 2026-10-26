package org.group3.tutorlink.features.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction information!")
public class TransactionResponse {

    @Schema(description = "Transaction id!")
    private UUID id;

    @Schema(description = "Transaction type!", example = "COMMISSION")
    private TransactionType type;

    @Schema(description = "Transaction amount (VND)!", example = "30000.00")
    private BigDecimal amount;

    @Schema(description = "Transaction status!", example = "SUCCESS")
    private TransactionStatus status;

    @Schema(description = "Payment method!", example = "WALLET")
    private PaymentMethod paymentMethod;

    @Schema(description = "Transaction time!")
    private Instant transactionDate;

    @Schema(description = "Tutor (wallet owner) id!")
    private UUID tutorId;

    @Schema(description = "Application id, only present for COMMISSION transactions!")
    private UUID applicationId;

    @Schema(description = "Order id on the payment gateway (MoMo orderId / ZaloPay app_trans_id), only for gateway top-ups!")
    private String providerOrderId;

    @Schema(description = "Gateway transaction id, available once the payment is completed!")
    private String providerTransId;

    @Schema(description = "URL to redirect the user to the MoMo/ZaloPay payment page (returned when creating a top-up)!")
    private String paymentLink;

    @Schema(description = "Content to render a QR code that can be scanned with the MoMo app (MoMo only)!")
    private String qrCodeUrl;

    @Schema(description = "Deep link that opens the MoMo app directly (MoMo only)!")
    private String deeplink;
}
