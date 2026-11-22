package org.group3.tutorlink.features.application.matching.signal;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.entity.Application;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;

@Component
@RequiredArgsConstructor
public class ExperienceSignal implements MatchSignal {
    private final MatchingProperties properties;

    @Override
    public String key() {
        return "experience";
    }

    @Override
    public OptionalDouble score(MatchContext context, Application application) {
        int years = Math.max(application.getTutor().getExperienceYears(), 0);
        return OptionalDouble.of(Math.min(years / properties.getExperienceCapYears(), 1.0));
    }
}