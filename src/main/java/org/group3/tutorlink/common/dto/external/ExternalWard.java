package org.group3.tutorlink.common.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalWard {

    private Integer code;
    private String name;
    private String codename;

    @JsonProperty("division_type")
    private String divisionType;

    @JsonProperty("province_code")
    private Integer provinceCode;
}