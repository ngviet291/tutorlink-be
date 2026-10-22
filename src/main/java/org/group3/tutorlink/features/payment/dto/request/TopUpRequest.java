package org.group3.tutorlink.features.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to top up the wallet of the current tutor!")
public class TopUpRequest {

    @NotNull(message = "Amount is required!")
    @DecimalMin(value = "1000", message = "Amount must be at least 1,000 VND!")
    @DecimalMax(value = "50000000", message = "Amount must not exceed 50,000,000 VND!")
    @Digits(integer = 17, fraction = 0, message = "Amount must be a whole number of VND!")
    @Schema(description = "Amount to top up (VND)!", example = "500000")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required!")
    @Schema(description = "Payment gateway used to top up: MOMO or ZALOPAY!", example = "MOMO")
    private PaymentMethod paymentMethod;
}
