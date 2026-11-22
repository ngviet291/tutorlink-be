package org.group3.tutorlink.features.ai.dto;

import lombok.*;

import java.util.UUID;
@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class CertificateSnapshot {
    private  UUID id;
    private  String fileUrl;
    private  String cloudinaryPublicId;
}