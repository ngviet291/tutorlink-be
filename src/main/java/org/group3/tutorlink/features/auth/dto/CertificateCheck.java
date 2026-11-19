package org.group3.tutorlink.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateCheck {
    private boolean valid;
    private double confidence;
    private String reason;
    private String name;
    private String issuedBy;
    private String issuedDate;
    private String expiryDate;
}