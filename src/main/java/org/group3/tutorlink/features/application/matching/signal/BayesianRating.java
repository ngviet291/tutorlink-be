package org.group3.tutorlink.features.application.matching.signal;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.springframework.stereotype.Component;

/** Rating trung bình Bayesian: gia sư ít review được kéo về mức trung bình hệ thống. */
@Component
@RequiredArgsConstructor
public class BayesianRating {

    private final MatchingProperties properties;

    /** Điểm rating đã chuẩn hóa về [0, 1]. */
    public double normalized(double averageRating, long reviewCount) {
        int minVotes = properties.getMinVotes();
        double bayesian = (reviewCount * averageRating + minVotes * properties.getGlobalAvgRating())
                / (reviewCount + minVotes);
        return bayesian / properties.getMaxRating();
    }
}