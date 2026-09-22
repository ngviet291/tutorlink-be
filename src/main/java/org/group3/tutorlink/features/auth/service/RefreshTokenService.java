package org.group3.tutorlink.features.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public void save(String refreshToken, String email) {
        redisTemplate.opsForValue().set(PREFIX + refreshToken, email, refreshExpiration, TimeUnit.SECONDS);
    }
    public boolean exists(String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + refreshToken));
    }

    public String getEmail(String refreshToken) {
        return redisTemplate.opsForValue().get(PREFIX + refreshToken);
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(PREFIX + refreshToken);
    }
}