package org.group3.tutorlink.features.tracking.repo;

import org.group3.tutorlink.features.tracking.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserEventRepository extends JpaRepository<UserEvent, UUID> {}