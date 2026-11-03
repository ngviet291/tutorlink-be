package org.group3.tutorlink.features.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Address information")
public class AddressRequest {

    @NotBlank(message = "Street is required")
    @Size(max = 255, message = "Street must not exceed 255 characters")
    @Schema(
            description = "Street address",
            example = "123 Nguyễn Văn Linh",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String street;
    @Size(max = 100, message = "Ward must not exceed 100 characters")
    @NotBlank(message = "Ward is required")
    @Schema(
            description = "Ward",
            example = "Tân Phong",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String ward;
    @Size(max = 100, message = "Province or city must not exceed 100 characters")
    @NotBlank(message = "Province or city is required")
    @Schema(
            description = "Province or city",
            example = "Hồ Chí Minh",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String province;
}