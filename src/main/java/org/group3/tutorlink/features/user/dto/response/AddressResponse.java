package org.group3.tutorlink.features.user.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {
    private String street;
    private String ward;
    private String province;
}