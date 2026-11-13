package org.group3.tutorlink.features.application.matching.signal;

import lombok.Builder;
import lombok.Getter;
import org.group3.tutorlink.features.post.entity.Post;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

@Getter
@Builder
public class MatchContext {
    private final Post post;
    private final Map<UUID, Long> reviewCounts;
    private final Map<UUID, Long> acceptedCounts;
    private final Map<UUID, Double> semanticScores;

    public long reviewCountOf(UUID tutorId) {
        return reviewCounts.getOrDefault(tutorId, 0L);
    }

    public long acceptedCountOf(UUID tutorId) {
        return acceptedCounts.getOrDefault(tutorId, 0L);
    }

    public OptionalDouble semanticScoreOf(UUID tutorId) {
        Double score = semanticScores.get(tutorId);
        return score == null ? OptionalDouble.empty() : OptionalDouble.of(score);
    }
}