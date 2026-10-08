package org.group3.tutorlink.features.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wallet balance of the current tutor!")
public class WalletBalanceResponse {

    @Schema(description = "Wallet id!")
    private UUID walletId;

    @Schema(description = "Current balance (VND)!", example = "500000.00")
    private BigDecimal balance;
}
