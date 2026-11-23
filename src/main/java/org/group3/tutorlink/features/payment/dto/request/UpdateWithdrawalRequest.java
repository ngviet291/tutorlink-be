package org.group3.tutorlink.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWithdrawalRequest {
    @NotBlank
    @Pattern(regexp = "^\\d{6,19}$")
    private String bankAccountNumber;

    @NotBlank
    @Size(max = 100)
    private String bankName;

    @NotBlank
    @Pattern(regexp = "^[\\p{L} ]{2,50}$")
    private String accountHolderName;
}
