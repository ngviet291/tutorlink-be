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
}
