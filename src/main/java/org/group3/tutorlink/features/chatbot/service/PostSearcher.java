package org.group3.tutorlink.features.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.chatbot.ai.PostSearchFilters;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.chatbot.dto.response.PostSummaryResponse;
import org.group3.tutorlink.features.chatbot.specification.PostSearchSpecification;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class PostSearcher {

    private static final String SORT_FIELD = "createdAt";

    private final PostRepository postRepository;
    private final ChatbotProperties properties;     // candidatePoolSize: lấy dư rồi mới xếp theo khu vực

    @Transactional(readOnly = true)
    public List<PostSummaryResponse> search(PostSearchFilters filters, int limit) {
        // Người dùng không nói rõ loại bài thì dùng loại mặc định trong cấu hình
        PostType type = filters.getPostType() != null ? filters.getPostType() : properties.getDefaultPostType();

        Specification<Post> spec = Specification.allOf(
                PostSearchSpecification.fetchSubjectAndAuthor(),
                PostSearchSpecification.isOpen(Instant.now()),
                PostSearchSpecification.hasType(type),
                PostSearchSpecification.subjectContains(filters.getSubjectName()),
                PostSearchSpecification.provinceContains(filters.getProvince()),
                PostSearchSpecification.hasEducationLevel(filters.getEducationLevel()),
                PostSearchSpecification.authorTutorMatches(
                        filters.getMinExperienceYears(),
                        filters.getMinRate(),
                        filters.getMaxRate(),
                        filters.getMinRating())
        );

        List<Post> candidates = postRepository
                .findAll(spec, PageRequest.of(0, properties.getCandidatePoolSize(), Sort.by(Sort.Direction.DESC, SORT_FIELD)))
                .getContent();

        return candidates.stream()
                .sorted(Comparator.comparing((Post p) -> !isInArea(p, filters.getArea())))  // đúng khu vực lên trước, giữ thứ tự mới nhất
                .limit(limit)
                .map(this::toSummary)
                .toList();
    }

    private boolean isInArea(Post post, String area) {
        if (!StringUtils.hasText(area)) {
            return false;
        }
        Address address = post.getAddress();
        String haystack = Stream.of(
                        address == null ? null : address.getStreet(),
                        address == null ? null : address.getWard(),
                        post.getTitle(),
                        post.getContent())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return removeAccents(haystack).contains(removeAccents(area));
    }

    /** "Gò Vấp" == "go vap". */
    private String removeAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
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