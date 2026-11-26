package org.group3.tutorlink.features.post.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.logging.SystemLogPublisher;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.util.ApplicationParties;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.request.UpdatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostApplicationResponse;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.dto.response.PostStatisticsByStatus;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.BudgetUnit;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.post.event.PostChangedEvent;
import org.group3.tutorlink.features.post.exception.PostNotFoundException;
import org.group3.tutorlink.features.post.exception.SubjectNotBelongToTutorException;
import org.group3.tutorlink.features.post.exception.TutorNotVerifiedException;
import org.group3.tutorlink.features.post.mapper.PostMapper;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.post.specification.PostSpecification;
import org.group3.tutorlink.features.post.service.PostService;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.exception.SubjectNotFoundException;
import org.group3.tutorlink.features.subject.repository.CertificateRepository;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.tracking.enums.SystemAction;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    // Đơn đang chờ xét duyệt hoặc chờ thanh toán đều cần biết bài không còn nhận
    private static final List<ApplicationStatus> WAITING_STATUSES =
            List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED);
    private final PostRepository postRepository;
    private final SubjectRepository subjectRepository;
    private final PostMapper postMapper;
    private final AppUtil appUtil;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final SystemLogPublisher systemLogPublisher;
    // [NEW] dùng cho visibility check post PENDING/CLOSED trong getPostById
    private final ApplicationRepository applicationRepository;
    private final NotificationHelper notificationHelper;
    private final CertificateRepository certificateRepository;
    // Đồng bộ vector store (gợi ý bài đăng) sau khi giao dịch commit
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public PostResponse createPost(CreatePostRequest request) {
        User user = userRepository.findById(
                appUtil.userIdFromAuthentication()
        ).orElseThrow(UserNotFoundException::new);

        validateAuthor(request.getType(), user);

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(SubjectNotFoundException::new);

        checkTutorCanTeach(user, subject);

        Post post = postMapper.toPost(appUtil.generateUUID(), user, subject, request);
        Post savedPost = postRepository.save(post);
        publishChanged(savedPost.getId());
        systemLogPublisher.success(user.getId(), SystemAction.POST_CREATED,
                Post.class.getSimpleName(), savedPost.getId(),
                Map.of("type", request.getType().name(),
                        "subjectId", subject.getId().toString()));
        return postMapper.toPostResponse(savedPost);
    }

    /**
     * Chi tiết bài đăng của chính người dùng.
     * Repository phải giới hạn cả postId và authorId.
     */
    @Transactional(readOnly = true)
    @Override
    public PostResponse getPostByIdAndMe(UUID postId) {
        UUID currentUserId = appUtil.userIdFromAuthentication();

        Post post = postRepository
                .findByIdWithSubjectAndWithAuthor(postId, currentUserId)
                .orElseThrow(PostNotFoundException::new);

        return postMapper.toPostResponse(post);
    }

    /**
     * Chi tiết bài đăng công khai.
     */
    // [OLD - giữ lại tham khảo] Chỉ serve PUBLISHED — PENDING/CLOSED/DRAFT đều 404
    // với mọi user → applicant không xem được bài đã match, FE drop đơn.
    // @Transactional(readOnly = true)
    // @Override
    // public PostResponse getPostById(UUID postId) {
    //     Post post = postRepository.findByIdAndStatusNot(postId)
    //             .orElseThrow(PostNotFoundException::new);
    //     return postMapper.toPostResponse(post);
    // }

    @Transactional(readOnly = true)
    @Override
    public PostResponse getPostById(UUID postId) {
        Post post = postRepository.findByIdForVisibilityCheck(postId)
                .orElseThrow(PostNotFoundException::new);

        if (post.getStatus() == PostStatus.PUBLISHED) {
            return postMapper.toPostResponse(post);
        }

        UUID userId = appUtil.userIdFromAuthenticationOrNull();
        if (userId != null && isPostParticipant(post, userId)) {
            return postMapper.toPostResponse(post);
        }

        throw new PostNotFoundException();
    }

    private boolean isPostParticipant(Post post, UUID userId) {
        if (post.getAuthor().getId().equals(userId)) {
            return true;
        }
        return applicationRepository.existsParticipantByPostIdAndUserId(post.getId(), userId);
    }

    /**
     * Danh sách bài đăng công khai.
     */
    @Transactional(readOnly = true)
    @Override
    public CursorResponse<PostResponse> getAllPosts(
            String keyword,
            PostType type,
            String subjectName,
            String province,
            String ward,
            TeachingMode teachingMode,
            EducationLevel educationLevel,
            BigDecimal maxBudget,
            BigDecimal minBudget,
            BudgetUnit budgetUnit,
            UUID authorId,
            UUID cursor,
            int limit
    ) {

        Specification<Post> spec = Specification
                .where(PostSpecification.keyword(keyword))
                .and(PostSpecification.hasStatus(PostStatus.PUBLISHED))
                .and(PostSpecification.hasType(type))
                .and(PostSpecification.hasSubject(subjectName))
                .and(PostSpecification.provinceContains(province))
                .and(PostSpecification.wardContains(ward))
                .and(PostSpecification.hasTeachingMode(teachingMode))
                .and(PostSpecification.hasEducationLevel(educationLevel))
                .and(PostSpecification.maxBudgetLessThanOrEqual(maxBudget))
                .and(PostSpecification.minBudgetGreaterThanOrEqual(minBudget))
                .and(PostSpecification.hasBudgetUnit(budgetUnit))
                .and(PostSpecification.hasAuthorId(authorId))
                .and(PostSpecification.fetchSubject())
                .and(PostSpecification.fetchApplications())
                .and(PostSpecification.cursor(cursor));

        Pageable pageable = PageRequest.of(
                0,
                limit + 1,
                Sort.by(Sort.Order.desc("id"))
        );

        List<Post> posts = postRepository.findAll(spec, pageable)
                .getContent()
                .stream()
                .distinct()
                .toList();

        return appUtil.buildCursorResponse(
                posts,
                limit,
                Post::getId,
                postMapper::toPostResponse
        );

    }

    /**
     * Danh sách bài đăng dành cho ADMIN.
     */
    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public CursorResponse<PostResponse> getAllPostsAdmin(
            PostStatus status,
            String keyword,
            PostType type,
            String subjectName,
            TeachingMode teachingMode,
            EducationLevel educationLevel,
            BigDecimal maxBudget,
            BigDecimal minBudget,
            BudgetUnit budgetUnit,
            UUID cursor,
            int limit
    ) {
        Specification<Post> spec = Specification
                .where(PostSpecification.keyword(keyword))
                .and(status != null
                        ? PostSpecification.hasStatus(status)
                        : PostSpecification.hasStatus(PostStatus.values()))
                .and(PostSpecification.hasType(type))
                .and(PostSpecification.hasSubject(subjectName))
                .and(PostSpecification.hasTeachingMode(teachingMode))
                .and(PostSpecification.hasEducationLevel(educationLevel))
                .and(PostSpecification.maxBudgetLessThanOrEqual(maxBudget))
                .and(PostSpecification.minBudgetGreaterThanOrEqual(minBudget))
                .and(PostSpecification.hasBudgetUnit(budgetUnit))
                .and(PostSpecification.fetchSubject())
                .and(PostSpecification.fetchApplications())
                .and(PostSpecification.cursor(cursor));

        Pageable pageable = PageRequest.of(
                0,
                limit + 1,
                Sort.by(Sort.Order.desc("id"))
        );

        List<Post> posts = postRepository.findAll(spec, pageable)
                .getContent()
                .stream()
                .distinct()
                .toList();

        return appUtil.buildCursorResponse(
                posts,
                limit,
                Post::getId,
                postMapper::toPostResponse
        );
    }

    /**
     * Chi tiết một bài đăng bất kỳ (mọi trạng thái) dành cho ADMIN.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public PostResponse getPostByIdAdmin(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        return postMapper.toPostResponse(post);
    }

    /**
     * Cập nhật bài đăng của người dùng hiện tại.
     */
    @Transactional
    @Override
    public PostResponse updatePost(
            UUID postId,
            UpdatePostRequest request
    ) {
        Post post = postRepository
                .findByIdWithSubjectAndWithAuthor(
                        postId,
                        appUtil.userIdFromAuthentication()
                )
                .orElseThrow(PostNotFoundException::new);

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(SubjectNotFoundException::new);

        checkTutorCanTeach(post.getAuthor(), subject);

        postMapper.updatePostFromRequest(post, request);
        post.setSubject(subject);

        Post updatedPost = postRepository.save(post);
        publishChanged(updatedPost.getId());
        return postMapper.toPostResponse(updatedPost);
    }

    @Transactional
    @Override
    public void closePost(UUID postId) {
        Post post = postRepository
                .findByIdWithSubjectAndWithAuthor(
                        postId,
                        appUtil.userIdFromAuthentication()
                )
                .orElseThrow(PostNotFoundException::new);

        post.setStatus(PostStatus.CLOSED);
        notifyWaitingApplicants(post, NotificationMessage.POST_CLOSED_APPLICANT);
    }


    @Transactional
    @Override
    public void removePost(UUID postId) {
        Post post = postRepository
                .findByIdWithSubjectAndWithAuthor(
                        postId,
                        appUtil.userIdFromAuthentication()
                )
                .orElseThrow(PostNotFoundException::new);

        post.setStatus(PostStatus.REMOVED);
        publishChanged(post.getId());
        notifyWaitingApplicants(post, NotificationMessage.POST_REMOVED_BY_AUTHOR);
    }

    /**
     * Lấy tất cả bài đăng của người dùng hiện tại.
     * <p>
     * postStatus == null: lấy tất cả trạng thái.
     * postStatus != null: chỉ lấy đúng trạng thái được yêu cầu.
     */
    @Transactional(readOnly = true)
    @Override
    public CursorResponse<PostResponse> getCurrentUserPosts(
            String keyword,
            PostType type,
            String subjectName,
            TeachingMode teachingMode,
            EducationLevel educationLevel,
            BigDecimal maxBudget,
            BigDecimal minBudget,
            BudgetUnit budgetUnit,
            UUID cursor,
            PostStatus postStatus,
            int limit
    ) {
        UUID currentUserId = appUtil.userIdFromAuthentication();
        log.info(
                "[GET_MY_POSTS] userId={}, keyword={}, type={}, subjectName={}, " +
                        "teachingMode={}, educationLevel={}, maxBudget={}, minBudget={}, " +
                        "cursor={}, postStatus={}, limit={}",
                currentUserId,
                keyword,
                type,
                subjectName,
                teachingMode,
                educationLevel,
                maxBudget,
                minBudget,
                cursor,
                postStatus,
                limit);
        Specification<Post> spec = Specification
                .where(PostSpecification.keyword(keyword))
                .and(PostSpecification.hasStatus(postStatus))
                .and(PostSpecification.hasStatusNotRemoved(postStatus))
                .and(PostSpecification.hasType(type))
                .and(PostSpecification.hasSubject(subjectName))
                .and(PostSpecification.hasTeachingMode(teachingMode))
                .and(PostSpecification.hasEducationLevel(educationLevel))
                .and(PostSpecification.maxBudgetLessThanOrEqual(maxBudget))
                .and(PostSpecification.minBudgetGreaterThanOrEqual(minBudget))
                .and(PostSpecification.hasBudgetUnit(budgetUnit))
                .and(PostSpecification.fetchSubject())
                .and(PostSpecification.cursor(cursor))
                .and(PostSpecification.hasAuthorId(currentUserId));

        Pageable pageable = PageRequest.of(
                0,
                limit + 1,
                Sort.by(Sort.Order.desc("id"))
        );

        List<Post> posts = postRepository.findAll(spec, pageable)
                .getContent();

        return appUtil.buildCursorResponse(
                posts,
                limit,
                Post::getId,
                postMapper::toPostResponse
        );
    }

    @Transactional
    @Override
    public PostResponse publishPost(UUID postId) {
        Post post = postRepository
                .findByIdWithSubjectAndWithAuthor(
                        postId,
                        appUtil.userIdFromAuthentication()
                )
                .orElseThrow(PostNotFoundException::new);

        post.setStatus(PostStatus.PUBLISHED);
        return postMapper.toPostResponse(post);
    }

    @Transactional(readOnly = true)
    @Override
    public CursorResponse<PostApplicationResponse> getPostApplications(
            UUID postId,
            int limit,
            UUID cursor
    ) {
        Pageable pageable = PageRequest.of(
                0,
                limit + 1,
                Sort.by(Sort.Order.desc("id"))
        );

        List<PostApplicationResponse> posts =
                postRepository.findPostApplications(
                        postId,
                        appUtil.userIdFromAuthentication(),
                        pageable,
                        cursor
                );

        return appUtil.buildCursorResponse(
                posts,
                posts.size(),
                PostApplicationResponse::getApplicationId,
                Function.identity()
        );
    }

    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<PostStatisticsByStatus> getStatisticsByStatus() {
        return postRepository.getStatisticsByStatus();
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public void removePostAdmin(UUID postId, String reason) {
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        post.setStatus(PostStatus.REMOVED);
        post.setRemovalReason(reason);
        publishChanged(post.getId());
        notificationHelper.notify(post.getAuthor(), NotificationMessage.POST_REMOVED_BY_ADMIN_AUTHOR,
                NotificationTarget.MY_POSTS, null, post.getTitle(), reason);
        notifyWaitingApplicants(post, NotificationMessage.POST_REMOVED_BY_ADMIN);
    }

    /**
     * Báo các ứng viên đang chờ (PENDING, ACCEPTED) rằng bài không còn nhận.
     */
    private void notifyWaitingApplicants(Post post, NotificationMessage message) {
        for (ApplicationStatus status : WAITING_STATUSES) {
            applicationRepository.findByPostIdAndApplicationStatus(post.getId(), status, Pageable.unpaged())
                    .forEach(application -> notificationHelper.notify(
                            ApplicationParties.applicantOf(application), message,
                            NotificationTarget.MY_APPLICATIONS, null, post.getTitle()));
        }
    }
    private void publishChanged(UUID postId) {
        eventPublisher.publishEvent(PostChangedEvent.builder().postId(postId).build());
    }

    /** Gia sư chỉ được đăng bài môn đã đăng ký và có chứng chỉ xác thực cho môn đó. */
    private void checkTutorCanTeach(User user, Subject subject) {
        if (user instanceof Tutor t && !t.hasCompleteProfile()) {
            throw new org.group3.tutorlink.features.post.exception.TutorProfileIncompleteException();
        }
        if (user instanceof Tutor tutor
                && (!tutorRepository.existsSubject(tutor.getId(), subject.getId())
                || !certificateRepository.existsByTutor_IdAndSubject_IdAndVerifiedTrue(
                tutor.getId(), subject.getId()))) {
            throw new SubjectNotBelongToTutorException();
        }
    }
}