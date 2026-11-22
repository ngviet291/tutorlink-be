package org.group3.tutorlink.features.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MatchBreakdown {
    private double price;          // 0..1
    private double rating;         // 0..1
    private double experience;     // 0..1
    private int experienceYears;
    private Double semantic;
}