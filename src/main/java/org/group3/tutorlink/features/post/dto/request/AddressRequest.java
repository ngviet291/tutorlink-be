package org.group3.tutorlink.features.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Address information")
public class AddressRequest {

    @Schema(
            description = "Street address",
            example = "123 Nguyễn Văn Linh"
    )
    private String street;

    @Schema(
            description = "Ward",
            example = "Tân Phong"
    )
    private String ward;

    @Schema(
            description = "Province or city",
            example = "Hồ Chí Minh"
    )
    private String province;
}