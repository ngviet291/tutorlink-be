package org.group3.tutorlink.features.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Redis ZSET delay queue cho application expiry.
 * member = applicationId (UUID string), score = epoch ms thời điểm hết hạn.
 * ZADD cùng member ghi đè score => đổi hạn là tự cập nhật.
 */
@Component
@RequiredArgsConstructor
public class ApplicationExpiryQueue {

    public enum Stage {
        PAYMENT("application:expiry:payment"),
        CONFIRM("application:expiry:confirm");

        final String key;

        Stage(String key) {
            this.key = key;
        }
    }

    // Lấy và xóa atomic các item đã đến hạn -> chỉ 1 instance nhận được mỗi item
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final DefaultRedisScript<List> CLAIM = new DefaultRedisScript<>("""
            local ids = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2])
            if #ids > 0 then redis.call('ZREM', KEYS[1], unpack(ids)) end
            return ids
            """, List.class);

    private final StringRedisTemplate redis;

    public void schedule(Stage stage, UUID applicationId, Instant expireAt) {
        redis.opsForZSet().add(stage.key, applicationId.toString(), expireAt.toEpochMilli());
    }

    public void cancel(Stage stage, UUID applicationId) {
        redis.opsForZSet().remove(stage.key, applicationId.toString());
    }

    @SuppressWarnings("unchecked")
    public List<String> claimDue(Stage stage, int limit) {
        List<String> ids = (List<String>) redis.execute(
                CLAIM, List.of(stage.key),
                String.valueOf(System.currentTimeMillis()), String.valueOf(limit));
        return ids == null ? List.of() : ids;
    }
}
