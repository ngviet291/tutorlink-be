package org.group3.tutorlink.features.payment.gateway.vietqr;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.payment.gateway.HmacUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Xác thực cho các API mà VietQR gọi vào backend:
 * - token_generate: VietQR gửi Basic auth (username/password do mình đặt rồi khai báo với VietQR);
 * - transaction-sync: VietQR gửi Bearer token vừa lấy từ token_generate.
 *
 * Token ký bằng secret RIÊNG (payment.vietqr.callback.jwt-secret), không dùng jwt.secret-key của người dùng,
 * để token VietQR không thể dùng làm token đăng nhập và ngược lại.
 */
@Component
@ConditionalOnProperty(name = "payment.vietqr.enabled", havingValue = "true")
public class VietQrCallbackAuth {

    private static final String ISSUER = "tutorlink-vietqr-callback";
    private static final String AUDIENCE = "vietqr-transaction-sync";
    // HS256 yêu cầu khóa tối thiểu 256 bit.
    private static final int MIN_SECRET_BYTES = 32;
    private final AppUtil appUtil;

    @Value("${payment.vietqr.callback.username:}")
    private String username;

    @Value("${payment.vietqr.callback.password:}")
    private String password;

    @Value("${payment.vietqr.callback.jwt-secret:}")
    private String jwtSecret;

    @Value("${payment.vietqr.callback.token-ttl-seconds:300}")
    private long tokenTtlSeconds;

    public VietQrCallbackAuth(AppUtil appUtil) {
        this.appUtil = appUtil;
    }

    /** Chưa cấu hình đủ thì từ chối mọi yêu cầu (tránh khớp nhầm chuỗi rỗng). */
    public boolean isConfigured() {
        return !username.isBlank() && !password.isBlank()
                && jwtSecret.getBytes(StandardCharsets.UTF_8).length >= MIN_SECRET_BYTES;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public boolean isBasicAuthValid(String authorizationHeader) {
        if (!isConfigured() || authorizationHeader == null) {
            return false;
        }
        String expected = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return HmacUtil.secureEquals(expected, authorizationHeader.trim());
    }

    public String issueToken() {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .subject("vietqr")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(tokenTtlSeconds)))
                .jwtID(appUtil.generateUUID().toString())
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secretBytes()));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Cannot sign VietQR callback token", ex);
        }
    }

    public boolean isBearerTokenValid(String authorizationHeader) {
        if (!isConfigured() || authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        try {
            SignedJWT jwt = SignedJWT.parse(authorizationHeader.substring("Bearer ".length()).trim());
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new MACVerifier(secretBytes()))) {
                return false;
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiresAt = claims.getExpirationTime();
            return expiresAt != null && expiresAt.after(new Date())
                    && ISSUER.equals(claims.getIssuer())
                    && claims.getAudience().contains(AUDIENCE);
        } catch (ParseException | JOSEException ex) {
            return false;
        }
    }

    private byte[] secretBytes() {
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }
}
