package org.group3.tutorlink.features.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.ai.service.TutorMatchingAiService;
import org.group3.tutorlink.features.application.dto.projection.ReviewCountRow;
import org.group3.tutorlink.features.application.dto.response.MatchBreakdown;
import org.group3.tutorlink.features.application.dto.response.RankedApplicationsResponse;
import org.group3.tutorlink.features.application.dto.response.ScoredApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.enums.MatchWarning;
import org.group3.tutorlink.features.application.exception.ErrorCodeApplication;
import org.group3.tutorlink.features.application.mapper.ApplicationMapper;
import org.group3.tutorlink.features.application.matching.AreaMatcher;
import org.group3.tutorlink.features.application.matching.AvailabilityChecker;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.service.ApplicationRankingService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.BudgetUnit;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.review.repository.ReviewRepository;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationRankingServiceImpl implements ApplicationRankingService {
    private final AreaMatcher areaMatcher;
    private final AvailabilityChecker availabilityChecker;
    private static final Comparator<Scored> BY_SCORE_DESC_THEN_APPLIED_AT =
            Comparator.<Scored>comparingDouble(Scored::score).reversed()
                    .thenComparing(s -> s.application().getAppliedAt());
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);
    private final MatchingProperties matchingProperties;
    private final ApplicationRepository applicationRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final ApplicationMapper applicationMapper;
    private final TutorMatchingAiService tutorMatchingAiService;
    private final AppUtil appUtil;

    /** Kết quả chấm điểm nội bộ, dùng để sắp xếp trước khi map sang DTO. */
    private record Scored(Application application, double score,
                          MatchBreakdown breakdown, List<MatchWarning> warnings) {}

    // =====================================================================
    // Hàm chính
    // =====================================================================

    @Override
    @PreAuthorize("hasAuthority('STUDENT')")
    public RankedApplicationsResponse rankApplications(UUID postId) {
        Post post = loadPostOwnedByCurrentUser(postId);

        List<Application> applications = applicationRepository
                .findWithTutorByPostIdAndStatus(postId, ApplicationStatus.PENDING);

        if (applications.size() <= matchingProperties.getTopN()) {
            return buildUnranked(applications);
        }
        return buildRanked(post, applications);
    }

    private Post loadPostOwnedByCurrentUser(UUID postId) {
        UUID userId = appUtil.userIdFromAuthentication();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCodeApplication.POST_NOT_FOUND));


        // Chỉ bài FIND_TUTOR (học viên đăng, gia sư ứng tuyển) mới cần xếp hạng gia sư
        if (post.getType() != PostType.FIND_TUTOR) {
            log.error("User {} tried to rank applications for post {} of type {}, which is not FIND_TUTOR", userId, postId, post.getType());
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }
        // Chỉ tác giả bài đăng được xem
        if (!post.getAuthor().getId().equals(userId)) {
            throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
        }
        return post;
    }

    // ---------- Ít ứng viên: hiển thị hết theo thời gian ứng tuyển, không chấm điểm ----------
    private RankedApplicationsResponse buildUnranked(List<Application> applications) {
        List<ScoredApplicationResponse> all = applications.stream()
                .sorted(Comparator.comparing(Application::getAppliedAt))
                .map(a -> ScoredApplicationResponse.builder()
                        .application(applicationMapper.toApplicationResponse(a))
                        .build())
                .toList();

        return RankedApplicationsResponse.builder()
                .ranked(false)
                .top(all)
                .others(List.of())
                .build();
    }

    // ---------- Nhiều ứng viên: chấm điểm, lấy top N, phần còn lại vào others ----------
    private RankedApplicationsResponse buildRanked(Post post, List<Application> applications) {
        int topN = matchingProperties.getTopN();
        Map<UUID, Long> reviewCounts = loadReviewCounts(applications);
        Map<UUID, Double> semanticScores = scoreSemanticFitOrEmpty(post, applications);

        List<ScoredApplicationResponse> sorted = applications.stream()
                .map(a -> score(post, a,
                        reviewCounts.getOrDefault(a.getTutor().getId(), 0L),
                        semanticScores.get(a.getId())))
                .sorted(BY_SCORE_DESC_THEN_APPLIED_AT)
                .map(this::toScoredApplicationResponse)
                .toList();

        return RankedApplicationsResponse.builder()
                .ranked(true)
                .top(sorted.subList(0, topN))
                .others(sorted.subList(topN, sorted.size()))
                .build();
    }

    // AI lỗi thì vẫn xếp hạng bằng điểm luật, người dùng không thấy lỗi
    private Map<UUID, Double> scoreSemanticFitOrEmpty(Post post, List<Application> applications) {
        try {
            return tutorMatchingAiService.scoreSemanticFit(post, applications);
        } catch (AppException e) {
            log.warn("AI matching unavailable, fallback to rule-based ranking: {}", e.getMessage());
            return Map.of();
        }
    }

    // =====================================================================
    // Chấm điểm một ứng viên
    // =====================================================================

    private Scored score(Post post, Application application, long reviewCount, Double semanticFit) {
        Tutor tutor = application.getTutor();

        MatchBreakdown breakdown = MatchBreakdown.builder()
                .price(priceFit(effectiveFee(application, tutor), post.getMinBudget(), post.getMaxBudget()))
                .rating(ratingScore(tutor.getAverageRating(), reviewCount))
                .experience(experienceScore(tutor.getExperienceYears()))
                .experienceYears(tutor.getExperienceYears())
                .semantic(semanticFit)
                .build();

        List<MatchWarning> warnings = findWarnings(tutor, post);
        double total = weightedScore(breakdown) * penaltyFactor(warnings);

        return new Scored(application, total, breakdown, warnings);
    }

    /** Có điểm AI thì dùng bộ trọng số có AI, không thì dùng bộ trọng số luật. */
    private double weightedScore(MatchBreakdown b) {
        MatchingProperties.Weights w = b.getSemantic() == null
                ? matchingProperties.getWeights()
                : matchingProperties.getAiWeights();

        return w.getPrice() * b.getPrice()
                + w.getRating() * b.getRating()
                + w.getExperience() * b.getExperience()
                + w.getSemantic() * Objects.requireNonNullElse(b.getSemantic(), 0.0);
    }

    // Không loại hẳn: chỉ giảm điểm để học viên vẫn lướt xem được
    private double penaltyFactor(List<MatchWarning> warnings) {
        MatchingProperties.Penalty penalty = matchingProperties.getPenalty();
        return warnings.stream()
                .mapToDouble(warning -> switch (warning) {
                    case SUBJECT_MISMATCH -> penalty.getSubject();
                    case LEVEL_MISMATCH -> penalty.getLevel();
                    case MODE_MISMATCH -> penalty.getMode();
                    case AREA_MISMATCH -> penalty.getArea();
                    case AVAILABILITY_MISMATCH -> penalty.getAvailability();
                })
                .reduce(1.0, (a, b) -> a * b);
    }

    private List<MatchWarning> findWarnings(Tutor tutor, Post post) {
        List<MatchWarning> warnings = new ArrayList<>();
        if (!subjectMatches(tutor, post)) {
            warnings.add(MatchWarning.SUBJECT_MISMATCH);
        }
        if (!levelMatches(tutor.getTeachingLevels(), post.getEducationLevel())) {
            warnings.add(MatchWarning.LEVEL_MISMATCH);
        }
        if (!modeMatches(tutor.getTeachingMode(), post.getTeachingMode())) {
            warnings.add(MatchWarning.MODE_MISMATCH);
        }

        if (!areaMatcher.matches(tutor, post)) {
            warnings.add(MatchWarning.AREA_MISMATCH);
        }
        if (!availabilityChecker.hasCapacity(tutor, post)) {
            warnings.add(MatchWarning.AVAILABILITY_MISMATCH);
        }
        return warnings;
    }

    // =====================================================================
    // Các tiêu chí thành phần
    // =====================================================================

    /** Ưu tiên phí gia sư đề xuất cho bài này, không có thì dùng giá hồ sơ. */
    private BigDecimal effectiveFee(Application application, Tutor tutor) {
        return application.getProposedFee() != null
                ? application.getProposedFee()
                : tutor.getHourlyRate();
    }

    private double priceFit(BigDecimal feeValue, BigDecimal min, BigDecimal max) {
        MatchingProperties.Price priceProps = matchingProperties.getPrice();
        // Thiếu ngân sách hoặc thiếu phí: không đủ dữ liệu so sánh, chấm trung tính
        if (feeValue == null || (min == null && max == null)) {
            return priceProps.getNoBudgetScore();
        }

        double fee = feeValue.doubleValue();
        double lo = min != null ? min.doubleValue() : 0;
        double hi = max != null ? max.doubleValue() : Double.MAX_VALUE;

        if (fee >= lo && fee <= hi) return 1.0;
        if (fee < lo) return priceProps.getBelowBudgetScore();
        if (hi <= 0) return 0;
        return Math.max(0, 1 - (fee - hi) / hi);
    }

    /** Trung bình Bayesian: gia sư ít review bị kéo về mức TB hệ thống, không bị thiệt. */
    private double ratingScore(double averageRating, long reviewCount) {
        int minVotes = matchingProperties.getMinVotes();
        double bayesian = (reviewCount * averageRating + minVotes * matchingProperties.getGlobalAvgRating())
                / (reviewCount + minVotes);
        return bayesian / matchingProperties.getMaxRating();
    }

    private double experienceScore(int years) {
        return Math.min(Math.max(years, 0) / matchingProperties.getExperienceCapYears(), 1.0);
    }

    private boolean subjectMatches(Tutor tutor, Post post) {
        if (post.getSubject() == null || tutor.getSubjects() == null) return false;
        UUID postSubjectId = post.getSubject().getId();
        return tutor.getSubjects().stream()
                .anyMatch(s -> s.getId().equals(postSubjectId));
    }

    /** Thiếu dữ liệu (gia sư cũ) thì coi là trung tính, không phạt. */
    private boolean levelMatches(Set<EducationLevel> tutorLevels, EducationLevel postLevel) {
        if (tutorLevels == null || tutorLevels.isEmpty() || postLevel == null) return true;
        return tutorLevels.contains(postLevel);
    }

    private boolean modeMatches(TeachingMode tutorMode, TeachingMode postMode) {
        if (tutorMode == null || postMode == null) return true;
        if (tutorMode == TeachingMode.BOTH || postMode == TeachingMode.BOTH) return true;
        return tutorMode == postMode;
    }

    // =====================================================================
    // Hàm hỗ trợ
    // =====================================================================

    /** Đếm review cho cả danh sách trong 1 query, tránh N+1. */
    private Map<UUID, Long> loadReviewCounts(List<Application> applications) {
        List<UUID> tutorIds = applications.stream()
                .map(a -> a.getTutor().getId())
                .distinct()
                .toList();

        return reviewRepository.countByTutorIds(tutorIds).stream()
                .collect(Collectors.toMap(ReviewCountRow::getTutorId, ReviewCountRow::getCnt));
    }

    private ScoredApplicationResponse toScoredApplicationResponse(Scored scored) {
        return ScoredApplicationResponse.builder()
                .application(applicationMapper.toApplicationResponse(scored.application()))
                .score(scored.score())
                .breakdown(scored.breakdown())
                .warnings(scored.warnings())
                .build();
    }

    /**
     * proposedFee do gia sư nhập theo đơn vị của bài đăng (form ứng tuyển cần hiển thị đơn vị này).
     * Không có thì quy giá theo giờ trong hồ sơ về đơn vị của bài đăng.
     */
    private BigDecimal effectiveFee(Application application, Tutor tutor, Post post) {
        if (application.getProposedFee() != null) {
            return application.getProposedFee();
        }
        return convertHourlyRate(tutor.getHourlyRate(), post);
    }

    // Trả null khi thiếu dữ liệu để quy đổi, priceFit sẽ chấm trung tính
    private BigDecimal convertHourlyRate(BigDecimal hourlyRate, Post post) {
        if (hourlyRate == null) return null;

        BudgetUnit unit = post.getBudgetUnit();
        if (unit == null || unit == BudgetUnit.PER_HOUR) return hourlyRate;
        if (post.getDurationMinutes() == null) return null;

        BigDecimal perSession = hourlyRate
                .multiply(BigDecimal.valueOf(post.getDurationMinutes()))
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);

        return switch (unit) {
            case PER_SESSION -> perSession;
            case PER_MONTH -> post.getSessionsPerWeek() == null
                    ? null
                    : perSession.multiply(BigDecimal.valueOf(post.getSessionsPerWeek()))
                    .multiply(BigDecimal.valueOf(matchingProperties.getWeeksPerMonth()));
            default -> hourlyRate;
        };
    }
}