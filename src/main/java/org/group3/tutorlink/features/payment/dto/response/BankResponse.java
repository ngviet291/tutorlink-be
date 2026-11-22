package org.group3.tutorlink.features.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.group3.tutorlink.features.payment.gateway.bank.BankInfo;

@Getter
@Builder
public class BankResponse {

    @Schema(description = "Mã ngân hàng, dùng làm bankCode khi tạo QR (vd VCB, MB).")
    private final String code;

    @Schema(description = "Mã BIN của ngân hàng.")
    private final String bin;

    @Schema(description = "Tên đầy đủ của ngân hàng.")
    private final String name;

    @Schema(description = "Tên viết tắt hiển thị.")
    private final String shortName;

    @Schema(description = "Đường dẫn logo.")
    private final String logo;

    @Schema(description = "Có hỗ trợ chuyển khoản qua VietQR hay không.")
    private final boolean transferSupported;

    @Schema(description = "Có hỗ trợ tra cứu tên chủ tài khoản hay không.")
    private final boolean lookupSupported;

    public static BankResponse from(BankInfo bank) {
        return BankResponse.builder()
                .code(bank.getCode())
                .bin(bank.getBin())
                .name(bank.getName())
                .shortName(bank.getShortName())
                .logo(bank.getLogo())
                .transferSupported(bank.isTransferSupported())
                .lookupSupported(bank.isLookupSupported())
                .build();
    }
}