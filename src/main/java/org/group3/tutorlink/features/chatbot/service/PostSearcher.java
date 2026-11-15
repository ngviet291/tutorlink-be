package org.group3.tutorlink.features.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.common.service.AddressService;
import org.group3.tutorlink.common.service.LocationNormalizationService;
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
    private final ChatbotProperties properties;
    private final AddressService addressService;
    private final LocationNormalizationService locationNormalizer;

    /**
     * Thứ tự ưu tiên:
     * 1. Có bài đúng phường/xã người dùng yêu cầu -> chỉ trả các bài đó.
     * 2. Không có -> trả bài ở các phường/xã khác trong cùng tỉnh (relaxed = true).
     * 3. Không biết tỉnh -> không đoán bừa, trả rỗng.
     *
     * Người dùng không nói phường/xã thì lấy bài trong tỉnh, bài cùng phường/xã với vị trí của họ lên đầu.
     */
    @Transactional(readOnly = true)
    public PostSearchResult search(PostSearchFilters filters, UserLocation userLocation, int limit) {
        PostType type = filters.getPostType() != null
                ? filters.getPostType()
                : properties.getDefaultPostType();
        String area = filters.getArea();
        String province = resolveProvince(filters, userLocation);
        List<Post> candidates = find(filters, type, province);
        List<Post> posts;
        boolean relaxed = false;
        if (StringUtils.hasText(area)) {
            List<Post> inWard = candidates.stream()
                    .filter(post -> isInWard(post, area))
                    .toList();

            if (!inWard.isEmpty()) {
                posts = inWard;
            } else if (province != null) {
                posts = candidates;
                relaxed = !candidates.isEmpty();
            } else {
                posts = List.of();
            }
        } else {
            posts = sameWardFirst(candidates, wardOf(userLocation));
        }

        log.info("Chatbot search: subject={}, province={}, area={}, type={} -> {} bài trong tỉnh, {} bài trả về, relaxed={}",
                filters.getSubjectName(), province, area, type, candidates.size(), posts.size(), relaxed);

        List<PostSummaryResponse> summaries = posts.stream()
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
                idIn(postIds)
        );

        return postRepository.findAll(spec).stream()
                .collect(Collectors.toMap(Post::getId, this::toSummary));
    }

    private Specification<Post> idIn(Collection<UUID> postIds) {
        return (root, query, cb) -> root.get("id").in(postIds);
    }

    // Người dùng nói phường/xã mà không nói tỉnh thì hiểu là tỉnh họ đang ở.
    private String resolveProvince(PostSearchFilters filters, UserLocation userLocation) {
        String province = filters.getProvince();

        boolean canUseUserProvince = !StringUtils.hasText(province)
                && StringUtils.hasText(filters.getArea())
                && userLocation != null
                && userLocation.isKnown();
        if (canUseUserProvince) {
            province = userLocation.getProvince();
        }

        return StringUtils.hasText(province) ? addressService.normalize(province) : null;
    }

    private List<Post> find(PostSearchFilters f, PostType type, String province) {
        Specification<Post> spec = Specification.allOf(
                PostSpecification.fetchSubject(),
                PostSpecification.fetchAuthor(),
                PostSpecification.hasStatus(PostStatus.PUBLISHED),
                PostSpecification.notExpired(Instant.now()),
                PostSpecification.hasType(type),
                PostSpecification.subjectContains(f.getSubjectName()),
                PostSpecification.provinceContains(province),
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
        return postRepository.findAll(spec, page).getContent();
    }

    private boolean isInWard(Post post, String ward) {
        return post.getAddress() != null && locationNormalizer.isSameWard(post.getAddress().getWard(), ward);
    }

    private String wardOf(UserLocation userLocation) {
        return userLocation != null && userLocation.isKnown() ? userLocation.getWard() : null;
    }

    // Bài cùng phường/xã lên đầu, các bài còn lại giữ nguyên thứ tự (mới nhất trước).
    private List<Post> sameWardFirst(List<Post> posts, String ward) {
        if (!StringUtils.hasText(ward)) {
            return posts;
        }
        Map<Boolean, List<Post>> split = posts.stream()
                .collect(Collectors.partitioningBy(post -> isInWard(post, ward)));
        return Stream.concat(split.get(true).stream(), split.get(false).stream()).toList();
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
        return Stream.of(address.getStreet(), address.getWard(), address.getProvince())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
    }
}