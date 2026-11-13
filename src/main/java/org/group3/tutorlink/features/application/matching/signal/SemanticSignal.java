package org.group3.tutorlink.features.application.matching.signal;

import org.group3.tutorlink.features.application.entity.Application;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;

@Component
public class SemanticSignal implements MatchSignal {
    @Override
    public String key() {
        return "semantic";
    }

    @Override
    public OptionalDouble score(MatchContext context, Application application) {
        return context.semanticScoreOf(application.getTutor().getId());
    }
}