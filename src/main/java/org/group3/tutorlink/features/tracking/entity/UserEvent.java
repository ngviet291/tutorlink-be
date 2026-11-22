package org.group3.tutorlink.features.tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.tracking.enums.ClientEventType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_events", indexes = {
        @Index(name = "idx_user_events_occurred", columnList = "occurred_at"),
        @Index(name = "idx_user_events_user", columnList = "user_id, occurred_at"),
        @Index(name = "idx_user_events_name", columnList = "name, occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "anon_id", nullable = false, length = 64)
    private String anonId;

    // Null khi chưa đăng nhập
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 64)
    private ClientEventType name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> props;

    // Thời điểm xảy ra theo đồng hồ trình duyệt
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    // Thời điểm server nhận
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}