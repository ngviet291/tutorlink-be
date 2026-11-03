package org.group3.tutorlink.features.auth.service;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.UploadMediaResponse;
import org.group3.tutorlink.common.enums.MediaType;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.ErrorCode;
import org.group3.tutorlink.common.logging.SystemLogPublisher;
import org.group3.tutorlink.common.service.MediaStorageService;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.dto.request.*;
import org.group3.tutorlink.features.auth.dto.response.AuthenticateResponse;
import org.group3.tutorlink.features.auth.dto.response.IntrospectResponse;
import org.group3.tutorlink.features.auth.event.TutorCertificatesSubmittedEvent;
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.mapper.CertificateMapper;
import org.group3.tutorlink.features.subject.mapper.SubjectMapper;
import org.group3.tutorlink.features.tracking.enums.SystemAction;
import org.group3.tutorlink.features.user.dto.response.BaseUserResponse;
import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.auth.exception.TokenExpiredException;
import org.group3.tutorlink.features.auth.exception.UnauthenticatedException;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.auth.repository.RoleRepository;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.dto.response.TutorResponse;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.mapper.UserMapper;
import org.group3.tutorlink.features.user.repository.StudentRepository;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class AuthenticationService {

    public static final String TUTOR_CERTIFICATES = "tutor-certificates/";
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TutorRepository tutorRepository;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlackListService tokenBlackListService;
    private final AppUtil appUtil;
    private final UserMapper userMapper;
    private final SubjectMapper subjectMapper;
    private final ChangePasswordService changePasswordService;
    private final PasswordGenerator passwordGenerator;
    private final EmailService emailService;
    private final SystemLogPublisher systemLogPublisher;
    private final MediaStorageService mediaStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final CertificateMapper certificateMapper;

    @Transactional
    public AuthenticateResponse registerStudent(RegisterStudentRequest req) {
        assertEmailNotTaken(req.getEmail());

        Role role = getRoleOrThrow(RoleName.STUDENT);

        Student student = userMapper.toStudent(req);
        student.setId(appUtil.generateUUID());
        student.setPassword(passwordEncoder.encode(req.getPassword()));
        student.setRole(role);
        student.setUserStatus(UserStatus.ACTIVE);

        studentRepository.save(student);
        systemLogPublisher.success(student.getId(), SystemAction.REGISTER,
                User.class.getSimpleName(), student.getId(),
                Map.of("role", RoleName.STUDENT.name()));
        log.info("Registered new student account: {}", student.getEmail());

        return buildAuthResponse(student);
    }

    @Transactional
    public AuthenticateResponse registerTutor(RegisterTutorRequest req, List<MultipartFile> certificates) {
        assertEmailNotTaken(req.getEmail());

        Role role = getRoleOrThrow(RoleName.TUTOR);

        List<Subject> subjects = subjectRepository.findAllById(req.getSubjectIds());
        if (subjects.size() != req.getSubjectIds().size()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        log.info("Certificates count: {}", certificates == null ? 0 : certificates.size());
        Tutor tutor = userMapper.toTutor(req);
        tutor.setId(appUtil.generateUUID());
        tutor.setPassword(passwordEncoder.encode(req.getPassword()));
        tutor.setRole(role);
        tutor.setUserStatus(UserStatus.ACTIVE);
        tutor.setSubjects(Set.copyOf(subjects));
        tutor.setAverageRating(0.0);
        tutor.setVerificationStatus(VerificationStatus.PENDING);
        List<MultipartFile> files = Optional.ofNullable(certificates).orElse(List.of())
                .stream().filter(f -> !f.isEmpty()).toList();

        if (!files.isEmpty()) {

            List<UploadMediaResponse> uploaded =
                    mediaStorageService.uploads(files, TUTOR_CERTIFICATES + tutor.getId(), MediaType.IMAGE);
            log.info("Uploaded {} certificates for tutor {}", uploaded.size(), tutor.getId());
            // Rollback DB thì xóa ảnh đã lên Cloudinary
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK)
                        uploaded.forEach(u -> mediaStorageService.delete(u.getPublicId()));
                }
            });

            tutor.setCertificates(Set.copyOf(certificateMapper.toCertificates(uploaded, tutor)));
        }


        tutorRepository.save(tutor);
        systemLogPublisher.success(tutor.getId(), SystemAction.REGISTER,
                User.class.getSimpleName(), tutor.getId(),
                Map.of("role", RoleName.TUTOR.name(),
                        "subjectCount", subjects.size()));

        log.info("Registered new tutor account: {}", tutor.getId());
        if (!files.isEmpty()) {
            eventPublisher.publishEvent(TutorCertificatesSubmittedEvent.builder().tutorId(tutor.getId()).build());
        }
        return buildAuthResponse(tutor);
    }

    @Transactional(readOnly = true)
    public AuthenticateResponse authenticate(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            systemLogPublisher.failed(SystemAction.LOGIN, ErrorCode.INVALID_CREDENTIALS.name(), null);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getUserStatus() == UserStatus.BANNED) {
            systemLogPublisher.failed(SystemAction.LOGIN, ErrorCode.ACCOUNT_BANNED.name(),
                    Map.of("userId", user.getId().toString()));
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }

        String refreshToken = jwtService.generateRefreshToken(user);
        AuthenticateResponse response = buildAuthResponse(user, refreshToken);

        systemLogPublisher.success(user.getId(), SystemAction.LOGIN, User.class.getSimpleName(), user.getId(),
                Map.of("method", "password"));


        return response;
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
            systemLogPublisher.success(UUID.fromString(signedJWT.getJWTClaimsSet().getSubject()), SystemAction.LOGOUT,
                    User.class.getSimpleName(), signedJWT.getJWTClaimsSet().getSubject(), null);
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

    private Role getRoleOrThrow(RoleName roleNameEnum) {
        return roleRepository.findByName(roleNameEnum)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.ROLE_NOT_FOUND
                        )
                );
    }

    private AuthenticateResponse buildAuthResponse(User user, String refreshToken) {

        BaseUserResponse userResponse = switch (user) {
            case Tutor tutor -> {
                TutorResponse r = userMapper.toTutorResponse(tutor);
                r.setSubjects(tutor.getSubjects().stream()
                        .map(subjectMapper::toUserSubjectResponse)
                        .toList());
                yield r;
            }
            case Student student -> userMapper.toStudentResponse(student);
            case Admin admin -> userMapper.toBaseUserResponse(admin);
            default -> throw new AppException(ErrorCode.USER_NOT_SUPPORTED);
        };

        return AuthenticateResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken)
                .tokenType("Bearer")
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


    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        IntrospectResponse introspectResponse = introspect(new IntrospectRequest(request.getToken()));
        User user = userRepository.findById(UUID.fromString(introspectResponse.getUserId()))
                .orElseThrow(UserNotFoundException::new);
        // Invalidate all tokens by saving the current timestamp in Redis
        String changedPasswordDate = String.valueOf(Instant.now().toEpochMilli());
        changePasswordService.saveTokenInvalidationTimestamp(user.getId().toString(), changedPasswordDate);
        refreshTokenService.revokeAllRefreshTokens(user.getId().toString());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        systemLogPublisher.success(user.getId(), SystemAction.PASSWORD_RESET,
                User.class.getSimpleName(), user.getId(), null);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresentOrElse(user -> {
            String newPassword = passwordGenerator.generatePassword();

            user.setPassword(passwordEncoder.encode(newPassword));
            systemLogPublisher.success(user.getId(), SystemAction.PASSWORD_FORGOT,
                    User.class.getSimpleName(), user.getId(), null);
            userRepository.save(user);

            // Invalidate all refresh tokens for the user
            refreshTokenService.revokeAllRefreshTokens(user.getId().toString());

            emailService.sendResetPasswordEmail(
                    user.getEmail(),
                    newPassword
            );
        }, () -> {
            throw new UserNotFoundException();
        });
    }
}