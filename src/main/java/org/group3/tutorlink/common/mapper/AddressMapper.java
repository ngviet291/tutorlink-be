package org.group3.tutorlink.common.mapper;

import org.group3.tutorlink.common.dto.external.ExternalProvince;
import org.group3.tutorlink.common.dto.external.ExternalWard;
import org.group3.tutorlink.common.dto.response.ProvinceResponse;
import org.group3.tutorlink.common.dto.response.WardResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    ProvinceResponse toProvince(ExternalProvince source);

    List<ProvinceResponse> toProvinces(List<ExternalProvince> source);

    /** API ngoài không phải lúc nào cũng trả province_code, nên lấy mã tỉnh từ đường dẫn làm dự phòng. */
    @Mapping(target = "code", source = "ward.code")
    @Mapping(target = "name", source = "ward.name")
    @Mapping(target = "provinceCode",
            expression = "java(ward.getProvinceCode() != null ? ward.getProvinceCode() : provinceCode)")
    WardResponse toWard(ExternalWard ward, Integer provinceCode);

    default List<WardResponse> toWards(List<ExternalWard> wards, Integer provinceCode) {
        if (wards == null) {
            return List.of();
        }
        return wards.stream().map(w -> toWard(w, provinceCode)).toList();
    }
}