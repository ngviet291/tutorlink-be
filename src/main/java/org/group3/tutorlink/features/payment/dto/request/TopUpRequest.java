package org.group3.tutorlink.features.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.payment.constant.PaymentConstraints;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu nạp ví qua MoMo.")
public class TopUpRequest {

    @NotNull(message = "Amount is required!")
    @DecimalMin(value = PaymentConstraints.TOP_UP_MIN_AMOUNT, message = "Amount must be at least 10,000 VND!")
    @DecimalMax(value = PaymentConstraints.TOP_UP_MAX_AMOUNT, message = "Amount must not exceed 50,000,000 VND!")
    @Digits(integer = 17, fraction = 0, message = "Amount must be a whole number of VND!")
    @Schema(description = "Số tiền nạp từ 10.000 đến 50.000.000 VND.", example = "500000")
    private BigDecimal amount;

    @NotNull
    @NotBlank
    @Schema(description = "Phương thức nạp: MOMO (mặc định nếu bỏ trống) hoặc VIETQR.", example = "VIETQR")
    private PaymentMethod paymentMethod;
}
