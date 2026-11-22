package org.group3.tutorlink.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tỉnh / thành phố")
public class ProvinceResponse {

    @Schema(example = "79")
    private Integer code;

    @Schema(example = "Thành phố Hồ Chí Minh")
    private String name;

    @Schema(example = "ho_chi_minh")
    private String codename;
}