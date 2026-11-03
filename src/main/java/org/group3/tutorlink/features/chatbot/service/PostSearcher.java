package org.group3.tutorlink.features.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.chatbot.ai.PostSearchFilters;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.chatbot.dto.request.UserLocation;
import org.group3.tutorlink.features.chatbot.dto.response.PostSummaryResponse;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.post.specification.PostSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class PostSearcher {

    private static final String SORT_FIELD = "createdAt";

    private final PostRepository postRepository;
    private final PostProximityRanker proximityRanker;
    private final ChatbotProperties properties;     // candidatePoolSize: lấy dư rồi mới xếp theo khu vực

    @Transactional(readOnly = true)
    public List<PostSummaryResponse> search(PostSearchFilters filters, UserLocation userLocation, int limit) {
        // Người dùng không nói rõ loại bài thì dùng loại mặc định trong cấu hình
        PostType type = filters.getPostType() != null ? filters.getPostType() : properties.getDefaultPostType();

        Specification<Post> spec = Specification.allOf(
                PostSpecification.fetchSubject(),
                PostSpecification.fetchAuthor(),
                PostSpecification.hasStatus(PostStatus.PUBLISHED),
                PostSpecification.notExpired(Instant.now()),
                PostSpecification.hasType(type),
                PostSpecification.subjectContains(filters.getSubjectName()),
                PostSpecification.provinceContains(filters.getProvince()),
                PostSpecification.hasEducationLevel(filters.getEducationLevel()),
                PostSpecification.authorTutorMatches(
                        filters.getMinExperienceYears(),
                        filters.getMinRate(),
                        filters.getMaxRate(),
                        filters.getMinRating())
        );

        List<Post> candidates = postRepository
                .findAll(spec, PageRequest.of(0, properties.getCandidatePoolSize(), Sort.by(Sort.Direction.DESC, SORT_FIELD)))
                .getContent();

        return candidates.stream()
                .sorted(proximityRanker.nearestFirst(filters.getArea(), userLocation))
                .limit(limit)
                .map(this::toSummary)
                .toList();
    }

    // Chuyển Post -> PostSummaryResponse (chỉ lấy những thông tin cần thiết)
    private PostSummaryResponse toSummary(Post post) {
        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .type(post.getType())
                .subjectName(post.getSubject().getName())
                .educationLevel(post.getEducationLevel())
                .teachingMode(post.getTeachingMode())
                .location(formatAddress(post.getAddress()))
                .minBudget(post.getMinBudget())
                .maxBudget(post.getMaxBudget())
                .budgetUnit(post.getBudgetUnit())
                .deadline(post.getDeadline())
                .authorId(post.getAuthor().getId())
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