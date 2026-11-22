package org.group3.tutorlink.features.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Pattern(regexp = "^\\d{6,19}$", message = "Bank account number must be 6 to 19 digits")
    private String bankAccountNumber;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @NotBlank(message = "Account holder name is required")
    @Pattern(regexp = "^[\\p{L} ]{2,50}$", message = "Account holder name must be 2 to 50 letters")
    private String accountHolderName;
}