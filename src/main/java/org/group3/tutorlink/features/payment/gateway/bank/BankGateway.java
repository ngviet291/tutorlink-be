package org.group3.tutorlink.features.payment.gateway.bank;

import java.util.List;

/**
 * Nguồn danh sách ngân hàng. Service chỉ phụ thuộc interface này,
 * muốn đổi nhà cung cấp chỉ cần viết thêm một bản cài đặt.
 */
public interface BankGateway {

    /** Lấy danh sách ngân hàng; lỗi nhà cung cấp -> AppException(BANK_LIST_UNAVAILABLE). */
    List<BankInfo> fetchBanks();
}