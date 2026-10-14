/*
 * @ (#) ApplicationRepository,java       1.0    29/09/2026
 *
 * Copyright (c) 2026 IUH. All righta reserved.
 */

package org.group3.tutorlink.features.application.repository;

/*
 * @description:
 * @author: Ho Thi Kim Xuyen
 * @version:     1.0
 * @date: 29/09/2026 14:57
 */
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository
        extends JpaRepository<Application, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
            FROM Application a
            WHERE a.post.id = :postId
              AND a.tutor.id = :tutorId
            """)
    boolean existsByPostIdAndTutorId(
            @Param("postId") UUID postId,
            @Param("tutorId") UUID tutorId
    );

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
            FROM Application a
            WHERE a.post.id = :postId
              AND a.student.id = :studentId
            """)
    boolean existsByPostIdAndStudentId(
            @Param("postId") UUID postId,
            @Param("studentId") UUID studentId
    );

    @Query("""
            SELECT a FROM Application a
            WHERE a.tutor.id = :tutorId
              AND (:cursor IS NULL OR a.id < :cursor)
            ORDER BY a.id DESC
            """)
    List<Application> findByTutorIdAfterCursor(
            @Param("tutorId") UUID tutorId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM Application a
            WHERE a.student.id = :studentId
              AND (:cursor IS NULL OR a.id < :cursor)
            ORDER BY a.id DESC
            """)
    List<Application> findByStudentIdAfterCursor(
            @Param("studentId") UUID studentId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.post p
            LEFT JOIN FETCH p.author
            WHERE a.id = :applicationId
            """)
    Optional<Application> findByIdForSelection(
            @Param("applicationId") UUID applicationId
    );

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.post p
            LEFT JOIN FETCH p.author
            LEFT JOIN FETCH a.student
            LEFT JOIN FETCH a.tutor
            WHERE a.id = :applicationId
              AND a.applicationStatus = :applicationStatus
              AND p.status = :postStatus
              AND (
                    a.student.id = :userId
                    OR a.tutor.id = :userId
                    OR p.author.id = :userId
              )
            """)
    Optional<Application> findByIdForViewer(
            @Param("applicationId") UUID applicationId,
            @Param("userId") UUID userId,
            @Param("applicationStatus") ApplicationStatus applicationStatus,
            @Param("postStatus") PostStatus postStatus
    );

    @Query("""
            SELECT a FROM Application a
            WHERE a.id = :applicationId
              AND (a.student.id = :userId OR a.tutor.id = :userId)
            """)
    Optional<Application> findByIdAndApplicantId(
            @Param("applicationId") UUID applicationId,
            @Param("userId") UUID userId
    );

    @Query("""
            SELECT a FROM Application a
            WHERE (:status IS NULL OR a.applicationStatus = :status)
              AND (:cursor IS NULL OR a.id < :cursor)
            ORDER BY a.id DESC
            """)
    List<Application> findApplicationsAfterCursor(
            @Param("status") ApplicationStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM Application a
            WHERE a.post.id = :postId
              AND a.applicationStatus = :applicationStatus
            """)
    Page<Application> findByPostIdAndApplicationStatus(
            @Param("postId") UUID postId,
            @Param("applicationStatus") ApplicationStatus applicationStatus,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
            FROM Application a
            WHERE a.post.id = :postId
              AND a.applicationStatus = :applicationStatus
            """)
    boolean existsByPostIdAndApplicationStatus(
            @Param("postId") UUID postId,
            @Param("applicationStatus") ApplicationStatus applicationStatus
    );

}