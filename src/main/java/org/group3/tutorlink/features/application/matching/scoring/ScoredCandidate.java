package org.group3.tutorlink.features.application.matching.scoring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.MatchWarning;

import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ScoredCandidate {
    private final Application application;
    private final double score;
    private final Map<String, Double> breakdown;
    private final List<MatchWarning> warnings;
}