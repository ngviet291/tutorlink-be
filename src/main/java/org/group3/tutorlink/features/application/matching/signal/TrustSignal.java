package org.group3.tutorlink.features.application.matching.signal;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.entity.Application;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;

@Component
@RequiredArgsConstructor
public class TrustSignal implements MatchSignal {
    private final MatchingProperties properties;

    @Override
    public String key() {
        return "trust";
    }

    @Override
    public OptionalDouble score(MatchContext context, Application application) {
        long accepted = context.acceptedCountOf(application.getTutor().getId());
        return OptionalDouble.of(Math.min((double) accepted / properties.getTrust().getAcceptedCap(), 1.0));
    }
}