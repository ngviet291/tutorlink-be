package org.group3.tutorlink.features.post.recommendation;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.enums.MatchWarning;
import org.group3.tutorlink.features.application.matching.AreaMatcher;
import org.group3.tutorlink.features.application.matching.scoring.MismatchPenaltyPolicy;
import org.group3.tutorlink.features.application.matching.semantic.TutorProfileTextBuilder;
import org.group3.tutorlink.features.application.matching.signal.ExpectedFeeResolver;
import org.group3.tutorlink.features.application.matching.signal.PriceSignal;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.exception.TutorNotVerifiedException;
import org.group3.tutorlink.features.post.exception.TutorProfileIncompleteException;
import org.group3.tutorlink.features.post.recommendation.scoring.RecencyScorer;
import org.group3.tutorlink.features.post.recommendation.scoring.ScoreAccumulator;
import org.group3.tutorlink.features.post.recommendation.semantic.PostSemanticMatcher;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.CertificateRepository;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.TutorSubjectProfile;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gợi ý bài FIND_TUTOR (học viên đăng tìm gia sư) cho gia sư.
 * Chỉ xét môn gia sư vừa đăng ký vừa có chứng chỉ đã xác thực, đúng điều kiện ứng tuyển.
 * Cấp học, hình thức dạy, khu vực xa, lịch rảnh không khớp thì chỉ bị giảm điểm.
 */
@Component
@RequiredArgsConstructor
public class TutorPostRecommender implements PostRecommender {

    private final PostCandidateFinder candidateFinder;
    private final CertificateRepository certificateRepository;
    private final PostSemanticMatcher semanticMatcher;
    private final TutorProfileTextBuilder profileTextBuilder;
    private final AreaMatcher areaMatcher;
    private final MismatchPenaltyPolicy penaltyPolicy;
    private final ExpectedFeeResolver feeResolver;
    private final RecencyScorer recencyScorer;
    private final MatchingProperties properties;

    @Override
    public boolean supports(User user) {
        return user instanceof Tutor;
    }

    @Override
    public List<ScoredPost> recommend(User user, int limit) {
        Tutor tutor = (Tutor) user;
        ensureCanApply(tutor);

        Set<UUID> subjectIds = applicableSubjectIds(tutor);
        if (subjectIds.isEmpty()) {
            return List.of();
        }
        List<Post> posts = candidateFinder.find(PostType.FIND_TUTOR, tutor.getId(), subjectIds);
        if (posts.isEmpty()) {
            return List.of();
        }

        Map<UUID, Double> semanticScores = semanticMatcher.score(
                profileTextBuilder.build(tutor),
                posts.stream().map(Post::getId).toList());
        Instant now = Instant.now();

        return posts.stream()
                .map(post -> score(tutor, post, semanticScores, now))
                .sorted(ScoredPost.BY_RELEVANCE)
                .limit(limit)
                .toList();
    }

    // Cùng thứ tự kiểm tra với createApplication
    private void ensureCanApply(Tutor tutor) {
        if (tutor.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new TutorNotVerifiedException();
        }
        if (!tutor.hasCompleteProfile()) {
            throw new TutorProfileIncompleteException();
        }
    }

    private Set<UUID> applicableSubjectIds(Tutor tutor) {
        if (tutor.getSubjects() == null) {
            return Set.of();
        }
        Set<UUID> verified = certificateRepository.findVerifiedSubjectIds(tutor.getId());
        return tutor.getSubjects().stream()
                .map(Subject::getId)
                .filter(verified::contains)
                .collect(Collectors.toSet());
    }

    private ScoredPost score(Tutor tutor, Post post, Map<UUID, Double> semanticScores, Instant now) {
        MatchingProperties.TutorWeights weights = properties.getPostFeed().getTutor();
        ScoreAccumulator accumulator = new ScoreAccumulator();

        Double semantic = semanticScores.get(post.getId());
        if (semantic != null) {
            accumulator.add("semantic", weights.getSemanticWeight(), semantic);
        }
        areaMatcher.evaluate(tutor, post)
                .ifPresent(fit -> accumulator.add("area", weights.getAreaWeight(), fit.score()));
        priceScore(tutor, post)
                .ifPresent(price -> accumulator.add("price", weights.getPriceWeight(), price));
        accumulator.add("recency", weights.getRecencyWeight(), recencyScorer.score(post, now));

        List<MatchWarning> warnings = penaltyPolicy.detect(tutor, post);
        double total = 100 * accumulator.average() * penaltyPolicy.factorOf(warnings);
        return new ScoredPost(post, total, accumulator.breakdown(), warnings);
    }

    /** Thiếu ngân sách hoặc thiếu phí để so sánh thì bỏ qua tiêu chí giá. */
    private Optional<Double> priceScore(Tutor tutor, Post post) {
        if (post.getMinBudget() == null && post.getMaxBudget() == null) {
            return Optional.empty();
        }
        return feeResolver.fromHourlyRate(hourlyRateFor(tutor, post), post)
                .map(fee -> PriceSignal.fit(
                        fee.doubleValue(), post.getMinBudget(), post.getMaxBudget(), properties.getPrice()));
    }

    /** Ưu tiên phí theo giờ khai riêng cho môn của bài, không có thì dùng phí chung của gia sư. */
    private BigDecimal hourlyRateFor(Tutor tutor, Post post) {
        UUID subjectId = post.getSubject().getId();
        if (tutor.getSubjectProfiles() != null) {
            Optional<BigDecimal> subjectRate = tutor.getSubjectProfiles().stream()
                    .filter(profile -> profile.getSubject().getId().equals(subjectId))
                    .map(TutorSubjectProfile::getHourlyRate)
                    .filter(Objects::nonNull)
                    .findFirst();
            if (subjectRate.isPresent()) {
                return subjectRate.get();
            }
        }
        return tutor.getHourlyRate();
    }
}