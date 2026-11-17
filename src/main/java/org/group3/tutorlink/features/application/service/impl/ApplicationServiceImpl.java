package org.group3.tutorlink.features.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.logging.SystemLogPublisher;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.event.TutorProfileChangedEvent;
import org.group3.tutorlink.features.application.exception.ApplicationNotFoundException;
import org.group3.tutorlink.features.application.exception.ApplicationInvalidStatusException;
import org.group3.tutorlink.features.application.exception.ApplicationNotAllowedException;
import org.group3.tutorlink.features.application.exception.ErrorCodeApplication;
import org.group3.tutorlink.features.application.mapper.ApplicationMapper;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.service.ApplicationService;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.notification.service.NotificationCreator;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.service.RefundService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.tracking.enums.SystemAction;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AppUtil appUtil;
    private final ApplicationMapper applicationMapper;
    private final SystemLogPublisher systemLogPublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationCreator notificationCreator;
    private final RefundService refundService;

    private static final String NOTI_APPLICATION_CREATED_TITLE = "Bạn có đơn ứng tuyển mới";
    private static final String NOTI_APPLICATION_SELECTED_TITLE = "Bạn được chọn nhận lớp";
    private static final String NOTI_APPLICATION_REJECTED_TITLE = "Đơn ứng tuyển không được chọn";
    private static final String NOTI_APPLICATION_UNSELECTED_TITLE = "Lựa chọn ứng viên đã được hủy";
    private static final String NOTI_APPLICATION_CONFIRMED_TITLE = "Lớp học đã được xác nhận";
    private static final String NOTI_APPLICATION_FINISHED_TITLE = "Lớp học đã hoàn thành";
    private static final String NOTI_APPLICATION_CANCELLED_TITLE = "Đơn ứng tuyển đã bị hủy";

    private static final List<ApplicationStatus> LIVE_STATUSES = List.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.ACCEPTED,
            ApplicationStatus.CONFIRMED,
            ApplicationStatus.FINISHED
    );

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse createApplication(CreateApplicationRequest request) {

        UUID userId = appUtil.userIdFromAuthentication();
        User currentUser = getUserOrThrow(userId);

        String applicant = currentUser instanceof Student ? "Student" : currentUser instanceof Tutor ? "Tutor" : "Unknown";

        Post post = postRepository.findAvailableForApplication(
                        request.getPostId(),
                        userId,
                        Instant.now()
                )
                .orElseThrow(() -> new AppException(ErrorCodeApplication.POST_NOT_FOUND));
        Application application = Application.builder()
                .id(appUtil.generateUUID())
                .post(post)
                .applicationStatus(ApplicationStatus.PENDING)
                .message(request.getMessage())
                .proposedFee(request.getProposedFee())
                .appliedAt(Instant.now())
                .build();

        if (post.getType() == PostType.FIND_TUTOR) {

            if (!(currentUser instanceof Tutor tutor)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }
            if (!(post.getAuthor() instanceof Student student)) {
                throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
            }

            if (applicationRepository.existsByPostIdAndTutorId(post.getId(), tutor.getId(), LIVE_STATUSES)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_ALREADY_EXISTS);
            }

            eventPublisher.publishEvent(TutorProfileChangedEvent.builder().tutorId(tutor.getId()).build());
            application.setTutor(tutor);
            application.setStudent(student);

        } else if (post.getType() == PostType.FIND_STUDENT) {

            if (!(currentUser instanceof Student student)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }
            if (!(post.getAuthor() instanceof Tutor tutor)) {
                throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
            }

            if (applicationRepository.existsByPostIdAndStudentId(post.getId(), student.getId(), LIVE_STATUSES)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_ALREADY_EXISTS);
            }

            application.setStudent(student);
            application.setTutor(tutor);

        } else {
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }

        Application saved = applicationRepository.save(application);
        systemLogPublisher.success(userId, SystemAction.APPLICATION_CREATED,
                Application.class.getSimpleName(), saved.getId(),
                Map.of("postId", post.getId().toString(), "proposedFee", String.valueOf(saved.getProposedFee()), "applicant", applicant));

        notifyUser(post.getAuthor(), NotificationType.APPLICATION,
                NOTI_APPLICATION_CREATED_TITLE,
                String.format("%s đã ứng tuyển vào bài \"%s\"", currentUser.getFullName(), post.getTitle()));
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UUID applicationId) {

        UUID userId = appUtil.userIdFromAuthentication();
        User currentUser = getUserOrThrow(userId);
        Application application;
        if (currentUser instanceof Admin) {
            application = getApplicationOrThrow(applicationId);
        } else {
            application = applicationRepository
                    .findByIdForViewer(
                            applicationId,
                            userId
                    )
                    .orElseThrow(() -> new AppException(
                            ErrorCodeApplication.APPLICATION_NOT_ALLOWED
                    ));
        }

        // Đã sửa
        return applicationMapper.toApplicationResponse(application);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    @Transactional(readOnly = true)
    public CursorResponse<ApplicationResponse> getMyApplications(UUID cursor, int limit) {

        UUID userId = appUtil.userIdFromAuthentication();
        User currentUser = getUserOrThrow(userId);

        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Application> applications;
        if (currentUser instanceof Student) {
            applications = applicationRepository.findByStudentIdAfterCursor(userId, cursor, pageable);
        } else if (currentUser instanceof Tutor) {
            applications = applicationRepository.findByTutorIdAfterCursor(userId, cursor, pageable);
        } else {
            throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
        }

        return appUtil.buildCursorResponse(
                applications,
                limit,
                Application::getId,
                applicationMapper::toApplicationResponse
        );
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public CursorResponse<ApplicationResponse> getApplications(
            ApplicationStatus status,
            UUID cursor,
            int limit
    ) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Application> applications = applicationRepository
                .findApplicationsAfterCursor(status, cursor, pageable);

        return appUtil.buildCursorResponse(
                applications,
                limit,
                Application::getId,
                applicationMapper::toApplicationResponse
        );
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse selectApplication(UUID applicationId) {

        UUID userId = appUtil.userIdFromAuthentication();
        User currentUser = getUserOrThrow(userId);
        Application application = applicationRepository.findSelectableApplication(
                        applicationId,
                        userId,
                        ApplicationStatus.PENDING,
                        PostStatus.PUBLISHED
                )
                .orElseThrow(ApplicationNotFoundException::new);
        Post post = application.getPost();

        boolean alreadySelected = applicationRepository
                .existsByPostIdAndApplicationStatus(post.getId(), ApplicationStatus.ACCEPTED);

        if (alreadySelected) {
            throw new AppException(ErrorCodeApplication.APPLICATION_ALREADY_SELECTED);
        }

        validateSelectorRole(post, currentUser);

        application.setApplicationStatus(ApplicationStatus.ACCEPTED);
        application.setSelectedAt(Instant.now());

        Page<Application> pendingApplications = applicationRepository
                .findByPostIdAndApplicationStatus(
                        post.getId(),
                        ApplicationStatus.PENDING,
                        Pageable.unpaged()
                );

        List<Application> rejectedOthers = new java.util.ArrayList<>();
        for (Application other : pendingApplications.getContent()) {
            if (!other.getId().equals(application.getId())) {
                other.setApplicationStatus(ApplicationStatus.REJECTED);
                rejectedOthers.add(other);
            }
        }

        post.setStatus(PostStatus.PENDING);

        Application saved = applicationRepository.save(application);

        User applicant = applicantOf(saved);
        // [PAYMENT-NOTI] Phí luôn do gia sư trả — chỉ nhắc người trả phí, không báo nhầm student.
        // FIND_TUTOR: gia sư là ứng viên (applicant) -> nhắc applicant thanh toán như cũ.
        // FIND_STUDENT: gia sư là chủ bài -> nhắc chủ bài thanh toán, báo học viên đang chờ.
        if (post.getType() == PostType.FIND_STUDENT) {
            notifyUser(post.getAuthor(), NotificationType.APPLICATION,
                    NOTI_APPLICATION_SELECTED_TITLE,
                    String.format("Bạn đã chọn %s cho bài \"%s\". Vui lòng thanh toán phí trong thời hạn quy định.",
                            applicantName(saved), post.getTitle()));
            notifyUser(applicant, NotificationType.APPLICATION,
                    NOTI_APPLICATION_SELECTED_TITLE,
                    String.format("Bạn được chọn cho bài \"%s\". Vui lòng chờ gia sư thanh toán phí.", post.getTitle()));
        } else {
            notifyUser(applicant, NotificationType.APPLICATION,
                    NOTI_APPLICATION_SELECTED_TITLE,
                    String.format("Bạn được chọn cho bài \"%s\". Vui lòng thanh toán phí trong thời hạn quy định.", post.getTitle()));
        }
        for (Application rejected : rejectedOthers) {
            notifyUser(applicantOf(rejected), NotificationType.APPLICATION,
                    NOTI_APPLICATION_REJECTED_TITLE,
                    String.format("Bài \"%s\" đã chọn ứng viên khác.", post.getTitle()));
        }
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse rejectApplication(UUID applicationId) {
        UUID userId = appUtil.userIdFromAuthentication();
        User currentUser = getUserOrThrow(userId);
        Application application = applicationRepository.findByIdForSelection(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);
        Post post = application.getPost();

        if (post == null || post.getAuthor() == null
                || !post.getAuthor().getId().equals(currentUser.getId())) {
            throw new ApplicationNotAllowedException();
        }
        if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
            throw new ApplicationInvalidStatusException();
        }
        validateSelectorRole(post, currentUser);
        application.setApplicationStatus(ApplicationStatus.REJECTED);
        Application saved = applicationRepository.save(application);
        notifyUser(applicantOf(saved), NotificationType.APPLICATION,
                NOTI_APPLICATION_REJECTED_TITLE,
                String.format("Đơn ứng tuyển của bạn vào bài \"%s\" đã bị từ chối.", post.getTitle()));
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse confirmApplication(UUID applicationId) {

        UUID userId = appUtil.userIdFromAuthentication();
        getUserOrThrow(userId);
        Application application = getApplicationOrThrow(applicationId);
        Post post = application.getPost();

        if (application.getApplicationStatus() != ApplicationStatus.ACCEPTED) {
            throw new AppException(ErrorCodeApplication.INVALID_APPLICATION_STATUS);
        }

        if (post.getStatus() != PostStatus.PENDING) {
            throw new AppException(ErrorCodeApplication.INVALID_APPLICATION_STATUS);
        }

        if (application.getTransaction() == null
                || application.getTransaction().getStatus() != TransactionStatus.SUCCESS) {
            throw new AppException(ErrorCodeApplication.PAYMENT_REQUIRED);
        }

        // Applicant (người apply) xác nhận sau khi trả phí
        if (post.getType() == PostType.FIND_TUTOR) {
            if (application.getTutor() == null || !application.getTutor().getId().equals(userId)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }
        } else if (post.getType() == PostType.FIND_STUDENT) {
            if (application.getStudent() == null || !application.getStudent().getId().equals(userId)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }
        } else {
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }

        application.setApplicationStatus(ApplicationStatus.CONFIRMED);
        post.setStatus(PostStatus.CLOSED);

        Application saved = applicationRepository.save(application);
        notifyUser(post.getAuthor(), NotificationType.APPLICATION,
                NOTI_APPLICATION_CONFIRMED_TITLE,
                String.format("%s đã xác nhận lớp học cho bài \"%s\".", applicantName(saved), post.getTitle()));
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse unselectApplication(UUID applicationId) {

        UUID userId = appUtil.userIdFromAuthentication();
        getUserOrThrow(userId);
        Application application = applicationRepository
                .findByIdForViewer(applicationId, userId)
                .orElseThrow(() -> new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED));
        Post post = application.getPost();

        if (post == null || post.getAuthor() == null
                || !post.getAuthor().getId().equals(userId)) {
            throw new ApplicationNotAllowedException();
        }

        if (application.getApplicationStatus() != ApplicationStatus.ACCEPTED) {
            throw new ApplicationInvalidStatusException();
        }

        if (application.getTransaction() != null
                && application.getTransaction().getStatus() == TransactionStatus.SUCCESS) {
            throw new AppException(ErrorCodeApplication.INVALID_APPLICATION_STATUS);
        }

        application.setApplicationStatus(ApplicationStatus.PENDING);
        application.setSelectedAt(null);
        revertPostToPublished(post);

        Application saved = applicationRepository.save(application);
        notifyUser(applicantOf(saved), NotificationType.APPLICATION,
                NOTI_APPLICATION_UNSELECTED_TITLE,
                String.format("Chủ bài đã bỏ chọn bạn cho bài \"%s\". Đơn quay về trạng thái chờ xử lý.", post.getTitle()));
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse finishApplication(UUID applicationId) {
        UUID userId = appUtil.userIdFromAuthentication();
        User currentUser = getUserOrThrow(userId);
        Application application = getApplicationOrThrow(applicationId);

        if (application.getStudent() == null && application.getTutor() == null) {
            throw new ApplicationNotAllowedException();
        }
        boolean isParticipant = isApplicationParticipant(application, userId);
        boolean isStudentOrTutor = currentUser instanceof Student
                || currentUser instanceof Tutor;
        if (!isParticipant || !isStudentOrTutor) {
            throw new ApplicationNotAllowedException();
        }
        // Chỉ CONFIRMED -> FINISHED; chặn re-finish để không gửi notification lặp
        if (application.getApplicationStatus() != ApplicationStatus.CONFIRMED) {
            throw new ApplicationInvalidStatusException();
        }
        application.setApplicationStatus(ApplicationStatus.FINISHED);
        Application saved = applicationRepository.save(application);
        Post finishedPost = saved.getPost();
        User other = finishedPost != null && finishedPost.getAuthor() != null
                && finishedPost.getAuthor().getId().equals(userId)
                ? applicantOf(saved)
                : finishedPost != null ? finishedPost.getAuthor() : null;
        notifyUser(other, NotificationType.APPLICATION,
                NOTI_APPLICATION_FINISHED_TITLE,
                String.format("Lớp học cho bài \"%s\" đã hoàn thành.", finishedPost != null ? finishedPost.getTitle() : ""));
        return applicationMapper.toApplicationResponse(saved);
    }

    private boolean isApplicationParticipant(Application application, UUID userId) {
        return isUserMatch(application.getStudent(), userId) || isUserMatch(application.getTutor(), userId) || isUserMatch(application.getPost().getAuthor(), userId);
    }

    private boolean isUserMatch(User user, UUID userId) {
        return user != null && user.getId().equals(userId);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse cancelApplication(UUID applicationId) {

        UUID userId = appUtil.userIdFromAuthentication();
        getUserOrThrow(userId);
        Application application = applicationRepository
                .findByIdForViewer(applicationId, userId)
                .orElseThrow(() -> new AppException(
                        ErrorCodeApplication.APPLICATION_NOT_ALLOWED
                ));
        Post post = application.getPost();

        ApplicationStatus currentStatus = application.getApplicationStatus();

        if (currentStatus != ApplicationStatus.PENDING
                && currentStatus != ApplicationStatus.ACCEPTED) {
            throw new AppException(ErrorCodeApplication.INVALID_APPLICATION_STATUS);
        }

        // Chỉ applicant (người apply) được cancel
        if (post.getType() == PostType.FIND_TUTOR) {
            if (application.getTutor() == null || !application.getTutor().getId().equals(userId)) {
                throw new ApplicationNotAllowedException();
            }
        } else if (post.getType() == PostType.FIND_STUDENT) {
            if (application.getStudent() == null || !application.getStudent().getId().equals(userId)) {
                throw new ApplicationNotAllowedException();
            }
        } else {
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }

        application.setApplicationStatus(ApplicationStatus.CANCELLED);

        if (currentStatus == ApplicationStatus.ACCEPTED) {
            application.setSelectedAt(null);
            // Đã trả phí mà applicant hủy -> hoàn lại commission cho tutor
            if (application.getTransaction() != null
                    && application.getTransaction().getStatus() == TransactionStatus.SUCCESS) {
                refundService.refundCommission(application);
            }
            revertPostToPublished(post);
        }

        Application saved = applicationRepository.save(application);
        notifyUser(post.getAuthor(), NotificationType.APPLICATION,
                NOTI_APPLICATION_CANCELLED_TITLE,
                String.format("%s đã hủy đơn ứng tuyển vào bài \"%s\".", applicantName(saved), post.getTitle()));
        return applicationMapper.toApplicationResponse(saved);
    }

    /** Người apply: FIND_TUTOR → tutor, FIND_STUDENT → student. */
    private User applicantOf(Application application) {
        Post post = application.getPost();
        if (post != null && post.getType() == PostType.FIND_TUTOR) {
            return application.getTutor();
        }
        return application.getStudent();
    }

    private String applicantName(Application application) {
        User applicant = applicantOf(application);
        return applicant != null ? applicant.getFullName() : "Ứng viên";
    }

    private void notifyUser(User recipient, NotificationType type, String title, String content) {
        if (recipient != null) {
            notificationCreator.createNotification(recipient.getId(), type, title, content);
        }
    }

    // Chỉ mở lại bài khi bài đang PENDING (đang chờ thanh toán/xác nhận).
    // Post CLOSED/REMOVED không được hồi sinh về PUBLISHED.
    private void revertPostToPublished(Post post) {
        if (post != null && post.getStatus() == PostStatus.PENDING) {
            post.setStatus(PostStatus.PUBLISHED);
            restoreRejectedApplications(post);
        }
    }

    private void restoreRejectedApplications(Post post) {
        Page<Application> rejectedApplications = applicationRepository
                .findByPostIdAndApplicationStatus(
                        post.getId(),
                        ApplicationStatus.REJECTED,
                        Pageable.unpaged()
                );
        for (Application rejected : rejectedApplications.getContent()) {
            rejected.setApplicationStatus(ApplicationStatus.PENDING);
        }
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private Application getApplicationOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);
    }

    private void validateSelectorRole(Post post, User currentUser) {
        if (post.getType() == PostType.FIND_TUTOR
                && !(currentUser instanceof Student)) {
            throw new AppException(
                    ErrorCodeApplication.APPLICATION_NOT_ALLOWED
            );
        }

        if (post.getType() == PostType.FIND_STUDENT
                && !(currentUser instanceof Tutor)) {
            throw new AppException(
                    ErrorCodeApplication.APPLICATION_NOT_ALLOWED
            );
        }

        if (post.getType() != PostType.FIND_TUTOR
                && post.getType() != PostType.FIND_STUDENT) {
            throw new AppException(
                    ErrorCodeApplication.POST_NOT_AVAILABLE
            );
        }
    }

}