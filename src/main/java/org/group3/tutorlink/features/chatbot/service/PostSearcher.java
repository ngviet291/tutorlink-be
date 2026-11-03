
package org.group3.tutorlink.features.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.common.service.impl.ProvinceNormalizationService;
import org.group3.tutorlink.features.chatbot.ai.PostSearchFilters;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.chatbot.dto.request.UserLocation;
import org.group3.tutorlink.features.chatbot.dto.response.PostSearchResult;
import org.group3.tutorlink.features.chatbot.dto.response.PostSummaryResponse;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.post.specification.PostSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostSearcher {

    private final PostRepository postRepository;
    private final PostProximityRanker proximityRanker;
    private final ChatbotProperties properties;
    private final ProvinceNormalizationService provinceNormalizationService;

    @Transactional(readOnly = true)
    public PostSearchResult search(PostSearchFilters filters, UserLocation userLocation, int limit) {
        PostType type = filters.getPostType() != null
                ? filters.getPostType()
                : properties.getDefaultPostType();

        // Bộ lọc chỉ theo môn học, dùng khi fallback
        PostSearchFilters subjectOnly = PostSearchFilters.builder()
                .subjectName(filters.getSubjectName())
                .build();
        List<Post> strictPosts = find(filters, type);

        log.info(
                "[CHATBOT DEBUG] Strict search: subject={}, province={}, type={}, education={}, resultCount={}",
                filters.getSubjectName(),
                filters.getProvince(),
                type,
                filters.getEducationLevel(),
                strictPosts.size()
        );

        boolean relaxed = false;
        List<Post> posts = strictPosts;

        if (strictPosts.isEmpty() && hasExtraFilters(filters)) {
            posts = find(subjectOnly, type);
            relaxed = !posts.isEmpty();

            log.info("[CHATBOT DEBUG] Fallback cùng loại bài: resultCount={}", posts.size());
        }

        if (posts.isEmpty()) {
            posts = find(subjectOnly, null);
            relaxed = !posts.isEmpty();

            log.info("[CHATBOT DEBUG] Fallback cùng môn học: resultCount={}", posts.size());
        }

        log.info(
                "[CHATBOT DEBUG] FINAL: strictCount={}, finalCount={}, relaxed={}",
                strictPosts.size(),
                posts.size(),
                relaxed
        );

        List<PostSummaryResponse> summaries = proximityRanker
                .sortNearestFirst(posts, filters.getArea(), userLocation)
                .stream()
                .limit(limit)
                .map(this::toSummary)
                .toList();

        return new PostSearchResult(summaries, relaxed);
    }

    // Lấy tóm tắt các bài theo ID bằng một query.
    // Bài đã đóng, hết hạn hoặc bị gỡ sẽ không có trong kết quả.
    @Transactional(readOnly = true)
    public Map<UUID, PostSummaryResponse> findByIds(Collection<UUID> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Specification<Post> spec = Specification.allOf(
                PostSpecification.fetchSubject(),
                PostSpecification.fetchAuthor(),
                PostSpecification.hasStatus(PostStatus.PUBLISHED),
                PostSpecification.notExpired(Instant.now()),
                idIn(postIds)
        );

        return postRepository.findAll(spec).stream()
                .collect(Collectors.toMap(Post::getId, this::toSummary));
    }

    private Specification<Post> idIn(Collection<UUID> postIds) {
        return (root, query, cb) -> root.get("id").in(postIds);
    }

    private boolean hasExtraFilters(PostSearchFilters f) {
        return StringUtils.hasText(f.getProvince())
                || f.getEducationLevel() != null
                || f.getMinExperienceYears() != null
                || f.getMinRate() != null
                || f.getMaxRate() != null
                || f.getMinRating() != null;
    }

    private List<Post> find(PostSearchFilters f, PostType type) {

        // Chuẩn hóa tên tỉnh/thành phố trước khi tạo điều kiện SQL.
        String normalizedProvince =
                provinceNormalizationService.normalize(f.getProvince());

        log.info(
                "[CHATBOT DEBUG] Province gốc={}, province chuẩn hóa={}",
                f.getProvince(),
                normalizedProvince
        );

        Specification<Post> spec = Specification.allOf(
                PostSpecification.fetchSubject(),
                PostSpecification.fetchAuthor(),
                PostSpecification.hasStatus(PostStatus.PUBLISHED),
                PostSpecification.notExpired(Instant.now()),
                PostSpecification.hasType(type),
                PostSpecification.subjectContains(f.getSubjectName()),
                PostSpecification.provinceContains(normalizedProvince),
                PostSpecification.hasEducationLevel(f.getEducationLevel()),
                PostSpecification.authorTutorMatches(
                        f.getMinExperienceYears(),
                        f.getMinRate(),
                        f.getMaxRate(),
                        f.getMinRating()
                )
        );

        PageRequest page = PageRequest.of(
                0,
                properties.getCandidatePoolSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<Post> posts = postRepository.findAll(spec, page).getContent();

        log.debug(
                "Chatbot search (subject={}, province={}, type={}) -> {} bài",
                f.getSubjectName(),
                normalizedProvince,
                type,
                posts.size()
        );

        return posts;
    }

    private PostSummaryResponse toSummary(Post post) {
        return PostSummaryResponse.builder()
                .id(post.getId())
                .subjectName(post.getSubject().getName())
                .title(post.getTitle())
                .location(formatAddress(post.getAddress()))
                .minBudget(post.getMinBudget())
                .maxBudget(post.getMaxBudget())
                .budgetUnit(post.getBudgetUnit())
                .authorName(post.getAuthor().getFullName())
                .authorAvatarUrl(post.getAuthor().getAvatarUrl())
                .build();
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return null;
        }

        return Stream.of(
                        address.getStreet(),
                        address.getWard(),
                        address.getProvince()
                )
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
    }
}
