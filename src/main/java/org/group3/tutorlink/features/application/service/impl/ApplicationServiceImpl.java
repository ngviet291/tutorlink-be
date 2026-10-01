///*
// * @ (#) ApplicationServiceImpl,java       1.0    29/09/2026
// *
// * Copyright (c) 2026 IUH. All righta reserved.
// */
//
//package org.group3.tutorlink.features.application.service.impl;
//
///*
// * @description:
// * @author: Ho Thi Kim Xuyen
// * @version:     1.0
// * @date: 29/09/2026 00
// */
//import lombok.RequiredArgsConstructor;
//import org.group3.tutorlink.common.exception.AppException;
//import org.group3.tutorlink.common.utils.AppUtil;
//import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
//import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
//import org.group3.tutorlink.features.application.entity.Application;
//import org.group3.tutorlink.features.application.enums.ApplicationStatus;
//import org.group3.tutorlink.features.application.exception.ApplicationNotFoundException;
//import org.group3.tutorlink.features.application.exception.ApplicationStatusException;
//import org.group3.tutorlink.features.application.repository.ApplicationRepository;
//import org.group3.tutorlink.features.application.service.ApplicationService;
//import org.group3.tutorlink.features.post.entity.Post;
//import org.group3.tutorlink.features.post.enums.PostStatus;
//import org.group3.tutorlink.features.post.enums.PostType;
//import org.group3.tutorlink.features.post.repository.PostRepository;
//import org.group3.tutorlink.features.user.entity.Admin;
//import org.group3.tutorlink.features.user.entity.Student;
//import org.group3.tutorlink.features.user.entity.Tutor;
//import org.group3.tutorlink.features.user.entity.User;
//import org.group3.tutorlink.features.user.repository.UserRepository;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class ApplicationServiceImpl implements ApplicationService {
//
//    private final ApplicationRepository applicationRepository;
//    private final PostRepository postRepository;
//    private final UserRepository userRepository;
//    private final AppUtil appUtil;
//
//    @Override
//    public ApplicationResponse createApplication(
//            CreateApplicationRequest request) {
//
//        UUID userId = appUtil.userIdFromAuthentication();
//
//        User currentUser = getUser(userId);
//
//        Post post = postRepository.findById(request.getPostId())
//                .orElseThrow(() ->
//                        new AppException(ApplicationStatusException.POST_NOT_FOUND)
//                );
//
//        // Post phải đang được đăng
//        if (post.getStatus() != PostStatus.PUBLISHED) {
//            throw new AppException(
//                    ApplicationStatusException.POST_NOT_AVAILABLE
//            );
//        }
//
//        // Kiểm tra deadline
//        if (post.getDeadline() != null
//                && Instant.now().isAfter(post.getDeadline())) {
//
//            throw new ApplicationNotFoundException();
//        }
//
//        // Người tạo post không được tự ứng tuyển vào post của mình
//        if (post.getAuthor() != null
//                && post.getAuthor().getId().equals(currentUser.getId())) {
//
//            throw new AppException(
//                    ApplicationStatusException.APPLICATION_NOT_ALLOWED
//            );
//        }
//
//        Application application = Application.builder()
//                .id(appUtil.generateUUID())
//                .post(post)
//                .applicationStatus(ApplicationStatus.PENDING)
//                .message(request.getMessage())
//                .appliedAt(LocalDateTime.now())
//                .build();
//
//        /*
//         * FIND_TUTOR
//         *
//         * Student tạo bài:
//         * "Tôi cần tìm Tutor"
//         *
//         * Tutor là người được phép ứng tuyển.
//         */
//        if (post.getType() == PostType.FIND_TUTOR) {
//
//            if (!(currentUser instanceof Tutor tutor)) {
//
//                throw new AppException(
//                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
//                );
//            }
//
//            boolean exists = applicationRepository
//                    .existsByPostIdAndTutorId(
//                            post.getId(),
//                            tutor.getId()
//                    );
//
//            if (exists) {
//                throw new AppException(
//                        ApplicationStatusException.APPLICATION_ALREADY_EXISTS
//                );
//            }
//
//            application.setTutor(tutor);
//        }
//
//        /*
//         * FIND_STUDENT
//         *
//         * Tutor tạo bài:
//         * "Tôi cần tìm Student"
//         *
//         * Student là người được phép ứng tuyển.
//         */
//        else if (post.getType() == PostType.FIND_STUDENT) {
//
//            if (!(currentUser instanceof Student student)) {
//
//                throw new AppException(
//                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
//                );
//            }
//
//            boolean exists = applicationRepository
//                    .existsByPostIdAndStudentId(
//                            post.getId(),
//                            student.getId()
//                    );
//
//            if (exists) {
//                throw new AppException(
//                        ApplicationStatusException.APPLICATION_ALREADY_EXISTS
//                );
//            }
//
//            application.setStudent(student);
//        }
//
//        else {
//
//            throw new AppException(
//                    ApplicationStatusException.POST_NOT_AVAILABLE
//            );
//        }
//
//        Application saved = applicationRepository.save(application);
//
//        return toResponse(saved);
//    }
//    @Override
//    @Transactional(readOnly = true)
//    public ApplicationResponse getApplicationById(
//            UUID applicationId
//    ) {
//
//        UUID userId = appUtil.userIdFromAuthentication();
//
//        User currentUser = getUser(userId);
//
//        Application application = applicationRepository
//                .findById(applicationId)
//                .orElseThrow(() ->
//                        new AppException(
//                                ApplicationStatusException.APPLICATION_NOT_FOUND
//                        )
//                );
//
//        if (!canViewApplication(currentUser, application)) {
//
//            throw new AppException(
//                    ApplicationStatusException.UNAUTHORIZED
//            );
//        }
//
//        return toResponse(application);
//    }
//
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<ApplicationResponse> getMyApplications(
//            String role,
//            Pageable pageable
//    ) {
//
//        UUID userId = appUtil.userIdFromAuthentication();
//
//        User currentUser = getUser(userId);
//
//        if (role == null || role.isBlank()) {
//            throw new AppException(
//                    ApplicationStatusException.BAD_REQUEST
//            );
//        }
//
//        Page<Application> applications;
//
//        if ("student".equalsIgnoreCase(role)) {
//
//            if (!(currentUser instanceof Student student)) {
//                throw new AppException(
//                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
//                );
//            }
//
//            applications = applicationRepository
//                    .findByStudentId(
//                            student.getId(),
//                            pageable
//                    );
//
//        } else if ("tutor".equalsIgnoreCase(role)) {
//
//            if (!(currentUser instanceof Tutor tutor)) {
//                throw new AppException(
//                        ApplicationStatusException.APPLICATION_NOT_ALLOWED
//                );
//            }
//
//            applications = applicationRepository
//                    .findByTutorId(
//                            tutor.getId(),
//                            pageable
//                    );
//
//        } else {
//
//            throw new AppException(
//                    ApplicationStatusException.BAD_REQUEST
//            );
//        }
//
//        return applications.map(this::toResponse);
//    }
//
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<ApplicationResponse> getApplications(
//            String status,
//            Pageable pageable
//    ) {
//
//        UUID userId = appUtil.userIdFromAuthentication();
//
//        User currentUser = getUser(userId);
//
//        if (!(currentUser instanceof Admin)) {
//            throw new AppException(
//                    ApplicationStatusException.UNAUTHORIZED
//            );
//        }
//
//        Page<Application> applications;
//
//        if (status == null || status.isBlank()) {
//
//            applications = applicationRepository
//                    .findAll(pageable);
//
//        } else {
//
//            ApplicationStatus applicationStatus;
//
//            try {
//
//                applicationStatus =
//                        ApplicationStatus.valueOf(
//                                status.toUpperCase()
//                        );
//
//            } catch (IllegalArgumentException e) {
//
//                throw new AppException(
//                        ApplicationStatusException.BAD_REQUEST
//                );
//            }
//
//            applications = applicationRepository
//                    .findByApplicationStatus(
//                            applicationStatus,
//                            pageable
//                    );
//        }
//
//        return applications.map(this::toResponse);
//    }
//
//
//
//    @Override
//    public ApplicationResponse approveApplication(
//            UUID applicationId
//    ) {
//
//        UUID userId = appUtil.userIdFromAuthentication();
//
//        User currentUser = getUser(userId);
//
//        if (!(currentUser instanceof Admin admin)) {
//
//            throw new AppException(
//                    ApplicationStatusException.UNAUTHORIZED
//            );
//        }
//
//        Application application = getApplication(
//                applicationId
//        );
//
//        if (application.getApplicationStatus()
//                != ApplicationStatus.PENDING) {
//
//            throw new AppException(
//                    ApplicationStatusException.INVALID_APPLICATION_STATUS
//            );
//        }
//
//        application.setApplicationStatus(
//                ApplicationStatus.ACCEPTED
//        );
//
//        application.setProcessedByAdmin(admin);
//
//        Application saved =
//                applicationRepository.save(application);
//
//        return toResponse(saved);
//    }
//
//
//    @Override
//    public ApplicationResponse rejectApplication(
//            UUID applicationId
//    ) {
//
//        UUID userId = appUtil.userIdFromAuthentication();
//
//        User currentUser = getUser(userId);
//
//        Application application = getApplication(
//                applicationId
//        );
//
//        boolean isAdmin =
//                currentUser instanceof Admin;
//
//        boolean isStudentPostOwner =
//                currentUser instanceof Student
//                        && application.getPost()
//                        .getAuthor()
//                        .getId()
//                        .equals(currentUser.getId());
//
//        if (!isAdmin && !isStudentPostOwner) {
//
//            throw new AppException(
//                    ApplicationStatusException.UNAUTHORIZED
//            );
//        }
//
//        if (application.getApplicationStatus()
//                != ApplicationStatus.PENDING) {
//
//            throw new AppException(
//                    ApplicationStatusException.INVALID_APPLICATION_STATUS
//            );
//        }
//
//        application.setApplicationStatus(
//                ApplicationStatus.REJECTED
//        );
//
//        if (isAdmin) {
//            application.setProcessedByAdmin(
//                    (Admin) currentUser
//            );
//        }
//
//        Application saved =
//                applicationRepository.save(application);
//
//        return toResponse(saved);
//    }
//
//
//    @Override
//    public ApplicationResponse confirmApplication(
//            UUID applicationId
//    ) {
//
//        UUID userId = appUtil.userIdFromAuthentication();
//
//        User currentUser = getUser(userId);
//
//        if (!(currentUser instanceof Student student)) {
//
//            throw new AppException(
//                    ApplicationStatusException.UNAUTHORIZED
//            );
//        }
//
//        Application application =
//                getApplication(applicationId);
//
//        Post post = application.getPost();
//
//        // Student phải là người tạo Post
//        if (post.getAuthor() == null
//                || !post.getAuthor()
//                .getId()
//                .equals(student.getId())) {
//
//            throw new AppException(
//                    ApplicationStatusException.UNAUTHORIZED
//            );
//        }
//
//        // Chỉ Application đã được approve mới được confirm
//        if (application.getApplicationStatus()
//                != ApplicationStatus.ACCEPTED) {
//
//            throw new AppException(
//                    ApplicationStatusException.INVALID_APPLICATION_STATUS
//            );
//        }
//
//        /*
//         * ApplicationStatus hiện tại của project chỉ có:
//         *
//         * PENDING
//         * ACCEPTED
//         * REJECTED
//         * CANCELLED
//         *
//         * Vì vậy CONFIRM không tạo status mới.
//         *
//         * Student xác nhận Tutor này:
//         * 1. Giữ application = ACCEPTED
//         * 2. Đóng Post
//         * 3. Các application PENDING khác -> REJECTED
//         */
//
//        post.setStatus(PostStatus.CLOSED);
//
//        Page<Application> otherApplications =
//                applicationRepository.findByApplicationStatus(
//                        ApplicationStatus.PENDING,
//                        Pageable.unpaged()
//                );
//
//        for (Application other : otherApplications.getContent()) {
//
//            if (other.getPost()
//                    .getId()
//                    .equals(post.getId())
//                    && !other.getId()
//                    .equals(application.getId())) {
//
//                other.setApplicationStatus(
//                        ApplicationStatus.REJECTED
//                );
//            }
//        }
//
//        applicationRepository.save(application);
//
//        return toResponse(application);
//    }
//
//
//
//    @Override
//    public ApplicationResponse finishApplication(
//            UUID applicationId
//    ) {
//
//        UUID userId = appUtil.userIdFromAuthentication();
//
//        User currentUser = getUser(userId);
//
//        Application application =
//                getApplication(applicationId);
//
//        boolean isStudent =
//                currentUser instanceof Student;
//
//        boolean isTutor =
//                currentUser instanceof Tutor;
//
//        if (!isStudent && !isTutor) {
//
//            throw new AppException(
//                    ApplicationStatusException.UNAUTHORIZED
//            );
//        }
//
//        boolean isParticipant =
//                (application.getStudent() != null
//                        && application.getStudent()
//                        .getId()
//                        .equals(currentUser.getId()))
//                        ||
//                        (application.getTutor() != null
//                                && application.getTutor()
//                                .getId()
//                                .equals(currentUser.getId()));
//
//        if (!isParticipant) {
//
//            throw new AppException(
//                    ApplicationStatusException.UNAUTHORIZED
//            );
//        }
//
//        if (application.getApplicationStatus()
//                != ApplicationStatus.ACCEPTED) {
//
//            throw new AppException(
//                    ApplicationStatusException.INVALID_APPLICATION_STATUS
//            );
//        }
//
//        return toResponse(application);
//    }
//
//
//
//    private User getUser(UUID userId) {
//
//        return userRepository.findById(userId)
//                .orElseThrow(() ->
//                        new AppException(
//                                ApplicationStatusException.USER_NOT_FOUND
//                        )
//                );
//    }
//
//
//    private Application getApplication(
//            UUID applicationId
//    ) {
//
//        return applicationRepository
//                .findById(applicationId)
//                .orElseThrow(() ->
//                        new AppException(
//                                ApplicationStatusException.APPLICATION_NOT_FOUND
//                        )
//                );
//    }
//
//
//    private boolean canViewApplication(
//            User currentUser,
//            Application application
//    ) {
//
//        // Admin được xem
//        if (currentUser instanceof Admin) {
//            return true;
//        }
//
//        UUID currentUserId =
//                currentUser.getId();
//
//        // Student applicant
//        if (application.getStudent() != null
//                && application.getStudent()
//                .getId()
//                .equals(currentUserId)) {
//
//            return true;
//        }
//
//        // Tutor applicant
//        if (application.getTutor() != null
//                && application.getTutor()
//                .getId()
//                .equals(currentUserId)) {
//
//            return true;
//        }
//
//        // Người tạo Post
//        if (application.getPost() != null
//                && application.getPost()
//                .getAuthor() != null
//                && application.getPost()
//                .getAuthor()
//                .getId()
//                .equals(currentUserId)) {
//
//            return true;
//        }
//
//        return false;
//    }
//
//
//    private ApplicationResponse toResponse(
//            Application application
//    ) {
//
//        UUID studentId = null;
//        UUID tutorId = null;
//        UUID adminId = null;
//        UUID transactionId = null;
//
//        if (application.getStudent() != null) {
//            studentId =
//                    application.getStudent().getId();
//        }
//
//        if (application.getTutor() != null) {
//            tutorId =
//                    application.getTutor().getId();
//        }
//
//        if (application.getProcessedByAdmin() != null) {
//            adminId =
//                    application.getProcessedByAdmin().getId();
//        }
//
//        if (application.getTransaction() != null) {
//            transactionId =
//                    application.getTransaction().getId();
//        }
//
//        return ApplicationResponse.builder()
//                .id(application.getId())
//                .postId(
//                        application.getPost() != null
//                                ? application.getPost().getId()
//                                : null
//                )
//                .studentId(studentId)
//                .tutorId(tutorId)
//                .applicationStatus(
//                        application.getApplicationStatus() != null
//                                ? application.getApplicationStatus().name()
//                                : null
//                )
//                .message(application.getMessage())
//                .appliedAt(application.getAppliedAt())
//                .processedByAdminId(adminId)
//                .transactionId(transactionId)
//                .build();
//    }
//}