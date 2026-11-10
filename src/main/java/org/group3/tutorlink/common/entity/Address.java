package org.group3.tutorlink.common.entity;

import jakarta.persistence.Embeddable;
import lombok.*;



@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Embeddable
public class Address {

    private String street; // số nhà, tên đường hoặc ấp
    private String ward; // phường xã
    private String province; // tỉnh thành phố
}
