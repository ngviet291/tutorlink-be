package org.group3.tutorlink.features.application.matching.scoring;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.matching.signal.MatchContext;
import org.group3.tutorlink.features.application.matching.signal.MatchSignal;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@Component
@RequiredArgsConstructor
public class WeightedScorer {
    private final List<MatchSignal> signals;
    private final MatchingProperties properties;

    public WeightedScore score(MatchContext context, Application application) {
        Map<String, Double> breakdown = new LinkedHashMap<>();
        double weightedSum = 0;
        double weightTotal = 0;

        for (MatchSignal signal : signals) {
            OptionalDouble value = signal.score(context, application);
            if (value.isEmpty()) continue;

            double weight = properties.weightOf(signal.key());
            breakdown.put(signal.key(), value.getAsDouble());
            weightedSum += weight * value.getAsDouble();
            weightTotal += weight;
        }
        double total = weightTotal == 0 ? 0 : weightedSum / weightTotal;
        return new WeightedScore(total, breakdown);
    }
}