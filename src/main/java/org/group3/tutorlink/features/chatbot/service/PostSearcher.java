package org.group3.tutorlink.features.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.entity.Address;
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

    @Transactional(readOnly = true)
    public PostSearchResult search(PostSearchFilters filters, UserLocation userLocation, int limit) {
        PostType type = filters.getPostType() != null ? filters.getPostType() : properties.getDefaultPostType();
        PostSearchFilters subjectOnly = PostSearchFilters.builder().subjectName(filters.getSubjectName()).build();

        List<Post> posts = find(filters, type);
        boolean relaxed = posts.isEmpty();

        // Nếu người dùng chỉ nói môn học thì bước này sẽ trùng hệt câu query trên, bỏ qua cho đỡ tốn 1 lần query
        if (posts.isEmpty() && hasExtraFilters(filters)) {
            posts = find(subjectOnly, type);
        }
        if (posts.isEmpty()) {
            posts = find(subjectOnly, null);
        }

        List<PostSummaryResponse> summaries = proximityRanker
                .sortNearestFirst(posts, filters.getArea(), userLocation).stream()
                .limit(limit)
                .map(this::toSummary)
                .toList();
        return new PostSearchResult(summaries, relaxed);
    }

    /** Lấy tóm tắt các bài theo id bằng 1 query. Bài đã đóng, hết hạn hoặc bị gỡ sẽ không có trong kết quả. */
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
                idIn(postIds));

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
        Specification<Post> spec = Specification.allOf(
                PostSpecification.fetchSubject(),
                PostSpecification.fetchAuthor(),
                PostSpecification.hasStatus(PostStatus.PUBLISHED),
                PostSpecification.notExpired(Instant.now()),
                PostSpecification.hasType(type),
                PostSpecification.subjectContains(f.getSubjectName()),
                PostSpecification.provinceContains(f.getProvince()),
                PostSpecification.hasEducationLevel(f.getEducationLevel()),
                PostSpecification.authorTutorMatches(
                        f.getMinExperienceYears(), f.getMinRate(), f.getMaxRate(), f.getMinRating()));

        PageRequest page = PageRequest.of(0, properties.getCandidatePoolSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Post> posts = postRepository.findAll(spec, page).getContent();
        log.debug("Chatbot search (subject={}, province={}, type={}) -> {} bài",
                f.getSubjectName(), f.getProvince(), type, posts.size());
        return posts;
    }

    private PostSummaryResponse toSummary(Post post) {
        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .subjectName(post.getSubject().getName())
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
        return Stream.of(address.getStreet(), address.getWard(), address.getProvince())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
    }
}