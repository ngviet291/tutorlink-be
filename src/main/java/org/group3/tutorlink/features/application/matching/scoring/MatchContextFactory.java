package org.group3.tutorlink.features.application.matching.scoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.dto.projection.ReviewCountRow;
import org.group3.tutorlink.features.application.dto.projection.TutorAcceptedRow;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.matching.semantic.SemanticMatcher;
import org.group3.tutorlink.features.application.matching.signal.MatchContext;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.review.repository.ReviewRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchContextFactory {
    private final ReviewRepository reviewRepository;
    private final ApplicationRepository applicationRepository;
    private final SemanticMatcher semanticMatcher;

    public MatchContext create(Post post, List<Application> applications) {
        List<UUID> tutorIds = applications.stream()
                .map(a -> a.getTutor().getId())
                .distinct()
                .toList();

        return MatchContext.builder()
                .post(post)
                .reviewCounts(countReviews(tutorIds))
                .acceptedCounts(countAccepted(tutorIds))
                .semanticScores(scoreSemantically(post, tutorIds))
                .build();
    }

    private Map<UUID, Long> countReviews(List<UUID> tutorIds) {
        return reviewRepository.countByTutorIds(tutorIds).stream()
                .collect(Collectors.toMap(ReviewCountRow::getTutorId, ReviewCountRow::getCnt, Long::sum));
    }

    private Map<UUID, Long> countAccepted(List<UUID> tutorIds) {
        return applicationRepository.countByTutorIdsAndStatus(tutorIds, ApplicationStatus.ACCEPTED).stream()
                .collect(Collectors.toMap(TutorAcceptedRow::getTutorId, TutorAcceptedRow::getCnt, Long::sum));
    }

    private Map<UUID, Double> scoreSemantically(Post post, List<UUID> tutorIds) {
        try {
            return semanticMatcher.score(post, tutorIds);
        } catch (RuntimeException e) {
            log.warn("Semantic matching unavailable, fallback to rule-based ranking: {}", e.getMessage());
            return Map.of();
        }
    }
}