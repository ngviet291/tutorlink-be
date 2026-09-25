package org.group3.tutorlink.features.auth.service;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.ErrorCode;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.dto.request.*;
import org.group3.tutorlink.features.auth.dto.response.AuthenticateResponse;
import org.group3.tutorlink.features.auth.dto.response.IntrospectResponse;
import org.group3.tutorlink.features.auth.dto.response.UserResponse;
import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.auth.exception.TokenExpiredException;
import org.group3.tutorlink.features.auth.exception.UnauthenticatedException;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.auth.repository.RoleRepository;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.repository.StudentRepository;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

@RequiredArgsConstructor
@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TutorRepository tutorRepository;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlackListService tokenBlacklistService;
    private final TokenBlackListService tokenBlackListService;

    @Transactional
    public AuthenticateResponse registerStudent(RegisterStudentRequest req) {
        assertEmailNotTaken(req.getEmail());

        Role role = getRoleOrThrow("ROLE_STUDENT");

        Student student = Student.builder()
                .id(AppUtil.generateUUID())
                .fullname(req.getFullname())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .address(req.getAddress())
                .userStatus(UserStatus.ACTIVE)
                .role(role)
                .grade(req.getGrade())
                .learningGoal(req.getLearningGoal())
                .build();

        studentRepository.save(student);

        log.info("Registered new student account: {}", student.getEmail());

        return buildAuthResponse(student);
    }

    @Transactional
    public AuthenticateResponse registerTutor(RegisterTutorRequest req) {
        assertEmailNotTaken(req.getEmail());

        Role role = getRoleOrThrow("ROLE_TUTOR");

        Subject subject = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        Tutor tutor = Tutor.builder()
                .id(AppUtil.generateUUID())
                .fullname(req.getFullname())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .address(req.getAddress())
                .userStatus(UserStatus.ACTIVE)
                .role(role)
                .experienceYears(req.getExperienceYears())
                .education(req.getEducation())
                .subject(subject)
                .averageRating(0.0)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        tutorRepository.save(tutor);

        log.info("Registered new tutor account: {}", tutor.getEmail());

        return buildAuthResponse(tutor);
    }

    @Transactional(readOnly = true)
    public AuthenticateResponse authenticate(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getUserStatus() == UserStatus.BANNED) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenService.saveRefreshToken(refreshToken, user.getEmail());

        log.info("User {} logged in", user.getEmail());

        return buildAuthResponse(user, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthenticateResponse refreshToken(RefreshTokenRequest refreshToken) {

        String refreshTokenStr = refreshToken.getRefreshToken();

        if (!refreshTokenService.isRefreshTokenExists(refreshTokenStr)) {
            throw new TokenExpiredException();
        }

        String username = refreshTokenService.getUser(refreshTokenStr);

        User user = userRepository.findByEmail(username)
                .orElseThrow(UserNotFoundException::new);

        String newRefreshToken = jwtService.generateRefreshToken(user);

        refreshTokenService.deleteRefreshToken(refreshTokenStr);
        refreshTokenService.saveRefreshToken(newRefreshToken, username);

        return buildAuthResponse(user, newRefreshToken);

    }


    public void logout(LogoutRequest request) {
        String token = request.getAccessToken();
        String refreshToken = request.getRefreshToken();
        SignedJWT signedJWT = jwtService.verifyToken(token);

        try {
            String jitToken = signedJWT.getJWTClaimsSet().getJWTID();
            long expirationTime = getSecondsUntilExpiration(signedJWT.getJWTClaimsSet().getExpirationTime());

            // store blacklist with time to live equals to the remaining time of the token
            tokenBlackListService.blacklistToken(jitToken, expirationTime);

            refreshTokenService.deleteRefreshToken(refreshToken);
        } catch (ParseException e) {
            throw new UnauthenticatedException();
        }

    }
    private long getSecondsUntilExpiration(Date expirationDate) {
        long expirationEpoch = expirationDate.toInstant().getEpochSecond();
        long nowEpoch = Instant.now().getEpochSecond();
        return expirationEpoch - nowEpoch;
    }

    private void assertEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AppException(
                    ErrorCode.EMAIL_ALREADY_EXISTS
            );
        }
    }

    private Role getRoleOrThrow(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.ROLE_NOT_FOUND
                        )
                );
    }

    private AuthenticateResponse buildAuthResponse(User user, String refreshToken) {
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullname(user.getFullname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .userStatus(user.getUserStatus())
                .role(user.getRole().getName())
                .build();

        return AuthenticateResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpiration())
                .user(userResponse)
                .build();
    }

    private AuthenticateResponse buildAuthResponse(User user) {
        String refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.saveRefreshToken(
                refreshToken,
                user.getEmail()
        );
        return buildAuthResponse(
                user,
                refreshToken
        );
    }

    /**
     * TODO: KHI NÀO LÀM BAN THÌ MỞ LÀM Ở CHỖ COMMENT
     *
     */
    public IntrospectResponse introspect(IntrospectRequest request) {
        IntrospectResponse introspectResponse = jwtService.introspect(request.getAccessToken());

//        if (introspectResponse.isActive() && introspectResponse.getUserId() != null) {
//            banService.findActiveBanForUser(UUID.fromString(introspectResponse.getUserId()))
//                    .ifPresent(ban -> {
//                        throw new AccountBanException(ban.getEndDate());
//                    });
//        }
        return introspectResponse;
    }



    @PreAuthorize("hasRole('ROLE_STUDENT') or hasRole('ROLE_TUTOR') or hasRole('ROLE_ADMIN')")
    public String testAccessDenied() {
        return "DATA";
    }
}