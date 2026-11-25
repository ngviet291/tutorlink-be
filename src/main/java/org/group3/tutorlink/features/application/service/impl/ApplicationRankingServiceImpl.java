package org.group3.tutorlink.features.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.application.dto.projection.StudentCountRow;
import org.group3.tutorlink.features.application.dto.response.RankedApplicationsResponse;
import org.group3.tutorlink.features.application.dto.response.ScoredApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.enums.MatchWarning;
import org.group3.tutorlink.features.application.exception.ErrorCodeApplication;
import org.group3.tutorlink.features.application.mapper.ApplicationMapper;
import org.group3.tutorlink.features.application.matching.scoring.*;
import org.group3.tutorlink.features.application.matching.semantic.student.StudentSemanticMatcher;
import org.group3.tutorlink.features.application.matching.signal.MatchContext;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.service.ApplicationRankingService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.exception.PostNotFoundException;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationRankingServiceImpl implements ApplicationRankingService {
    private static final Comparator<ScoredCandidate> BY_SCORE_DESC_THEN_APPLIED_AT =
            Comparator.comparingDouble(ScoredCandidate::getScore).reversed()
                    .thenComparing(c -> c.getApplication().getAppliedAt());

    private final MatchingProperties properties;
    private final ApplicationRepository applicationRepository;
    private final PostRepository postRepository;
    private final ApplicationMapper applicationMapper;
    private final MatchContextFactory contextFactory;
    private final WeightedScorer weightedScorer;
    private final MismatchPenaltyPolicy penaltyPolicy;
    private final AppUtil appUtil;
    private final StudentApplicantScorer studentScorer;
    private final StudentSemanticMatcher studentSemanticMatcher;


    // Học viên xếp gia sư ứng tuyển bài FIND_TUTOR; gia sư xếp học viên ứng tuyển bài FIND_STUDENT
    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT','TUTOR')")
    public RankedApplicationsResponse rankApplications(UUID postId) {

        Post post = loadPostOwnedByCurrentUser(postId);
        boolean findStudent = post.getType() == PostType.FIND_STUDENT;

        List<Application> applications = findStudent
                ? applicationRepository.findWithStudentByPostIdAndStatus(postId, ApplicationStatus.PENDING)
                : applicationRepository.findWithTutorByPostIdAndStatus(postId, ApplicationStatus.PENDING);

        if (applications.size() <= properties.getTopN()) {
            return buildUnranked(applications);
        }
        return findStudent ? buildRankedStudents(post, applications) : buildRanked(post, applications);
    }

    // Chỉ chủ bài mới xem: bài FIND_TUTOR của học viên, bài FIND_STUDENT của gia sư
    private Post loadPostOwnedByCurrentUser(UUID postId) {
        UUID userId = appUtil.userIdFromAuthentication();
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        if (post.getType() != PostType.FIND_TUTOR && post.getType() != PostType.FIND_STUDENT) {
            log.error("User {} tried to rank applications for post {} of type {}", userId, postId, post.getType());
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }
        if (!post.getAuthor().getId().equals(userId)) {
            throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
        }
        return post;
    }

    private RankedApplicationsResponse buildUnranked(List<Application> applications) {
        List<ScoredApplicationResponse> all = applications.stream()
                .sorted(Comparator.comparing(Application::getAppliedAt))
                .map(a -> ScoredApplicationResponse.builder()
                        .application(applicationMapper.toApplicationResponse(a))
                        .build())
                .toList();

        return RankedApplicationsResponse.builder().ranked(false).top(all).others(List.of()).build();
    }

    private RankedApplicationsResponse buildRanked(Post post, List<Application> applications) {
        MatchContext context = contextFactory.create(post, applications);
        int topN = properties.getTopN();

        List<ScoredApplicationResponse> sorted = applications.stream()
                .map(application -> evaluate(context, application))
                .sorted(BY_SCORE_DESC_THEN_APPLIED_AT)
                .map(this::toResponse)
                .toList();

        return RankedApplicationsResponse.builder()
                .ranked(true)
                .top(sorted.subList(0, topN))
                .others(sorted.subList(topN, sorted.size()))
                .build();
    }

    private RankedApplicationsResponse buildRankedStudents(Post post, List<Application> applications) {
        int topN = properties.getTopN();
        List<UUID> studentIds = applications.stream().map(a -> a.getStudent().getId()).distinct().toList();
        Map<UUID, Long> completed = applicationRepository
                .countByStudentIdsAndStatuses(studentIds, List.of(ApplicationStatus.CONFIRMED, ApplicationStatus.FINISHED))
                .stream()
                .collect(Collectors.toMap(StudentCountRow::getStudentId, StudentCountRow::getCnt, Long::sum));

        Map<UUID, Double> semantic = scoreSemantically(post, studentIds);

        List<ScoredApplicationResponse> sorted = applications.stream()
                .map(application -> studentScorer.score(post, application, completed, semantic))
                .sorted(BY_SCORE_DESC_THEN_APPLIED_AT)
                .map(this::toResponse)
                .toList();

        return RankedApplicationsResponse.builder()
                .ranked(true)
                .top(sorted.subList(0, topN))
                .others(sorted.subList(topN, sorted.size()))
                .build();
    }

    private Map<UUID, Double> scoreSemantically(Post post, List<UUID> studentIds) {
        try {
            return studentSemanticMatcher.score(post, studentIds);
        } catch (RuntimeException e) {
            log.warn("Student semantic matching unavailable, fallback to rule-based ranking: {}", e.getMessage());
            return Map.of();
        }
    }

    private ScoredCandidate evaluate(MatchContext context, Application application) {
        WeightedScore weighted = weightedScorer.score(context, application);
        List<MatchWarning> warnings = penaltyPolicy.detect(application.getTutor(), context.getPost());
        double total = weighted.getTotal() * penaltyPolicy.factorOf(warnings);
        return new ScoredCandidate(application, total, weighted.getBreakdown(), warnings);
    }

    private ScoredApplicationResponse toResponse(ScoredCandidate candidate) {
        return ScoredApplicationResponse.builder()
                .application(applicationMapper.toApplicationResponse(candidate.getApplication()))
                .score(candidate.getScore())
                .breakdown(candidate.getBreakdown())
                .warnings(candidate.getWarnings())
                .build();
    }
}