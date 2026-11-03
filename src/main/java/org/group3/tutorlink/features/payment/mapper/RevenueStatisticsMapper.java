package org.group3.tutorlink.features.payment.mapper;

import org.group3.tutorlink.features.payment.dto.response.RevenueStatisticsResponse;
import org.group3.tutorlink.features.payment.repository.RevenueStatisticsRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface RevenueStatisticsMapper {

    @Mapping(target = "from", source = "from")
    @Mapping(target = "to", source = "to")
    @Mapping(target = "totalRevenue", source = "row.totalRevenue", defaultExpression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "totalRefunded", source = "row.totalRefunded", defaultExpression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "totalTopUp", source = "row.totalTopUp", defaultExpression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "successCount", source = "row.successCount")
    @Mapping(target = "failedCount", source = "row.failedCount")
    @Mapping(target = "refundedCount", source = "row.refundedCount")
    @Mapping(target = "topUpCount", source = "row.topUpCount")
    @Mapping(target = "totalTransactions", source = "row.totalTransactions")
    RevenueStatisticsResponse toResponse(RevenueStatisticsRow row, LocalDate from, LocalDate to);
}
