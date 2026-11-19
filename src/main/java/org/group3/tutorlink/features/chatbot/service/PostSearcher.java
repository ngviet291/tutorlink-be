package org.group3.tutorlink.features.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.common.service.AddressService;
import org.group3.tutorlink.common.geo.GeoPoint;
import org.group3.tutorlink.common.geo.GeoUtils;
import org.group3.tutorlink.common.service.LocationNormalizationService;
import org.group3.tutorlink.common.service.WardLocationService;
import org.group3.tutorlink.features.chatbot.ai.PostSearchFilters;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.chatbot.config.Scoring;
import org.group3.tutorlink.features.chatbot.dto.request.UserLocation;
import org.group3.tutorlink.features.chatbot.dto.response.PostSearchResult;
import org.group3.tutorlink.features.chatbot.dto.response.PostSummaryResponse;
import org.group3.tutorlink.features.chatbot.ranking.PostScorer;
import org.group3.tutorlink.features.chatbot.ranking.SearchAliasService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
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
import java.util.Comparator;
import java.util.HashMap;
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
    private final WardLocationService wardLocations;
    private final PostScorer scorer;
    private final SearchAliasService aliases;


    /**
     * Tìm bài bằng chấm điểm xếp hạng thay vì so khớp chuỗi cứng:
     * - Môn học: độ giống tên (bỏ dấu, chịu gõ sai nhẹ), loại bài lệch môn quá xa.
     * - Vị trí: khoảng cách Haversine giữa tâm phường/xã của bài đăng và điểm tham chiếu
     *   (phường người dùng nói tới, không nói thì phường của chính họ). Gần hơn thì điểm cao hơn.
     * - Cấp học và độ mới cũng cộng điểm; xem {@link PostScorer}.
     *
     * Người dùng nói rõ phường/xã:
     * 1. Chỉ lấy bài trong bán kính; không có thì mở rộng bán kính (relaxed = true).
     * 2. Vẫn không có thì trả bài trong cùng tỉnh (relaxed = true).
     * 3. Không biết tỉnh -> không đoán bừa, trả rỗng.
     */
    @Transactional(readOnly = true)
    public PostSearchResult search(PostSearchFilters filters, UserLocation userLocation, int limit) {
        Scoring scoring = properties.getScoring();
        PostType type = filters.getPostType() != null
                ? filters.getPostType()
                : properties.getDefaultPostType();
        String area = filters.getArea();
        boolean explicitArea = StringUtils.hasText(area);
        String province = resolveProvince(filters, userLocation);

        List<Post> candidates = find(filters, type, province);
        if (StringUtils.hasText(filters.getSubjectName())) {
            candidates = candidates.stream()
                    .filter(post -> scorer.subjectSimilarity(post, filters) >= scoring.getMinSubjectSimilarity())
                    .toList();
        }

        GeoPoint reference = referencePoint(explicitArea ? province : userProvince(userLocation),
                explicitArea ? area : wardOf(userLocation));
        Map<UUID, Double> distances = distances(candidates, reference, explicitArea ? area : null);

        List<Post> pool = candidates;
        double radius = scoring.getRadiusKm();
        boolean relaxed = false;
        if (explicitArea) {
            if (province == null) {
                pool = List.of();
            } else {
                pool = within(candidates, distances, radius);
                if (pool.isEmpty()) {
                    radius = scoring.getExpandedRadiusKm();
                    pool = within(candidates, distances, radius);
                    relaxed = !pool.isEmpty();
                }
                if (pool.isEmpty()) {
                    radius = scoring.getExpandedRadiusKm();
                    pool = candidates;
                    relaxed = !candidates.isEmpty();
                }
            }
        }

        Instant now = Instant.now();
        Map<UUID, Double> scores = new HashMap<>();
        for (Post post : pool) {
            scores.put(post.getId(), scorer.score(post, filters, distances.get(post.getId()), radius, now));
        }
        List<Post> ranked = pool.stream()
                .sorted(Comparator
                        .comparingDouble((Post post) -> -scores.get(post.getId()))
                        .thenComparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        log.info("Chatbot search: subject={}, province={}, area={}, type={} -> {} ứng viên, {} bài sau lọc bán kính, "
                        + "tham chiếu={}, bán kính={} km, relaxed={}",
                filters.getSubjectName(), province, area, type, candidates.size(), ranked.size(),
                reference != null, radius, relaxed);

        List<PostSummaryResponse> summaries = ranked.stream()
                .limit(limit)
                .map(post -> toSummary(post, distances.get(post.getId())))
                .toList();
        return new PostSearchResult(summaries, relaxed);
    }

    // Điểm tham chiếu: tâm phường/xã; chỉ biết tỉnh thì không dùng (tâm tỉnh quá thô để tính khoảng cách).
    private GeoPoint referencePoint(String province, String ward) {
        if (!StringUtils.hasText(province) || !StringUtils.hasText(ward)) {
            return null;
        }
        return wardLocations.findWard(province, ward).orElse(null);
    }

    private Map<UUID, Double> distances(List<Post> posts, GeoPoint reference, String textFallbackWard) {
        Map<UUID, Double> result = new HashMap<>();
        for (Post post : posts) {
            Double distance = distanceTo(post, reference, textFallbackWard);
            if (distance != null) {
                result.put(post.getId(), distance);
            } else if (reference != null) {
                // Bài không xác định được vị trí: vẫn tính tiêu chí khoảng cách nhưng 0 điểm
                result.put(post.getId(), Double.POSITIVE_INFINITY);
            }
        }
        return result;
    }

    private Double distanceTo(Post post, GeoPoint reference, String textFallbackWard) {
        Address address = post.getAddress();
        boolean hasReference = reference != null || StringUtils.hasText(textFallbackWard);
        // Dạy online hoặc không có địa chỉ: học ở đâu cũng được nên không phạt, không loại
        if (hasReference && (post.getTeachingMode() == TeachingMode.ONLINE || address == null)) {
            return Double.NaN;
        }
        if (reference != null) {
            return wardLocations.findWard(address.getProvince(), address.getWard())
                    .map(point -> GeoUtils.distanceKm(reference, point))
                    .orElse(null);
        }
        // Phường/xã người dùng nói không có trong dữ liệu tọa độ: quay về so tên (cùng phường = 0 km)
        if (StringUtils.hasText(textFallbackWard) && address != null) {
            return locationNormalizer.isSameWard(address.getWard(), textFallbackWard) ? 0.0 : Double.POSITIVE_INFINITY;
        }
        return null;
    }

    private List<Post> within(List<Post> posts, Map<UUID, Double> distances, double radiusKm) {
        return posts.stream()
                .filter(post -> {
                    Double d = distances.get(post.getId());
                    return d != null && (d.isNaN() || d <= radiusKm);
                })
                .toList();
    }

    private String userProvince(UserLocation userLocation) {
        return userLocation != null && userLocation.isKnown() ? userLocation.getProvince() : null;
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
                .collect(Collectors.toMap(Post::getId, post -> toSummary(post, null)));
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

        // "Hanoi", "Saigon", "Ho Chi Minh City"... -> tên tỉnh quen thuộc trước khi chuẩn hóa
        province = aliases.province(province);
        return StringUtils.hasText(province) ? addressService.normalize(province) : null;
    }

    // Bài trong tỉnh được hỏi, cộng bài dạy online / chưa có địa chỉ (học ở đâu cũng được)
    private Specification<Post> inProvinceOrLocationFree(String province) {
        if (!StringUtils.hasText(province)) {
            return null;
        }
        Specification<Post> locationFree = (root, query, cb) -> cb.or(
                cb.equal(root.get("teachingMode"), TeachingMode.ONLINE),
                cb.isNull(root.get("address").get("province")));
        return PostSpecification.provinceContains(province).or(locationFree);
    }

    private List<Post> find(PostSearchFilters f, PostType type, String province) {
        Specification<Post> spec = Specification.allOf(
                PostSpecification.fetchSubject(),
                PostSpecification.fetchAuthor(),
                PostSpecification.hasStatus(PostStatus.PUBLISHED),
                PostSpecification.notExpired(Instant.now()),
                PostSpecification.hasType(type),
                inProvinceOrLocationFree(province),
                PostSpecification.authorTutorMatches(
                        f.getMinExperienceYears(),
                        f.getMinRate(),
                        f.getMaxRate(),
                        f.getMinRating()
                )
        );

        PageRequest page = PageRequest.of(
                0,
                Math.max(properties.getCandidatePoolSize(), properties.getScoring().getMinCandidatePool()),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return postRepository.findAll(spec, page).getContent();
    }

    private String wardOf(UserLocation userLocation) {
        return userLocation != null && userLocation.isKnown() ? userLocation.getWard() : null;
    }

    private PostSummaryResponse toSummary(Post post, Double distance) {
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
                .distanceKm(toKm(distance))
                .build();
    }

    // NaN (dạy online / chưa có địa chỉ) và vô cực (không xác định được vị trí) thì không có khoảng cách.
    private Double toKm(Double distance) {
        if (distance == null || distance.isNaN() || distance.isInfinite()) {
            return null;
        }
        return Math.round(distance * 10) / 10.0;
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
