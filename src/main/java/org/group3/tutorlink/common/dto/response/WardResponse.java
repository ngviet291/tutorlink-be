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
@Schema(description = "Phường / xã / đặc khu")
public class WardResponse {

    private Integer code;

    private String name;

    @Schema(description = "Mã tỉnh chứa phường/xã này", example = "79")
    private Integer provinceCode;
}