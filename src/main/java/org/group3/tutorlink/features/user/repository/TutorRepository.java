package org.group3.tutorlink.features.user.repository;

import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TutorRepository extends JpaRepository<Tutor, UUID> {
}
