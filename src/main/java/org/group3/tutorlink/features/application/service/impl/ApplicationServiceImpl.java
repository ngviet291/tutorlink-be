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
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.exception.ApplicationNotAllowedException;
import org.group3.tutorlink.features.application.exception.ApplicationNotFoundException;
import org.group3.tutorlink.features.application.exception.ErrorCodeApplication;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AppUtil appUtil;


    @Override
    public ApplicationResponse createApplication(
            CreateApplicationRequest request
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() ->
                        new AppException(
                                ErrorCodeApplication.POST_NOT_FOUND
                        )
                );


        if (post.getStatus() != PostStatus.PUBLISHED) {

            throw new AppException(
                    ErrorCodeApplication.POST_NOT_AVAILABLE
            );
        }


        if (post.getDeadline() != null
                && Instant.now().isAfter(post.getDeadline())) {

            throw new AppException(
                    ErrorCodeApplication.APPLICATION_DEADLINE_EXPIRED
            );
        }



        if (post.getAuthor() != null
                && post.getAuthor()
                .getId()
                .equals(currentUser.getId())) {

            throw new ApplicationNotAllowedException();
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

                throw new ApplicationNotAllowedException();
            }

            boolean exists =
                    applicationRepository.existsByPostIdAndTutorId(
                            post.getId(),
                            tutor.getId()
                    );

            if (exists) {

                throw new AppException(
                        ErrorCodeApplication.APPLICATION_ALREADY_EXISTS
                );
            }

            application.setTutor(tutor);
        }



        else if (post.getType() == PostType.FIND_STUDENT) {

            if (!(currentUser instanceof Student student)) {

                throw new ApplicationNotAllowedException();
            }

            boolean exists =
                    applicationRepository.existsByPostIdAndStudentId(
                            post.getId(),
                            student.getId()
                    );

            if (exists) {

                throw new AppException(
                        ErrorCodeApplication.APPLICATION_ALREADY_EXISTS
                );
            }

            application.setStudent(student);
        }



        else {

            throw new AppException(
                    ErrorCodeApplication.POST_NOT_AVAILABLE
            );
        }

        Application saved =
                applicationRepository.save(application);

        return toResponse(saved);
    }



    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(
            UUID applicationId
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Application application =
                getApplication(applicationId);

        if (!canViewApplication(
                currentUser,
                application
        )) {

            throw new ApplicationNotAllowedException();
        }

        return toResponse(application);
    }


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
                    ErrorCodeApplication.INVALID_ROLE
            );
        }

        Page<Application> applications;


        if ("student".equalsIgnoreCase(role)) {

            if (!(currentUser instanceof Student student)) {

                throw new ApplicationNotAllowedException();
            }

            applications =
                    applicationRepository.findByStudentId(
                            student.getId(),
                            pageable
                    );
        }



        else if ("tutor".equalsIgnoreCase(role)) {

            if (!(currentUser instanceof Tutor tutor)) {

                throw new ApplicationNotAllowedException();
            }

            applications =
                    applicationRepository.findByTutorId(
                            tutor.getId(),
                            pageable
                    );
        }


        else {

            throw new AppException(
                    ErrorCodeApplication.INVALID_ROLE
            );
        }

        return applications.map(this::toResponse);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplications(
            String status,
            Pageable pageable
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);


        if (!(currentUser instanceof Admin)) {

            throw new ApplicationNotAllowedException();
        }

        Page<Application> applications;


        if (status == null || status.isBlank()) {

            applications =
                    applicationRepository.findAll(pageable);
        }



        else {

            ApplicationStatus applicationStatus;

            try {

                applicationStatus =
                        ApplicationStatus.valueOf(
                                status.toUpperCase()
                        );

            } catch (IllegalArgumentException e) {

                throw new AppException(
                        ErrorCodeApplication.INVALID_STATUS
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



    @Override
    public ApplicationResponse selectApplication(
            UUID applicationId
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Application application =
                getApplication(applicationId);

        Post post = application.getPost();



        if (post == null
                || post.getAuthor() == null
                || !post.getAuthor()
                .getId()
                .equals(currentUser.getId())) {

            throw new ApplicationNotAllowedException();
        }



        if (application.getApplicationStatus()
                != ApplicationStatus.PENDING) {

            throw new AppException(
                    ErrorCodeApplication.INVALID_APPLICATION_STATUS
            );
        }


        if (post.getStatus() != PostStatus.PUBLISHED) {

            throw new AppException(
                    ErrorCodeApplication.POST_NOT_AVAILABLE
            );
        }



        boolean alreadySelected =
                applicationRepository
                        .existsByPostIdAndApplicationStatus(
                                post.getId(),
                                ApplicationStatus.ACCEPTED
                        );

        if (alreadySelected) {

            throw new AppException(
                    ErrorCodeApplication.APPLICATION_ALREADY_SELECTED
            );
        }



        if (post.getType() == PostType.FIND_TUTOR) {

            if (!(currentUser instanceof Student)) {

                throw new ApplicationNotAllowedException();
            }

        } else if (post.getType() == PostType.FIND_STUDENT) {

            if (!(currentUser instanceof Tutor)) {

                throw new ApplicationNotAllowedException();
            }

        } else {

            throw new AppException(
                    ErrorCodeApplication.POST_NOT_AVAILABLE
            );
        }



        application.setApplicationStatus(
                ApplicationStatus.ACCEPTED
        );



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


        post.setStatus(PostStatus.CLOSED);

        Application saved =
                applicationRepository.save(application);

        return toResponse(saved);
    }



    @Override
    public ApplicationResponse confirmApplication(
            UUID applicationId
    ) {

        UUID userId = appUtil.userIdFromAuthentication();

        User currentUser = getUser(userId);

        Application application =
                getApplication(applicationId);

        Post post = application.getPost();


        if (application.getApplicationStatus()
                != ApplicationStatus.ACCEPTED) {

            throw new AppException(
                    ErrorCodeApplication.INVALID_APPLICATION_STATUS
            );
        }



        if (post.getType() == PostType.FIND_TUTOR) {

            if (!(currentUser instanceof Tutor tutor)) {

                throw new ApplicationNotAllowedException();
            }

            if (application.getTutor() == null
                    || !application.getTutor()
                    .getId()
                    .equals(tutor.getId())) {

                throw new ApplicationNotAllowedException();
            }
        }



        else if (post.getType() == PostType.FIND_STUDENT) {

            if (!(currentUser instanceof Student student)) {

                throw new ApplicationNotAllowedException();
            }

            if (application.getStudent() == null
                    || !application.getStudent()
                    .getId()
                    .equals(student.getId())) {

                throw new ApplicationNotAllowedException();
            }
        }


        else {

            throw new AppException(
                    ErrorCodeApplication.POST_NOT_AVAILABLE
            );
        }


        application.setApplicationStatus(
                ApplicationStatus.FINISHED
        );

        Application saved =
                applicationRepository.save(application);

        return toResponse(saved);
    }


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

            throw new ApplicationNotAllowedException();
        }

        ApplicationStatus currentStatus =
                application.getApplicationStatus();


        if (currentStatus != ApplicationStatus.PENDING
                && currentStatus != ApplicationStatus.ACCEPTED) {

            throw new AppException(
                    ErrorCodeApplication.INVALID_APPLICATION_STATUS
            );
        }


        application.setApplicationStatus(
                ApplicationStatus.CANCELLED
        );

        Application saved =
                applicationRepository.save(application);

        return toResponse(saved);
    }


    private User getUser(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCodeApplication.USER_NOT_FOUND
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


        if (currentUser instanceof Admin) {
            return true;
        }

        UUID currentUserId =
                currentUser.getId();



        if (application.getStudent() != null
                && application.getStudent()
                .getId()
                .equals(currentUserId)) {

            return true;
        }

        if (application.getTutor() != null
                && application.getTutor()
                .getId()
                .equals(currentUserId)) {

            return true;
        }


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
