package org.group3.tutorlink.features.post.recommendation.semantic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStorePostSemanticMatcher implements PostSemanticMatcher {
    private final VectorStore vectorStore;
    private final MatchingProperties properties;

    @Override
    public Map<UUID, Double> score(String profileText, Collection<UUID> candidatePostIds) {
        if (!StringUtils.hasText(profileText) || candidatePostIds.isEmpty()) {
            return Map.of();
        }
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(profileText)
                    .topK(candidatePostIds.size())
                    .filterExpression(onlyCandidates(candidatePostIds))
                    .build();

            return vectorStore.similaritySearch(request).stream()
                    .collect(Collectors.toMap(
                            document -> UUID.fromString(document.getId()),
                            document -> calibrate(Objects.requireNonNullElse(document.getScore(), 0.0)),
                            Math::max));
        } catch (RuntimeException e) {
            // Vector store lỗi thì bỏ tiêu chí ngữ nghĩa, vẫn xếp hạng được bằng các tiêu chí còn lại
            log.warn("Semantic matching unavailable, fallback to rule-based ranking: {}", e.getMessage());
            return Map.of();
        }
    }

    private Filter.Expression onlyCandidates(Collection<UUID> candidatePostIds) {
        Object[] ids = candidatePostIds.stream().map(UUID::toString).toArray();
        return new FilterExpressionBuilder().in(PostDocumentFields.POST_ID, ids).build();
    }

    /** Co giãn cosine thô về [0, 1] theo khoảng floor/ceiling trong cấu hình. */
    private double calibrate(double cosine) {
        MatchingProperties.Semantic cfg = properties.getSemantic();
        double scaled = (cosine - cfg.getFloor()) / (cfg.getCeiling() - cfg.getFloor());
        return Math.clamp(scaled, 0.0, 1.0);
    }
}