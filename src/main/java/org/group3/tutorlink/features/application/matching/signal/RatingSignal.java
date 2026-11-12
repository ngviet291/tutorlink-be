package org.group3.tutorlink.features.application.matching.signal;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;

@Component
@RequiredArgsConstructor
public class RatingSignal implements MatchSignal {
    private final MatchingProperties properties;

    @Override
    public String key() {
        return "rating";
    }

    @Override
    public OptionalDouble score(MatchContext context, Application application) {
        Tutor tutor = application.getTutor();
        long reviews = context.reviewCountOf(tutor.getId());
        int minVotes = properties.getMinVotes();

        double bayesian = (reviews * tutor.getAverageRating() + minVotes * properties.getGlobalAvgRating())
                / (reviews + minVotes);
        return OptionalDouble.of(bayesian / properties.getMaxRating());
    }
}