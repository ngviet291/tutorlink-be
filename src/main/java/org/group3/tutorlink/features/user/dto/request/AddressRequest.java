package org.group3.tutorlink.features.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AddressRequest {
    @NotBlank(message = "Street not be blank")
    private String street;
    private String ward;
    @NotBlank(message = "Province not be blank")
    private String province;
}