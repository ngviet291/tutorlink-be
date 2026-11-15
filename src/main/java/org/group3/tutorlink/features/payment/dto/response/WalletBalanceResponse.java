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
@Schema(description = "Số dư ví của gia sư đang đăng nhập.")
public class WalletBalanceResponse {

    @Schema(description = "Mã ví.")
    private UUID walletId;

    @Schema(description = "Số dư hiện tại, đơn vị VND.", example = "500000.00")
    private BigDecimal balance;
}
