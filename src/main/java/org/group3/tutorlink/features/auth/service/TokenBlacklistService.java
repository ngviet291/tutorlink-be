package org.group3.tutorlink.features.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private final StringRedisTemplate redisTemplate;

    public void blacklist(String jwtId, long ttlSeconds) {
        if (jwtId == null || ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jwtId, "true", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String jwtId) {
        if (jwtId == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jwtId));
    }
}