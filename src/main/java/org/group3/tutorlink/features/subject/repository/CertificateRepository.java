package org.group3.tutorlink.features.subject.repository;

import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findAllByTutor_IdOrderByCreatedAtDesc(UUID tutorId);
    long countByTutor_Id(UUID tutorId);
    Optional<Certificate> findByIdAndTutor_Id(UUID id, UUID tutorId);
}
