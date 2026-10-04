/*
 * @ (#) ApplicationServiceImpl,java       1.0    29/09/2026
 *
 * Copyright (c) 2026 IUH. All righta reserved.
 */

package org.group3.tutorlink.features.application.service.impl;

/*
 * @description:
 * @author: Ho Thi Kim Xuyen
 * @version:     1.0
 * @date: 29/09/2026 00
 */
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.exception.ApplicationNotFoundException;
import org.group3.tutorlink.features.application.exception.ApplicationStatusException;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.service.ApplicationService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl
        implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AppUtil appUtil;

    // ============================================================
    // CREATE APPLICATION
    // ============================================================

    @Override
    public ApplicationResponse createApplication(
            CreateApplicationRequest request
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() ->
                        new AppException(
                                ApplicationStatusException.POST_NOT_FOUND
                        )
                );

        // Post phải đang được đăng
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new AppException(
                    ApplicationStatusException.POST_NOT_AVAILABLE
            );
        }

        // Kiểm tra deadline
        if (post.getDeadline() != null
                && Instant.now().isAfter(post.getDeadline())) {

            throw new AppException(
                    ApplicationStatusException.APPLICATION_DEADLINE_EXPIRED
            );
        }

        // Người tạo Post không được tự apply vào Post của mình
        if (post.getAuthor() != null
                && post.getAuthor()
                .getId()
                .equals(currentUser.getId())) {

            throw new AppException(
                    ApplicationStatusException.APPLICATION_NOT_ALLOWED
            );
        }

        Application application = Application.builder()
                .id(appUtil.generateUUID())
                .post(post)
                .applicationStatus(ApplicationStatus.PENDING)
                .message(request.getMessage())
                .appliedAt(LocalDateTime.now())
                .build();

        // ========================================================
        // FIND_TUTOR
        // Student tạo Post
        // Tutor apply
        // ========================================================

        if (post.getType() == PostType.FIND_TUTOR) {

            if (!(currentUser instanceof Tutor tutor)) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }

            boolean exists =
                    applicationRepository.existsByPostIdAndTutorId(
                            post.getId(),
                            tutor.getId()
                    );

            if (exists) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_ALREADY_EXISTS
                );
            }

            application.setTutor(tutor);
        }

        // ========================================================
        // FIND_STUDENT
        // Tutor tạo Post
        // Student apply
        // ========================================================

        else if (post.getType() == PostType.FIND_STUDENT) {

            if (!(currentUser instanceof Student student)) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }

            boolean exists =
                    applicationRepository.existsByPostIdAndStudentId(
                            post.getId(),
                            student.getId()
                    );

            if (exists) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_ALREADY_EXISTS
                );
            }

            application.setStudent(student);
        }

        else {

            throw new AppException(
                    ApplicationStatusException.POST_NOT_AVAILABLE
            );
        }

        Application saved =
                applicationRepository.save(application);

        return toResponse(saved);
    }

    // ============================================================
    // GET APPLICATION BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(
            UUID applicationId
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Application application = getApplication(applicationId);

        if (!canViewApplication(
                currentUser,
                application
        )) {

            throw new AppException(
                    ApplicationStatusException.APPLICATION_NOT_ALLOWED
            );
        }

        return toResponse(application);
    }

    // ============================================================
    // GET MY APPLICATIONS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(
            String role,
            Pageable pageable
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        if (role == null || role.isBlank()) {

            throw new AppException(
                    ApplicationStatusException.INVALID_ROLE
            );
        }

        Page<Application> applications;

        if ("student".equalsIgnoreCase(role)) {

            if (!(currentUser instanceof Student student)) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }

            applications =
                    applicationRepository.findByStudentId(
                            student.getId(),
                            pageable
                    );

        } else if ("tutor".equalsIgnoreCase(role)) {

            if (!(currentUser instanceof Tutor tutor)) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }

            applications =
                    applicationRepository.findByTutorId(
                            tutor.getId(),
                            pageable
                    );

        } else {

            throw new AppException(
                    ApplicationStatusException.INVALID_ROLE
            );
        }

        return applications.map(this::toResponse);
    }

    // ============================================================
    // ADMIN VIEW APPLICATIONS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplications(
            String status,
            Pageable pageable
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        if (!(currentUser instanceof Admin)) {

            throw new AppException(
                    ApplicationStatusException.APPLICATION_NOT_ALLOWED
            );
        }

        Page<Application> applications;

        if (status == null || status.isBlank()) {

            applications =
                    applicationRepository.findAll(pageable);

        } else {

            ApplicationStatus applicationStatus;

            try {

                applicationStatus =
                        ApplicationStatus.valueOf(
                                status.toUpperCase()
                        );

            } catch (IllegalArgumentException e) {

                throw new AppException(
                        ApplicationStatusException.INVALID_STATUS
                );
            }

            applications =
                    applicationRepository.findByApplicationStatus(
                            applicationStatus,
                            pageable
                    );
        }

        return applications.map(this::toResponse);
    }

    // ============================================================
    // SELECT APPLICATION
    // ============================================================

    @Override
    public ApplicationResponse selectApplication(
            UUID applicationId
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Application application =
                getApplication(applicationId);

        Post post = application.getPost();

        // --------------------------------------------------------
        // Chỉ chủ Post mới được chọn Application
        // --------------------------------------------------------

        if (post.getAuthor() == null
                || !post.getAuthor()
                .getId()
                .equals(currentUser.getId())) {

            throw new AppException(
                    ApplicationStatusException.APPLICATION_NOT_ALLOWED
            );
        }

        // --------------------------------------------------------
        // Chỉ PENDING mới được chọn
        // --------------------------------------------------------

        if (application.getApplicationStatus()
                != ApplicationStatus.PENDING) {

            throw new AppException(
                    ApplicationStatusException.INVALID_APPLICATION_STATUS
            );
        }

        // --------------------------------------------------------
        // Kiểm tra Post đang còn mở
        // --------------------------------------------------------

        if (post.getStatus() != PostStatus.PUBLISHED) {

            throw new AppException(
                    ApplicationStatusException.POST_NOT_AVAILABLE
            );
        }

        // --------------------------------------------------------
        // Kiểm tra Post đã có Application ACCEPTED chưa
        // --------------------------------------------------------

        boolean alreadySelected =
                applicationRepository
                        .existsByPostIdAndApplicationStatus(
                                post.getId(),
                                ApplicationStatus.ACCEPTED
                        );

        if (alreadySelected) {

            throw new AppException(
                    ApplicationStatusException.APPLICATION_ALREADY_SELECTED
            );
        }

        // --------------------------------------------------------
        // Gán người tạo Post vào Application
        // --------------------------------------------------------

        if (post.getType() == PostType.FIND_TUTOR) {

            /*
             * Student tạo Post
             * Tutor là applicant
             */
            if (!(currentUser instanceof Student student)) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }

            application.setStudent(student);
        }

        else if (post.getType() == PostType.FIND_STUDENT) {

            /*
             * Tutor tạo Post
             * Student là applicant
             */
            if (!(currentUser instanceof Tutor tutor)) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }

            application.setTutor(tutor);
        }

        // --------------------------------------------------------
        // Student/Tutor chọn applicant
        // PENDING -> ACCEPTED
        // --------------------------------------------------------

        application.setApplicationStatus(
                ApplicationStatus.ACCEPTED
        );

        // --------------------------------------------------------
        // Các Application PENDING khác bị REJECTED
        // --------------------------------------------------------

        Page<Application> pendingApplications =
                applicationRepository
                        .findByPostIdAndApplicationStatus(
                                post.getId(),
                                ApplicationStatus.PENDING,
                                Pageable.unpaged()
                        );

        for (Application other :
                pendingApplications.getContent()) {

            if (!other.getId()
                    .equals(application.getId())) {

                other.setApplicationStatus(
                        ApplicationStatus.REJECTED
                );
            }
        }

        // --------------------------------------------------------
        // Post đóng sau khi đã chọn
        // --------------------------------------------------------

        post.setStatus(PostStatus.CLOSED);

        Application saved =
                applicationRepository.save(application);

        return toResponse(saved);
    }

    // ============================================================
    // CONFIRM APPLICATION
    // ============================================================

    @Override
    public ApplicationResponse confirmApplication(
            UUID applicationId
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Application application =
                getApplication(applicationId);

        Post post = application.getPost();

        // --------------------------------------------------------
        // Chỉ ACCEPTED mới được confirm
        // --------------------------------------------------------

        if (application.getApplicationStatus()
                != ApplicationStatus.ACCEPTED) {

            throw new AppException(
                    ApplicationStatusException.INVALID_APPLICATION_STATUS
            );
        }

        // --------------------------------------------------------
        // FIND_TUTOR
        //
        // Student tạo Post
        // Tutor apply
        // Student chọn Tutor
        // Tutor confirm
        // --------------------------------------------------------

        if (post.getType() == PostType.FIND_TUTOR) {

            if (!(currentUser instanceof Tutor tutor)) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }

            if (application.getTutor() == null
                    || !application.getTutor()
                    .getId()
                    .equals(tutor.getId())) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }
        }

        // --------------------------------------------------------
        // FIND_STUDENT
        //
        // Tutor tạo Post
        // Student apply
        // Tutor chọn Student
        // Student confirm
        // --------------------------------------------------------

        else if (post.getType() == PostType.FIND_STUDENT) {

            if (!(currentUser instanceof Student student)) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }

            if (application.getStudent() == null
                    || !application.getStudent()
                    .getId()
                    .equals(student.getId())) {

                throw new AppException(
                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
                );
            }
        }

        else {

            throw new AppException(
                    ApplicationStatusException.POST_NOT_AVAILABLE
            );
        }

        // --------------------------------------------------------
        // ACCEPTED -> COMPLETED
        // --------------------------------------------------------

        application.setApplicationStatus(
                ApplicationStatus.COMPLETED
        );

        Application saved =
                applicationRepository.save(application);

        return toResponse(saved);
    }

    // ============================================================
    // CANCEL APPLICATION
    // ============================================================

    @Override
    public ApplicationResponse cancelApplication(
            UUID applicationId
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Application application =
                getApplication(applicationId);

        boolean isApplicant = false;

        if (application.getTutor() != null
                && application.getTutor()
                .getId()
                .equals(currentUser.getId())) {

            isApplicant = true;
        }

        if (application.getStudent() != null
                && application.getStudent()
                .getId()
                .equals(currentUser.getId())) {

            isApplicant = true;
        }

        if (!isApplicant) {

            throw new AppException(
                    ApplicationStatusException.APPLICATION_NOT_ALLOWED
            );
        }

        ApplicationStatus currentStatus =
                application.getApplicationStatus();

        if (currentStatus != ApplicationStatus.PENDING
                && currentStatus != ApplicationStatus.ACCEPTED) {

            throw new AppException(
                    ApplicationStatusException.INVALID_APPLICATION_STATUS
            );
        }

        application.setApplicationStatus(
                ApplicationStatus.CANCELLED
        );

        Application saved =
                applicationRepository.save(application);

        return toResponse(saved);
    }

    // ============================================================
    // HELPER
    // ============================================================

    private User getUser(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new AppException(
                                ApplicationStatusException.USER_NOT_FOUND
                        )
                );
    }

    private Application getApplication(
            UUID applicationId
    ) {

        return applicationRepository.findById(applicationId)
                .orElseThrow(
                        ApplicationNotFoundException::new
                );
    }

    private boolean canViewApplication(
            User currentUser,
            Application application
    ) {

        // Admin được xem
        if (currentUser instanceof Admin) {
            return true;
        }

        UUID currentUserId =
                currentUser.getId();

        // Student applicant / owner
        if (application.getStudent() != null
                && application.getStudent()
                .getId()
                .equals(currentUserId)) {

            return true;
        }

        // Tutor applicant / owner
        if (application.getTutor() != null
                && application.getTutor()
                .getId()
                .equals(currentUserId)) {

            return true;
        }

        // Người tạo Post
        if (application.getPost() != null
                && application.getPost().getAuthor() != null
                && application.getPost()
                .getAuthor()
                .getId()
                .equals(currentUserId)) {

            return true;
        }

        return false;
    }

    private ApplicationResponse toResponse(
            Application application
    ) {

        UUID studentId = null;
        UUID tutorId = null;
        UUID adminId = null;
        UUID transactionId = null;

        if (application.getStudent() != null) {

            studentId =
                    application.getStudent().getId();
        }

        if (application.getTutor() != null) {

            tutorId =
                    application.getTutor().getId();
        }

        if (application.getProcessedByAdmin() != null) {

            adminId =
                    application.getProcessedByAdmin().getId();
        }

        if (application.getTransaction() != null) {

            transactionId =
                    application.getTransaction().getId();
        }

        return ApplicationResponse.builder()
                .id(application.getId())

                .postId(
                        application.getPost() != null
                                ? application.getPost().getId()
                                : null
                )

                .studentId(studentId)

                .tutorId(tutorId)

                .applicationStatus(
                        application.getApplicationStatus() != null
                                ? application
                                .getApplicationStatus()
                                .name()
                                : null
                )

                .message(application.getMessage())

                .appliedAt(application.getAppliedAt())

                .processedByAdminId(adminId)

                .transactionId(transactionId)

                .build();
    }
}