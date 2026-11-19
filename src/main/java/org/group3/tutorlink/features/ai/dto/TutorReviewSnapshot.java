package org.group3.tutorlink.features.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class TutorReviewSnapshot {
    private String fullName;
    private List<CertificateSnapshot> certificates;
}