package org.group3.tutorlink.features.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum withdrawal amount is 10,000 VND")
    private BigDecimal amount;

    @NotBlank(message = "Bank account number is required")
    private String bankAccountNumber;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;
}
