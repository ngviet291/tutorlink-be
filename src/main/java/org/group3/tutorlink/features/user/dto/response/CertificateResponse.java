package org.group3.tutorlink.features.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {
    private UUID id;
    private String name;
    private String issuedBy;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private String fileUrl;
    private boolean verified;
}