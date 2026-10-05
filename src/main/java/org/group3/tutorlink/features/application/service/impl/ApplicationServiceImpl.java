package org.group3.tutorlink.features.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.exception.ApplicationNotFoundException;
import org.group3.tutorlink.features.application.exception.ErrorCodeApplication;
import org.group3.tutorlink.features.application.mapper.ApplicationMapper;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.service.ApplicationService;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AppUtil appUtil;
    // Đã sửa
    private final ApplicationMapper applicationMapper;

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse createApplication(CreateApplicationRequest request) {

        UUID userId = appUtil.userIdFromAuthentication();
        User currentUser = getUserOrThrow(userId);

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(ErrorCodeApplication.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }

        if (post.getDeadline() != null && Instant.now().isAfter(post.getDeadline())) {
            throw new AppException(ErrorCodeApplication.APPLICATION_DEADLINE_EXPIRED);
        }

        if (post.getAuthor() != null
                && post.getAuthor().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
        }

        Application application = Application.builder()
                .id(appUtil.generateUUID())
                .post(post)
                .applicationStatus(ApplicationStatus.PENDING)
                .message(request.getMessage())
                .appliedAt(Instant.now())
                .build();

        if (post.getType() == PostType.FIND_TUTOR) {

            if (!(currentUser instanceof Tutor tutor)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }

            if (applicationRepository.existsByPostIdAndTutorId(post.getId(), tutor.getId())) {
                throw new AppException(ErrorCodeApplication.APPLICATION_ALREADY_EXISTS);
            }

            application.setTutor(tutor);

        } else if (post.getType() == PostType.FIND_STUDENT) {

            if (!(currentUser instanceof Student student)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }

            if (applicationRepository.existsByPostIdAndStudentId(post.getId(), student.getId())) {
                throw new AppException(ErrorCodeApplication.APPLICATION_ALREADY_EXISTS);
            }

            application.setStudent(student);

        } else {
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }

        Application saved = applicationRepository.save(application);
        // Đã sửa
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STUDENT', 'TUTOR')")
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
                            userId,
                            ApplicationStatus.PENDING,
                            PostStatus.PUBLISHED
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

        if (limit < 0 || limit > 100) {
            throw new AppException(ErrorCodeApplication.INVALID_LIMIT);
        }

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
        if (limit < 0 || limit > 100) {
            throw new AppException(ErrorCodeApplication.INVALID_LIMIT);
        }

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
        Application application = applicationRepository.findByIdForSelection(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);
        Post post = application.getPost();

        if (post == null
                || post.getAuthor() == null
                || !post.getAuthor().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
        }

        if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
            throw new AppException(ErrorCodeApplication.INVALID_APPLICATION_STATUS);
        }

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }

        boolean alreadySelected = applicationRepository
                .existsByPostIdAndApplicationStatus(post.getId(), ApplicationStatus.ACCEPTED);

        if (alreadySelected) {
            throw new AppException(ErrorCodeApplication.APPLICATION_ALREADY_SELECTED);
        }

        validateSelectorRole(post, currentUser);

        application.setApplicationStatus(ApplicationStatus.ACCEPTED);

        Page<Application> pendingApplications = applicationRepository
                .findByPostIdAndApplicationStatus(
                        post.getId(),
                        ApplicationStatus.PENDING,
                        Pageable.unpaged()
                );

        for (Application other : pendingApplications.getContent()) {
            if (!other.getId().equals(application.getId())) {
                other.setApplicationStatus(ApplicationStatus.REJECTED);
            }
        }

        post.setStatus(PostStatus.CLOSED);

        Application saved = applicationRepository.save(application);
        // Đã sửa
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse confirmApplication(UUID applicationId) {

        UUID userId = appUtil.userIdFromAuthentication();
        User currentUser = getUserOrThrow(userId);
        Application application = getApplicationOrThrow(applicationId);
        Post post = application.getPost();

        if (application.getApplicationStatus() != ApplicationStatus.ACCEPTED) {
            throw new AppException(ErrorCodeApplication.INVALID_APPLICATION_STATUS);
        }

        // Chỉ người đã apply được xác nhận
        if (post.getType() == PostType.FIND_TUTOR) {

            if (!(currentUser instanceof Tutor tutor)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }
            if (application.getTutor() == null
                    || !application.getTutor().getId().equals(tutor.getId())) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }

        } else if (post.getType() == PostType.FIND_STUDENT) {

            if (!(currentUser instanceof Student student)) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }
            if (application.getStudent() == null
                    || !application.getStudent().getId().equals(student.getId())) {
                throw new AppException(ErrorCodeApplication.APPLICATION_NOT_ALLOWED);
            }

        } else {
            throw new AppException(ErrorCodeApplication.POST_NOT_AVAILABLE);
        }

        application.setApplicationStatus(ApplicationStatus.FINISHED);

        Application saved = applicationRepository.save(application);
        // Đã sửa
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    public ApplicationResponse cancelApplication(UUID applicationId) {

        UUID userId = appUtil.userIdFromAuthentication();
        getUserOrThrow(userId);
        Application application = applicationRepository
                .findByIdAndApplicantId(applicationId, userId)
                .orElseThrow(() -> new AppException(
                        ErrorCodeApplication.APPLICATION_NOT_ALLOWED
                ));

        ApplicationStatus currentStatus = application.getApplicationStatus();

        if (currentStatus != ApplicationStatus.PENDING
                && currentStatus != ApplicationStatus.ACCEPTED) {
            throw new AppException(ErrorCodeApplication.INVALID_APPLICATION_STATUS);
        }

        application.setApplicationStatus(ApplicationStatus.CANCELLED);

        Application saved = applicationRepository.save(application);
        // Đã sửa
        return applicationMapper.toApplicationResponse(saved);
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