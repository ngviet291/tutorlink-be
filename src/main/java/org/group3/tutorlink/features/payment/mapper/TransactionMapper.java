package org.group3.tutorlink.features.payment.mapper;

import org.group3.tutorlink.features.payment.dto.response.TransactionResponse;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "tutor.id", target = "tutorId")
    @Mapping(source = "tutor.fullName", target = "tutorName")
    @Mapping(source = "application.id", target = "applicationId")
    // Liên kết thanh toán được bổ sung từ kết quả của cổng, không lưu trong thực thể.
    @Mapping(target = "paymentLink", ignore = true)
    @Mapping(target = "qrCodeUrl", ignore = true)
    @Mapping(target = "deeplink", ignore = true)
    TransactionResponse toTransactionResponse(Transaction transaction);
}
