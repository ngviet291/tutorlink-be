package org.group3.tutorlink.features.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to top up the wallet of the current tutor!")
public class TopUpRequest {

    @NotNull(message = "Amount is required!")
    @Positive(message = "Amount must be greater than 0!")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places!")
    @Schema(description = "Amount to top up (VND)!", example = "500000")
    private BigDecimal amount;
}
