package org.group3.tutorlink.features.subject.repository;

import org.group3.tutorlink.features.subject.entity.Subject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    @Query("select count(t) > 0 from Tutor t where t.subject.id = :subjectId")
    boolean isUsedByTutor(@Param("subjectId") UUID subjectId);

    @Query("""
        SELECT s FROM Subject s
        WHERE (:cursor IS NULL OR s.id < :cursor)
        """)
    List<Subject> findPage(@Param("cursor") UUID cursor, Pageable pageable);

}
