package org.group3.tutorlink.features.post.recommendation;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.application.dto.projection.ReviewCountRow;
import org.group3.tutorlink.features.application.enums.MatchWarning;
import org.group3.tutorlink.features.application.matching.AreaMatcher;
import org.group3.tutorlink.features.application.matching.GradeLevelResolver;
import org.group3.tutorlink.features.application.matching.semantic.student.StudentProfileTextBuilder;
import org.group3.tutorlink.features.application.matching.signal.BayesianRating;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.recommendation.scoring.RecencyScorer;
import org.group3.tutorlink.features.post.recommendation.scoring.ScoreAccumulator;
import org.group3.tutorlink.features.post.recommendation.semantic.PostSemanticMatcher;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.review.repository.ReviewRepository;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gợi ý bài FIND_STUDENT (gia sư đăng tìm học viên) cho học viên.
 * Môn quan tâm suy ra từ các bài FIND_TUTOR học viên từng đăng; chưa có thì bỏ qua tiêu chí môn.
 */
@Component
@RequiredArgsConstructor
public class StudentPostRecommender implements PostRecommender {

    private final PostCandidateFinder candidateFinder;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final PostSemanticMatcher semanticMatcher;
    private final StudentProfileTextBuilder profileTextBuilder;
    private final AreaMatcher areaMatcher;
    private final GradeLevelResolver gradeResolver;
    private final BayesianRating bayesianRating;
    private final RecencyScorer recencyScorer;
    private final MatchingProperties properties;

    @Override
    public boolean supports(User user) {
        return user instanceof Student;
    }

    @Override
    public List<ScoredPost> recommend(User user, int limit) {
        Student student = (Student) user;

        List<Post> posts = candidateFinder.find(PostType.FIND_STUDENT, student.getId(), null);
        if (posts.isEmpty()) {
            return List.of();
        }

        StudentContext context = new StudentContext(
                gradeResolver.resolve(student.getGrade()),
                postRepository.findSubjectIdsByAuthor(student.getId(), PostType.FIND_TUTOR),
                countAuthorReviews(posts),
                semanticMatcher.score(
                        profileTextBuilder.build(student),
                        posts.stream().map(Post::getId).toList()),
                Instant.now());

        return posts.stream()
                .map(post -> score(student, post, context))
                .sorted(ScoredPost.BY_RELEVANCE)
                .limit(limit)
                .toList();
    }

    private Map<UUID, Long> countAuthorReviews(List<Post> posts) {
        List<UUID> authorIds = posts.stream().map(post -> post.getAuthor().getId()).distinct().toList();
        return reviewRepository.countByTutorIds(authorIds).stream()
                .collect(Collectors.toMap(ReviewCountRow::getTutorId, ReviewCountRow::getCnt, Long::sum));
    }

    private ScoredPost score(Student student, Post post, StudentContext context) {
        MatchingProperties.StudentWeights weights = properties.getPostFeed().getStudent();
        MatchingProperties.Penalty penalty = properties.getPenalty();
        ScoreAccumulator accumulator = new ScoreAccumulator();
        List<MatchWarning> warnings = new ArrayList<>();
        double factor = 1;

        Double semantic = context.semanticScores().get(post.getId());
        if (semantic != null) {
            accumulator.add("semantic", weights.getSemanticWeight(), semantic);
        }

        Optional<AreaMatcher.AreaFit> area = areaMatcher.evaluate(student.getAddress(), post);
        if (area.isPresent()) {
            accumulator.add("area", weights.getAreaWeight(), area.get().score());
            if (area.get().far()) {
                warnings.add(MatchWarning.AREA_MISMATCH);
                factor *= penalty.getArea();
            }
        }

        if (context.level().isPresent() && post.getEducationLevel() != null) {
            boolean match = context.level().get() == post.getEducationLevel();
            accumulator.add("level", weights.getLevelWeight(), match ? 1 : 0);
            if (!match) {
                warnings.add(MatchWarning.LEVEL_MISMATCH);
                factor *= penalty.getLevel();
            }
        }

        if (!context.interestSubjectIds().isEmpty()) {
            boolean interested = context.interestSubjectIds().contains(post.getSubject().getId());
            accumulator.add("subject", weights.getSubjectWeight(), interested ? 1 : 0);
        }

        if (post.getAuthor() instanceof Tutor author) {
            long reviews = context.reviewCounts().getOrDefault(author.getId(), 0L);
            accumulator.add("rating", weights.getRatingWeight(),
                    bayesianRating.normalized(author.getAverageRating(), reviews));
        }

        accumulator.add("recency", weights.getRecencyWeight(), recencyScorer.score(post, context.now()));

        double total = 100 * accumulator.average() * factor;
        return new ScoredPost(post, total, accumulator.breakdown(), warnings);
    }

    /** Dữ liệu tra cứu một lần cho cả lượt chấm điểm. */
    private record StudentContext(Optional<EducationLevel> level,
                                  Set<UUID> interestSubjectIds,
                                  Map<UUID, Long> reviewCounts,
                                  Map<UUID, Double> semanticScores,
                                  Instant now) {
    }
}