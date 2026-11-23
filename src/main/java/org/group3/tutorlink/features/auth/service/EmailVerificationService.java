package org.group3.tutorlink.features.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.ErrorCode;
import org.group3.tutorlink.features.auth.exception.ErrorCodeAuth;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Xác minh email khi đăng ký bằng mã 6 số lưu trong Redis.
 * <ul>
 *   <li>{@code register:code:{email}}: mã, hết hạn sau {@code ttl}</li>
 *   <li>{@code register:attempts:{email}}: số lần nhập sai, vượt {@code max-attempts} thì hủy mã</li>
 *   <li>{@code register:cooldown:{email}}: chặn gửi lại quá nhanh</li>
 * </ul>
 */
@Slf4j
@Service
public class EmailVerificationService {

    private static final String CODE_PREFIX = "register:code:";
    private static final String ATTEMPTS_PREFIX = "register:attempts:";
    private static final String COOLDOWN_PREFIX = "register:cooldown:";

    private final RedisTemplate<String, String> redis;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;
    private final Duration resendCooldown;
    private final int maxAttempts;

    public EmailVerificationService(
            @Qualifier("redisTemplate") RedisTemplate<String, String> redis,
            EmailService emailService,
            UserRepository userRepository,
            @Value("${app.verification.ttl-seconds:600}") long ttlSeconds,
            @Value("${app.verification.resend-cooldown-seconds:60}") long cooldownSeconds,
            @Value("${app.verification.max-attempts:5}") int maxAttempts) {
        this.redis = redis;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.resendCooldown = Duration.ofSeconds(cooldownSeconds);
        this.maxAttempts = maxAttempts;
    }

    /** Tạo mã, lưu Redis rồi gửi về email. Không gửi nếu email đã có tài khoản. */
    public void sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        String key = normalize(email);

        // setIfAbsent là thao tác nguyên tử nên bấm gửi liên tục cũng chỉ qua được 1 lần
        Boolean acquired = redis.opsForValue().setIfAbsent(COOLDOWN_PREFIX + key, "1", resendCooldown);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new AppException(ErrorCodeAuth.VERIFICATION_RESEND_TOO_SOON);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForValue().set(CODE_PREFIX + key, code, ttl);
        redis.delete(ATTEMPTS_PREFIX + key);

        try {
            emailService.sendVerificationEmail(email, code, (int) ttl.toMinutes());
        } catch (RuntimeException e) {
            // Gửi mail lỗi thì cho người dùng thử lại ngay
            redis.delete(List.of(CODE_PREFIX + key, COOLDOWN_PREFIX + key));
            log.error("Failed to send verification email to {}", email, e);
            throw e;
        }
    }

    /** Kiểm tra mã (chưa xóa). Gọi {@link #invalidate(String)} sau khi đăng ký thành công. */
    public void verify(String email, String code) {
        String key = normalize(email);
        String stored = redis.opsForValue().get(CODE_PREFIX + key);
        if (stored == null) {
            throw new AppException(ErrorCodeAuth.VERIFICATION_CODE_EXPIRED);
        }

        Long attempts = redis.opsForValue().increment(ATTEMPTS_PREFIX + key);
        if (attempts != null && attempts == 1L) {
            redis.expire(ATTEMPTS_PREFIX + key, ttl);
        }
        if (attempts != null && attempts > maxAttempts) {
            redis.delete(List.of(CODE_PREFIX + key, ATTEMPTS_PREFIX + key));
            throw new AppException(ErrorCodeAuth.VERIFICATION_TOO_MANY_ATTEMPTS);
        }

        boolean match = code != null && MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                code.trim().getBytes(StandardCharsets.UTF_8));
        if (!match) {
            throw new AppException(ErrorCodeAuth.VERIFICATION_CODE_INVALID);
        }
    }

    /** Xóa mã sau khi dùng xong (cooldown giữ nguyên). */
    public void invalidate(String email) {
        String key = normalize(email);
        redis.delete(List.of(CODE_PREFIX + key, ATTEMPTS_PREFIX + key));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
