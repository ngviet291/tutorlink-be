package org.group3.tutorlink.common.entity;

import jakarta.persistence.Embeddable;
import lombok.*;



@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Embeddable
public class Address {
    private String street;
    private String ward;
    private String province;
}
