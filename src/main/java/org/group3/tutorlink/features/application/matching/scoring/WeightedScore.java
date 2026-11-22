package org.group3.tutorlink.features.application.matching.scoring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class WeightedScore {
    private final double total;
    private final Map<String, Double> breakdown;
}