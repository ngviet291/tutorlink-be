package org.group3.tutorlink.features.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.dto.request.*;
import org.group3.tutorlink.features.user.dto.response.BaseUserResponse;
import org.group3.tutorlink.features.user.dto.response.TutorResponse;
import org.group3.tutorlink.features.user.entity.AvailableSlot;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.exception.UserErrorCode;
import org.group3.tutorlink.features.user.exception.UserValidationException;
import org.group3.tutorlink.features.user.mapper.UserMapper;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.group3.tutorlink.features.user.specification.UserSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AppUtil appUtil;
    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;

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
        tutor.setVerificationStatus(approved ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
        tutor.setVerificationNote(approved ? null : request.getReason());
        Tutor savedTutor = userRepository.save(tutor);
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
        user.setUserStatus(request.getUserStatus());
        return userMapper.toBaseUserResponse(userRepository.save(user));
    }
    @Transactional(readOnly = true)
    public CursorResponse<TutorResponse> getTutors(
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

        List<Tutor> tutors = tutorRepository.findApprovedTutors(
                VerificationStatus.APPROVED,
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
        if (r.getBio() != null) t.setBio(r.getBio());
        if (r.getExperienceYears() != null) t.setExperienceYears(r.getExperienceYears());
        if (r.getEducation() != null) t.setEducation(r.getEducation());
        if (r.getHourlyRate() != null) t.setHourlyRate(r.getHourlyRate());
        if (r.getTeachingMode() != null) t.setTeachingMode(r.getTeachingMode());
        if (r.getTeachingLevels() != null) t.setTeachingLevels(new HashSet<>(r.getTeachingLevels()));
        if (r.getTeachingAreas() != null) t.setTeachingAreas(new ArrayList<>(r.getTeachingAreas()));
        if (r.getAvailableSlots() != null) {
            List<AvailableSlot> slots = new ArrayList<>();
            for (AvailableSlotRequest s : r.getAvailableSlots()) {
                slots.add(AvailableSlot.builder().dayOfWeek(s.getDayOfWeek())
                        .startTime(s.getStartTime()).endTime(s.getEndTime()).build());
            }
            t.setAvailableSlots(slots);
        }
        if (r.getSubjectIds() != null) {
            List<Subject> subjects = subjectRepository.findAllById(r.getSubjectIds());
            if (subjects.size() != new HashSet<>(r.getSubjectIds()).size())
                throw new UserValidationException(UserErrorCode.USER_VALIDATION_FAILED);
            t.setSubjects(new HashSet<>(subjects));
        }
        return userMapper.toTutorResponse(tutorRepository.save(t));
    }
}