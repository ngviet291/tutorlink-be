package org.group3.tutorlink.features.payment.gateway.bank;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Thông tin một ngân hàng do nhà cung cấp danh sách ngân hàng trả về (độc lập với DTO của API). */
@Getter
@AllArgsConstructor
public class BankInfo {
    private final Integer id;
    private final String code;
    private final String bin;
    private final String name;
    private final String shortName;
    private final String logo;
    private final boolean transferSupported;
    private final boolean lookupSupported;
}