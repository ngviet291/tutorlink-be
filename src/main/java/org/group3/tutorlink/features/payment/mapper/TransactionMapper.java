package org.group3.tutorlink.features.payment.mapper;

import org.group3.tutorlink.features.payment.dto.response.TransactionResponse;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "tutor.id", target = "tutorId")
    @Mapping(source = "application.id", target = "applicationId")
    TransactionResponse toTransactionResponse(Transaction transaction);
}
