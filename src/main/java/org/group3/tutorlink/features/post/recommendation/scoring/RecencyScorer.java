package org.group3.tutorlink.features.post.recommendation.scoring;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Điểm độ mới (0..1): giảm một nửa sau mỗi chu kỳ bán rã cấu hình. */
@Component
@RequiredArgsConstructor
public class RecencyScorer {

    private static final double UNKNOWN_SCORE = 0.5;
    private static final double HALF = 0.5;

    private final MatchingProperties properties;

    public double score(Post post, Instant now) {
        Instant created = post.getCreatedAt();
        if (created == null) {
            return UNKNOWN_SCORE;
        }
        double halfLifeDays = properties.getPostFeed().getRecencyHalfLifeDays();
        double ageDays = Math.max(0, Duration.between(created, now).toHours() / 24.0);
        return Math.pow(HALF, ageDays / halfLifeDays);
    }
}