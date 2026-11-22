package org.group3.tutorlink.features.tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.tracking.enums.LogStatus;
import org.group3.tutorlink.features.tracking.enums.SystemAction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "system_logs", indexes = {
        @Index(name = "idx_system_logs_created", columnList = "created_at"),
        @Index(name = "idx_system_logs_user", columnList = "user_id, created_at"),
        @Index(name = "idx_system_logs_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SystemAction action;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LogStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

}