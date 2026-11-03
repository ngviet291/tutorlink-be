package org.group3.tutorlink.common.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProvinceApiResponse {

    private Integer code;

    private String name;

    @JsonProperty("division_type")
    private String divisionType;

    private String codename;
}