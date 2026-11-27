package org.group3.tutorlink.features.user.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.dto.response.UploadMediaResponse;
import org.group3.tutorlink.common.enums.MediaType;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.service.MediaStorageService;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.application.event.TutorProfileChangedEvent;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.dto.request.*;
import org.group3.tutorlink.features.user.dto.response.BaseUserResponse;
import org.group3.tutorlink.features.user.dto.response.TutorResponse;
import org.group3.tutorlink.features.user.entity.*;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.exception.UserErrorCode;
import org.group3.tutorlink.features.user.exception.UserValidationException;
import org.group3.tutorlink.features.user.mapper.UserMapper;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.group3.tutorlink.features.user.specification.UserSpecification;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AppUtil appUtil;
    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;
    private final MediaStorageService mediaStorageService;

    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private static final String AVATAR_FOLDER = "avatars/";
    private final NotificationHelper notificationHelper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public BaseUserResponse getMyProfile() {
        User user = userRepository.findById(appUtil.userIdFromAuthentication())
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toBaseUserResponse(user);
    }

    @Transactional
    public BaseUserResponse updateMyProfile(UpdateProfileRequest request) {
        User user = userRepository.findById(appUtil.userIdFromAuthentication())
                .orElseThrow(UserNotFoundException::new);
        userMapper.updateUserFromRequest(user, request);
        return userMapper.toBaseUserResponse(userRepository.save(user));
    }

    /**
     * Đổi ảnh đại diện của người dùng hiện tại (mọi role).
     */
    @Transactional
    public BaseUserResponse updateMyAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(UserErrorCode.AVATAR_FILE_REQUIRED);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")
                || file.getSize() > MAX_AVATAR_BYTES) {
            throw new AppException(UserErrorCode.AVATAR_FILE_INVALID);
        }
        User user = userRepository.findById(appUtil.userIdFromAuthentication())
                .orElseThrow(UserNotFoundException::new);
        UploadMediaResponse uploaded = mediaStorageService.uploadSync(
                file, AVATAR_FOLDER + user.getId(), MediaType.IMAGE);
        user.setAvatarUrl(uploaded.getUrl());
        return userMapper.toBaseUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public BaseUserResponse getPublicProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new UserValidationException(
                    UserErrorCode.PUBLIC_PROFILE_NOT_AVAILABLE
            );
        }
        BaseUserResponse response;
        if (user instanceof Tutor tutor) {

            if (tutor.getVerificationStatus() != VerificationStatus.APPROVED) {
                throw new UserValidationException(
                        UserErrorCode.PUBLIC_PROFILE_NOT_AVAILABLE
                );
            }
            response = userMapper.toTutorResponse(tutor);
        } else if (user instanceof Student student) {
            response = userMapper.toStudentResponse(student);
        } else {
            throw new UserValidationException(
                    UserErrorCode.PUBLIC_PROFILE_NOT_AVAILABLE
            );
        }
        response.setEmail(null);
        response.setPhone(null);
        if (response.getAddress() != null) {
            response.getAddress().setStreet(null); // chỉ để lộ phường/xã, tỉnh
        }
        if (response instanceof TutorResponse tutorResponse) {
            tutorResponse.setVerificationNote(null);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public BaseUserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toBaseUserResponse(user);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public CursorResponse<BaseUserResponse> getAllUsers(
            RoleName role,
            UserStatus status,
            VerificationStatus verificationStatus,
            UUID cursor,
            int limit
    ) {
        Specification<User> spec = Specification
                .where(UserSpecification.hasRole(role))
                .and(UserSpecification.hasStatus(status))
                .and(UserSpecification.hasVerificationStatus(verificationStatus))
                .and(UserSpecification.cursor(cursor))
                .and(UserSpecification.fetchRole());

        Pageable pageable = PageRequest.of(
                0,
                limit + 1,
                Sort.by(Sort.Order.desc("id"))
        );

        List<User> users = userRepository.findAll(spec, pageable).getContent();

        return appUtil.buildCursorResponse(
                users,
                limit,
                User::getId,
                userMapper::toBaseUserResponse
        );
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public TutorResponse approveTutor(UUID userId, ApproveTutorRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (!(user instanceof Tutor tutor)) {
            throw new UserValidationException(UserErrorCode.USER_IS_NOT_TUTOR);
        }
        boolean approved = request.getApproved();
        if (approved) {
            // Không có chứng chỉ thì không có gì để duyệt
            if (tutor.getCertificates() == null || tutor.getCertificates().isEmpty()) {
                throw new UserValidationException(UserErrorCode.USER_VALIDATION_FAILED);
            }
            // Admin duyệt tay = xác thực các chứng chỉ, nếu không gia sư không đăng bài được
            tutor.getCertificates().forEach(c -> {
                c.setVerified(true);
                c.setPendingAdminReview(false);
                c.setReviewNote(null);
            });
        }
        tutor.setVerificationStatus(approved ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
        tutor.setVerificationNote(approved ? null : request.getReason());
        Tutor savedTutor = userRepository.save(tutor);
        eventPublisher.publishEvent(TutorProfileChangedEvent.builder().tutorId(savedTutor.getId()).build());
        if (approved) {
            notificationHelper.notify(savedTutor, NotificationMessage.TUTOR_APPROVED,
                    NotificationTarget.TUTOR_PROFILE, null);
        } else if (request.getReason() == null || request.getReason().isBlank()) {
            notificationHelper.notify(savedTutor, NotificationMessage.TUTOR_REJECTED_NO_REASON,
                    NotificationTarget.TUTOR_PROFILE, null);
        } else {
            notificationHelper.notify(savedTutor, NotificationMessage.TUTOR_REJECTED,
                    NotificationTarget.TUTOR_PROFILE, null, request.getReason().trim());
        }
        TutorResponse response = userMapper.toTutorResponse(savedTutor);
        return response;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public BaseUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        boolean isAdmin = user.getRole() != null
                && RoleName.ADMIN == user.getRole().getName();
        if (isAdmin && request.getUserStatus() == UserStatus.BANNED) {
            throw new UserValidationException(UserErrorCode.CANNOT_LOCK_ADMIN);
        }
        UserStatus oldStatus = user.getUserStatus();
        user.setUserStatus(request.getUserStatus());
        User saved = userRepository.save(user);
        if (oldStatus == UserStatus.BANNED && saved.getUserStatus() == UserStatus.ACTIVE) {
            notificationHelper.notify(saved, NotificationMessage.ACCOUNT_UNLOCKED,
                    NotificationTarget.NONE, null);
        }
        return userMapper.toBaseUserResponse(saved);
    }
    @Transactional(readOnly = true)
    public CursorResponse<TutorResponse> getTutors(
            String name,
            String subjectName,
            String province,
            Double minRate,
            Double maxRate,
            Double minRating,
            Integer minExperience,
            UUID cursor,
            int limit
    ) {
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by(Sort.Order.desc("id")));
        String nameKeyword = (name == null || name.isBlank()) ? null : name.trim();
        List<Tutor> tutors = tutorRepository.findApprovedTutors(
                VerificationStatus.APPROVED,
                nameKeyword,
                subjectName,
                province,
                minRate,
                maxRate,
                minRating,
                minExperience,
                cursor,
                pageable
        );

        return appUtil.buildCursorResponse(
                tutors,
                limit,
                Tutor::getId,
                userMapper::toTutorResponse
        );
    }

    @PreAuthorize("hasAuthority('TUTOR')")
    @Transactional(readOnly = true)
    public TutorResponse getMyTutorProfile() {
        Tutor tutor = tutorRepository.findById(appUtil.userIdFromAuthentication())
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toTutorResponse(tutor);
    }

    @PreAuthorize("hasAuthority('TUTOR')")
    @Transactional
    public TutorResponse updateMyTutorProfile(UpdateTutorProfileRequest r) {
        Tutor t = tutorRepository.findById(appUtil.userIdFromAuthentication())
                .orElseThrow(UserNotFoundException::new);

        userMapper.updateTutorBasic(r, t);

        // Khi hồ sơ đã có dữ liệu theo môn thì giá trị chung do syncAggregates tính, không nhận từ request
        boolean perSubject = r.getSubjectProfiles() != null
                || (t.getSubjectProfiles() != null && !t.getSubjectProfiles().isEmpty());
        if (!perSubject) {
            userMapper.updateTutorGeneral(r, t);
        }

        if (r.getAvailableSlots() != null) {
            validateSlots(r.getAvailableSlots());
            t.setAvailableSlots(r.getAvailableSlots().stream()
                    .map(userMapper::toAvailableSlot)
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        if (r.getSubjectProfiles() != null) {
            applySubjectProfiles(t, r.getSubjectProfiles());
        }

        Tutor saved = tutorRepository.save(t);
        eventPublisher.publishEvent(TutorProfileChangedEvent.builder().tutorId(saved.getId()).build());
        return userMapper.toTutorResponse(saved);
    }

    private void validateSlots(List<AvailableSlotRequest> slots) {
        Set<List<Object>> seen = new HashSet<>();
        for (AvailableSlotRequest s : slots) {
            if (!s.getEndTime().isAfter(s.getStartTime())) {
                throw new UserValidationException(UserErrorCode.USER_VALIDATION_FAILED);
            }
            // Khóa so sánh: ngày + giờ bắt đầu + giờ kết thúc (bỏ qua ID)
            if (!seen.add(List.of(s.getDayOfWeek(), s.getStartTime(), s.getEndTime()))) {
                throw new UserValidationException(UserErrorCode.AVAILABLE_SLOT_DUPLICATED);
            }
        }
    }

    /**
     * Danh sách đầy đủ các môn: thêm/sửa theo request, môn bị bỏ thì xóa cùng chứng chỉ của môn đó.
     */
    private void applySubjectProfiles(Tutor t, List<SubjectProfileRequest> requests) {
        Set<UUID> ids = new HashSet<>();
        for (SubjectProfileRequest p : requests) {
            if (!ids.add(p.getSubjectId())) {
                throw new UserValidationException(UserErrorCode.USER_VALIDATION_FAILED);
            }
        }
        Map<UUID, Subject> subjects = subjectRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        if (subjects.size() != ids.size()) {
            throw new UserValidationException(UserErrorCode.USER_VALIDATION_FAILED);
        }

        if (t.getSubjectProfiles() == null) t.setSubjectProfiles(new HashSet<>());
        if (t.getSubjects() == null) t.setSubjects(new HashSet<>());

        // Môn bị bỏ: xóa dữ liệu môn, môn và chứng chỉ của môn đó
        t.getSubjectProfiles().removeIf(p -> !ids.contains(p.getSubject().getId()));
        t.getSubjects().removeIf(s -> !ids.contains(s.getId()));
        List<String> removedFiles = new ArrayList<>();
        if (t.getCertificates() != null) {
            t.getCertificates().removeIf(c -> {
                boolean remove = c.getSubject() != null && !ids.contains(c.getSubject().getId());
                if (remove && c.getCloudinaryPublicId() != null) {
                    removedFiles.add(c.getCloudinaryPublicId());
                }
                return remove;
            });
        }

        Map<UUID, TutorSubjectProfile> existing = t.getSubjectProfiles().stream()
                .collect(Collectors.toMap(p -> p.getSubject().getId(), p -> p));
        for (SubjectProfileRequest req : requests) {
            Subject subject = subjects.get(req.getSubjectId());
            TutorSubjectProfile p = existing.get(subject.getId());
            if (p == null) {
                p = new TutorSubjectProfile();
                p.setId(appUtil.generateUUID());
                p.setTutor(t);
                p.setSubject(subject);
                t.getSubjectProfiles().add(p);
            }
            p.setExperienceYears(req.getExperienceYears() != null ? req.getExperienceYears() : 0);
            p.setHourlyRate(req.getHourlyRate());
            p.setTeachingMode(req.getTeachingMode());
            t.getSubjects().add(subject);
        }
        syncAggregates(t);
        if (!removedFiles.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    removedFiles.forEach(mediaStorageService::delete);
                }
            });
        }
    }

    /**
     * Giá trị tổng hợp trên Tutor (dùng cho tìm kiếm, chatbot...): kinh nghiệm cao nhất, giá thấp nhất, hình thức BOTH nếu khác nhau.
     */
    private void syncAggregates(Tutor t) {
        List<TutorSubjectProfile> profiles = new ArrayList<>(t.getSubjectProfiles());
        if (profiles.isEmpty()) {
            t.setExperienceYears(0);
            t.setHourlyRate(null);
            t.setTeachingMode(null);
            return;
        }
        t.setExperienceYears(profiles.stream()
                .mapToInt(TutorSubjectProfile::getExperienceYears).max().orElse(0));
        profiles.stream().map(TutorSubjectProfile::getHourlyRate).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).ifPresent(t::setHourlyRate);

        Set<TeachingMode> modes = profiles.stream().map(TutorSubjectProfile::getTeachingMode)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (modes.size() == 1) {
            t.setTeachingMode(modes.iterator().next());
        } else if (modes.size() > 1) {
            t.setTeachingMode(TeachingMode.BOTH);
        }
    }

    public CursorResponse<TutorResponse> getTopTutors(int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Tutor> topTutors = tutorRepository.findTopTutorN(pageable);
        return appUtil.buildCursorResponse(
                topTutors,
                size,
                Tutor::getId,
                userMapper::toTutorResponse
        );
    }
}