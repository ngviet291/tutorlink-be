package org.group3.tutorlink.features.subject.repository;

import org.group3.tutorlink.features.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
}
