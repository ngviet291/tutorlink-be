package org.group3.tutorlink.features.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.logging.SystemLogPublisher;
import org.group3.tutorlink.common.utils.AfterCommit;
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
import org.group3.tutorlink.features.application.scheduler.ApplicationExpiryQueue;
import org.group3.tutorlink.features.application.service.ApplicationService;
import org.group3.tutorlink.features.application.util.ApplicationParties;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
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
    private final ApplicationExpiryQueue expiryQueue;
    private final AfterCommit afterCommit;
    private final NotificationHelper notificationHelper;

    @Value("${application.expiry.payment-window-minutes:1440}")
    private long paymentWindowMinutes;

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

        notificationHelper.notify(post.getAuthor(), NotificationMessage.APPLICATION_CREATED,
                NotificationTarget.POST, post.getId(), currentUser.getFullName(), post.getTitle());
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

        // Các đơn PENDING khác giữ nguyên; chỉ bị REJECTED khi gia sư được chọn đã trả phí (PaymentServiceImpl)
        post.setStatus(PostStatus.PENDING);

        Application saved = applicationRepository.save(application);
        schedulePaymentExpiry(saved);

        User applicant = ApplicationParties.applicantOf(saved);
        if (post.getType() == PostType.FIND_TUTOR) {
            notificationHelper.notify(applicant, NotificationMessage.APPLICATION_SELECTED_TUTOR,
                    NotificationTarget.MY_APPLICATIONS, null, post.getTitle());
        } else {
            notificationHelper.notify(applicant, NotificationMessage.APPLICATION_SELECTED_STUDENT,
                    NotificationTarget.MY_APPLICATIONS, null, post.getTitle());
            notificationHelper.notify(post.getAuthor(), NotificationMessage.APPLICATION_PAY_FEE,
                    NotificationTarget.MY_APPLICATIONS, null, applicantName(saved), post.getTitle());
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
        notificationHelper.notify(ApplicationParties.applicantOf(saved),
                NotificationMessage.APPLICATION_REJECTED,
                NotificationTarget.POST, post.getId(), post.getTitle());
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
        cancelExpiry(saved);
        notificationHelper.notify(post.getAuthor(), NotificationMessage.APPLICATION_CONFIRMED,
                NotificationTarget.POST, post.getId(), applicantName(saved), post.getTitle());
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
        post.setStatus(PostStatus.PUBLISHED);
        restoreRejectedApplications(post);

        Application saved = applicationRepository.save(application);
        cancelExpiry(saved);
        notificationHelper.notify(ApplicationParties.applicantOf(saved),
                NotificationMessage.APPLICATION_UNSELECTED,
                NotificationTarget.POST, post.getId(), post.getTitle());
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
        if (application.getApplicationStatus() != ApplicationStatus.CONFIRMED
                && application.getApplicationStatus() != ApplicationStatus.FINISHED) {
            throw new ApplicationInvalidStatusException();
        }
        if (application.getApplicationStatus() == ApplicationStatus.FINISHED) {
            return applicationMapper.toApplicationResponse(application);
        }
        application.setApplicationStatus(ApplicationStatus.FINISHED);
        Application saved = applicationRepository.save(application);
        Post finishedPost = saved.getPost();
        boolean finisherIsAuthor = finishedPost != null && finishedPost.getAuthor() != null
                && finishedPost.getAuthor().getId().equals(userId);
        User other = finisherIsAuthor
                ? ApplicationParties.applicantOf(saved)
                : finishedPost != null ? finishedPost.getAuthor() : null;
        // Người nhận là applicant → xem đơn của tôi; người nhận là chủ bài → xem chi tiết bài
        NotificationTarget finishedTarget = finisherIsAuthor ? NotificationTarget.MY_APPLICATIONS : NotificationTarget.POST;
        UUID finishedTargetId = finisherIsAuthor || finishedPost == null ? null : finishedPost.getId();
        notificationHelper.notify(other, NotificationMessage.APPLICATION_FINISHED,
                finishedTarget, finishedTargetId, finishedPost != null ? finishedPost.getTitle() : "");
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
            post.setStatus(PostStatus.PUBLISHED);
            restoreRejectedApplications(post);
        }

        Application saved = applicationRepository.save(application);
        if (currentStatus == ApplicationStatus.ACCEPTED) {
            cancelExpiry(saved);
        }
        notificationHelper.notify(post.getAuthor(), NotificationMessage.APPLICATION_CANCELLED,
                NotificationTarget.POST, post.getId(), applicantName(saved), post.getTitle());
        return applicationMapper.toApplicationResponse(saved);
    }

    /**
     * Đặt lịch hết hạn W1 (chờ thanh toán) vào Redis sau khi commit.
     * Lỗi Redis không làm fail request — reconciler sẽ vớt lại.
     */
    private void schedulePaymentExpiry(Application application) {
        UUID id = application.getId();
        Instant selectedAt = application.getSelectedAt();
        if (id == null || selectedAt == null) {
            return;
        }
        Instant expireAt = selectedAt.plus(paymentWindowMinutes, ChronoUnit.MINUTES);
        afterCommit.run(() -> {
            try {
                expiryQueue.schedule(ApplicationExpiryQueue.Stage.PAYMENT, id, expireAt);
            } catch (Exception e) {
                log.warn("Failed to schedule payment expiry for application {}", id, e);
            }
        });
    }

    /** Hủy mọi lịch hết hạn của đơn (PAYMENT + CONFIRM) sau khi commit. */
    private void cancelExpiry(Application application) {
        UUID id = application.getId();
        if (id == null) {
            return;
        }
        afterCommit.run(() -> {
            try {
                expiryQueue.cancel(ApplicationExpiryQueue.Stage.PAYMENT, id);
                expiryQueue.cancel(ApplicationExpiryQueue.Stage.CONFIRM, id);
            } catch (Exception e) {
                log.warn("Failed to cancel expiry for application {}", id, e);
            }
        });
    }

    private String applicantName(Application application) {
        User applicant = ApplicationParties.applicantOf(application);
        return applicant != null ? applicant.getFullName() : "";
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
            notificationHelper.notify(ApplicationParties.applicantOf(rejected),
                    NotificationMessage.APPLICATION_REOPENED,
                    NotificationTarget.POST, post.getId(), post.getTitle());
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

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public CursorResponse<ApplicationResponse> getApplicationsByPost(UUID postId, UUID cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Application> applications = applicationRepository.findByPostIdAfterCursor(postId, cursor, pageable);

        return appUtil.buildCursorResponse(
                applications,
                limit,
                Application::getId,
                applicationMapper::toApplicationResponse
        );
    }
}