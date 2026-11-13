package org.group3.tutorlink.features.application.matching.semantic;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VectorStoreSemanticMatcher implements SemanticMatcher {
    private final VectorStore vectorStore;
    private final PostQueryTextBuilder queryTextBuilder;
    private final MatchingProperties properties;

    @Override
    public Map<UUID, Double> score(Post post, Collection<UUID> candidateTutorIds) {
        if (candidateTutorIds.isEmpty()) return Map.of();

        SearchRequest request = SearchRequest.builder()
                .query(queryTextBuilder.build(post))
                .topK(candidateTutorIds.size())
                .filterExpression(onlyCandidates(candidateTutorIds))
                .build();

        return vectorStore.similaritySearch(request).stream()
                .collect(Collectors.toMap(
                        document -> UUID.fromString(document.getId()),
                        document -> calibrate(Objects.requireNonNullElse(document.getScore(), 0.0)),
                        Math::max));
    }

    private Filter.Expression onlyCandidates(Collection<UUID> candidateTutorIds) {
        Object[] ids = candidateTutorIds.stream().map(UUID::toString).toArray();
        return new FilterExpressionBuilder().in(TutorDocumentFields.TUTOR_ID, ids).build();
    }

    /** Co giãn cosine thô về [0, 1] theo khoảng floor/ceiling trong cấu hình. */
    private double calibrate(double cosine) {
        MatchingProperties.Semantic cfg = properties.getSemantic();
        double scaled = (cosine - cfg.getFloor()) / (cfg.getCeiling() - cfg.getFloor());
        return Math.clamp(scaled, 0.0, 1.0);
    }
}