package org.group3.tutorlink.features.payment.mapper;

import org.group3.tutorlink.features.payment.dto.response.FeeConfigResponse;
import org.group3.tutorlink.features.payment.entity.MatchingFeeConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeeConfigMapper {

    @Mapping(source = "createdByAdmin.id", target = "createdByAdminId")
    FeeConfigResponse toFeeConfigResponse(MatchingFeeConfig config);
}
