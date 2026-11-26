package org.group3.tutorlink.features.application.matching.signal;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;

@Component
@RequiredArgsConstructor
public class RatingSignal implements MatchSignal {
    private final BayesianRating bayesianRating;

    @Override
    public String key() {
        return "rating";
    }

    @Override
    public OptionalDouble score(MatchContext context, Application application) {
        Tutor tutor = application.getTutor();
        long reviews = context.reviewCountOf(tutor.getId());
        return OptionalDouble.of(bayesianRating.normalized(tutor.getAverageRating(), reviews));
    }
}