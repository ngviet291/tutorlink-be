package org.group3.tutorlink.features.tracking.repo;
import org.group3.tutorlink.features.tracking.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SystemLogRepository extends JpaRepository<SystemLog, UUID> {}