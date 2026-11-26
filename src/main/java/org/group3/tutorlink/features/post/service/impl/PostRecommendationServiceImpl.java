package org.group3.tutorlink.features.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.post.exception.PostErrorCode;
import org.group3.tutorlink.features.post.exception.PostValidationException;
import org.group3.tutorlink.features.post.mapper.PostMapper;
import org.group3.tutorlink.features.post.recommendation.PostRecommender;
import org.group3.tutorlink.features.post.recommendation.ScoredPost;
import org.group3.tutorlink.features.post.recommendation.dto.RecommendedPostResponse;
import org.group3.tutorlink.features.post.service.PostRecommendationService;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostRecommendationServiceImpl implements PostRecommendationService {

    private final AppUtil appUtil;
    private final UserRepository userRepository;
    private final List<PostRecommender> recommenders;
    private final PostMapper postMapper;
    private final MatchingProperties properties;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedPostResponse> recommend(Integer limit) {
        int size = resolveLimit(limit);

        User user = userRepository.findById(appUtil.userIdFromAuthentication())
                .orElseThrow(UserNotFoundException::new);

        PostRecommender recommender = recommenders.stream()
                .filter(candidate -> candidate.supports(user))
                .findFirst()
                .orElseThrow(() -> new PostValidationException(PostErrorCode.RECOMMENDATION_ROLE_NOT_SUPPORTED));

        return recommender.recommend(user, size).stream()
                .map(this::toResponse)
                .toList();
    }

    private int resolveLimit(Integer limit) {
        MatchingProperties.PostFeed feed = properties.getPostFeed();
        if (limit == null) {
            return feed.getDefaultLimit();
        }
        if (limit < 1) {
            throw new PostValidationException(PostErrorCode.INVALID_RECOMMENDATION_LIMIT);
        }
        return Math.min(limit, feed.getMaxLimit());
    }

    private RecommendedPostResponse toResponse(ScoredPost scored) {
        return RecommendedPostResponse.builder()
                .post(postMapper.toPostResponse(scored.getPost()))
                .score(Math.round(scored.getScore() * 10) / 10.0)
                .breakdown(scored.getBreakdown())
                .warnings(scored.getWarnings())
                .build();
    }
}