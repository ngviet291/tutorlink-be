package org.group3.tutorlink.common.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Dữ liệu thô của https://provinces.open-api.vn/api/v2/p/ (giữ nguyên tên field của API ngoài). */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalProvince {

    private Integer code;
    private String name;
    private String codename;

    @JsonProperty("division_type")
    private String divisionType;

    @JsonProperty("phone_code")
    private Integer phoneCode;

    private List<ExternalWard> wards = new ArrayList<>();
}