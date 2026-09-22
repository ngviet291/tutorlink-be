package org.group3.tutorlink.features.auth.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.ErrorCode;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.dto.response.IntrospectResponse;
import org.group3.tutorlink.features.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${spring.application.name}")
    private String issuer;

    private static final String ROLE_CLAIM = "role";
    private static final String USERNAME_CLAIM = "username";

    public String generateAccessToken(User user) {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512)
                .type(JOSEObjectType.JWT)
                .build();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .claim(USERNAME_CLAIM, user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().getName())
                .issuer(issuer)
                .issueTime(new Date())
                .expirationTime(AppUtil.expirationDate(expiration))
                .jwtID(AppUtil.generateUUID().toString())
                .build();
        JWSObject jwsObject = new JWSObject(header, new Payload(claims.toJSONObject()));
        try {
            jwsObject.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new AppException(ErrorCode.GENERATE_TOKEN_FAILED);
        }
    }
    public String generateRefreshToken() {
        return AppUtil.generateOpaqueToken();
    }

    public long getExpirationSeconds() {
        return expiration;
    }

    public SignedJWT verifyToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));
            boolean verified = signedJWT.verify(verifier);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expirationTime = claims.getExpirationTime();
            if (!verified || expirationTime == null) {
                throw new AppException(ErrorCode.INTROSPECT_FAILED);
            }

            if (!expirationTime.after(new Date())) {
                throw new AppException(ErrorCode.INTROSPECT_FAILED);
            }
            String jwtId = claims.getJWTID();
            if (jwtId == null) {
                throw new AppException(ErrorCode.INTROSPECT_FAILED);
            }
            if (tokenBlacklistService.isBlacklisted(jwtId)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            return signedJWT;
        } catch (ParseException | JOSEException e) {
            throw new AppException(
                    ErrorCode.INTROSPECT_FAILED
            );
        }
    }

    public String getJwtId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getJWTID();
        } catch (ParseException e) {
            throw new AppException(ErrorCode.INTROSPECT_FAILED);
        }
    }

    public long getRemainingSeconds(String token) {
        try {
            SignedJWT signedJWT = verifyToken(token);

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            long remainingMillis = expirationTime.getTime() - System.currentTimeMillis();
            return Math.max(0, remainingMillis / 1000);
        } catch (ParseException e) {
            throw new AppException(ErrorCode.INTROSPECT_FAILED);
        }
    }
    public IntrospectResponse introspect(String token) {
        try {
            SignedJWT signedJWT = verifyToken(token);
            JWTClaimsSet claims =
                    signedJWT.getJWTClaimsSet();
            return IntrospectResponse.builder()
                    .active(true)
                    .role(
                            claims.getStringClaim(ROLE_CLAIM)
                    )
                    .userId(
                            claims.getSubject()
                    )
                    .exp(
                            claims.getExpirationTime()
                                    .getTime() / 1000
                    )
                    .iat(
                            claims.getIssueTime()
                                    .getTime() / 1000
                    )
                    .sub(
                            claims.getSubject()
                    )
                    .build();

        } catch (AppException e) {
            return IntrospectResponse.builder()
                    .active(false)
                    .build();
        } catch (ParseException e) {

            throw new AppException(
                    ErrorCode.INTROSPECT_FAILED
            );
        }
    }

    public boolean isTokenValid(String token) {
        try {
            verifyToken(token);
            return true;

        } catch (AppException e) {
            return false;
        }
    }
}