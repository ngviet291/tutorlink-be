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
import org.group3.tutorlink.features.application.dto.projection.TutorAcceptedRow;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
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
              AND a.applicationStatus IN :liveStatuses
            """)
    boolean existsByPostIdAndTutorId(
            @Param("postId") UUID postId,
            @Param("tutorId") UUID tutorId,
            @Param("liveStatuses") Collection<ApplicationStatus> liveStatuses
    );

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
            FROM Application a
            WHERE a.post.id = :postId
              AND a.student.id = :studentId
              AND a.applicationStatus IN :liveStatuses
            """)
    boolean existsByPostIdAndStudentId(
            @Param("postId") UUID postId,
            @Param("studentId") UUID studentId,
            @Param("liveStatuses") Collection<ApplicationStatus> liveStatuses
    );

    // User có đơn (bất kỳ status) trên post với vai trò student hoặc tutor —
    // gồm cả author bài (author luôn nằm trong application row) lẫn applicant.
    // Dùng cho visibility check post PENDING/CLOSED trong PostServiceImpl.getPostById.
    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
            FROM Application a
            WHERE a.post.id = :postId
              AND (a.student.id = :userId OR a.tutor.id = :userId)
            """)
    boolean existsParticipantByPostIdAndUserId(
            @Param("postId") UUID postId,
            @Param("userId") UUID userId
    );

    @Query("""
            SELECT a FROM Application a
            WHERE a.post.id = :postId
              AND (:cursor IS NULL OR a.id < :cursor)
            ORDER BY a.id DESC
            """)
    List<Application> findByPostIdAfterCursor(
            @Param("postId") UUID postId,
            @Param("cursor") UUID cursor,
            Pageable pageable
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
            JOIN FETCH p.author
            WHERE a.id = :applicationId
              AND a.applicationStatus = :applicationStatus
              AND p.status = :postStatus
              AND p.author.id = :userId
            """)
    Optional<Application> findSelectableApplication(
            @Param("applicationId") UUID applicationId,
            @Param("userId") UUID userId,
            @Param("applicationStatus") ApplicationStatus applicationStatus,
            @Param("postStatus") PostStatus postStatus
    );

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.post p
            LEFT JOIN FETCH p.author
            LEFT JOIN FETCH a.student
            LEFT JOIN FETCH a.tutor
            WHERE a.id = :applicationId
              AND (
                    a.student.id = :userId
                    OR a.tutor.id = :userId
                    OR p.author.id = :userId
              )
            """)
    Optional<Application> findByIdForViewer(
            @Param("applicationId") UUID applicationId,
            @Param("userId") UUID userId
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
            JOIN FETCH a.post p
            LEFT JOIN FETCH p.author
            LEFT JOIN FETCH a.student
            LEFT JOIN FETCH a.tutor
            WHERE a.id = :applicationId
              AND a.applicationStatus = :applicationStatus
              AND (
                    a.student.id = :studentId
                    OR p.author.id = :studentId
              )
            """)
    Optional<Application> findFinishedByStudentId(
            @Param("applicationId") UUID applicationId,
            @Param("studentId") UUID studentId,
            @Param("applicationStatus") ApplicationStatus applicationStatus
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

    // Đếm số lượt ACCEPTED trong 1 khoảng thời gian
    @Query("""
        SELECT COUNT(a) FROM Application a
        WHERE a.applicationStatus = :status
          AND a.appliedAt >= :start AND a.appliedAt < :end
        """)
    long countByStatusBetween(@Param("status") ApplicationStatus status,
                              @Param("start") Instant start,
                              @Param("end") Instant end);

    @Query("""
        SELECT a.tutor.id AS tutorId,
               a.tutor.fullName AS fullName,
               a.tutor.averageRating AS averageRating,
               COUNT(a) AS acceptedCount
        FROM Application a
        WHERE a.applicationStatus = :status
          AND a.tutor IS NOT NULL
          AND a.appliedAt >= :start AND a.appliedAt < :end
        GROUP BY a.tutor.id, a.tutor.fullName, a.tutor.averageRating
        ORDER BY COUNT(a) DESC
        """)
    List<TopTutorRow> findTopTutorsByAcceptedCount(
            @Param("status") ApplicationStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);
    @Query("""
    SELECT DISTINCT a FROM Application a
    JOIN FETCH a.tutor t
    LEFT JOIN FETCH t.subjects
    WHERE a.post.id = :postId
      AND a.applicationStatus = :status
    """)
    List<Application> findWithTutorByPostIdAndStatus(
            @Param("postId") UUID postId,
            @Param("status") ApplicationStatus status);


    @Query("""
        select a.tutor.id as tutorId, count(a) as cnt
        from Application a
        where a.tutor.id in :tutorIds
          and a.applicationStatus = :status
        group by a.tutor.id
        """)
    List<TutorAcceptedRow> countByTutorIdsAndStatus(@Param("tutorIds") Collection<UUID> tutorIds,
                                                    @Param("status") ApplicationStatus status);

    // W1: đơn ACCEPTED đã quá payment-window kể từ lúc select mà chưa có transaction SUCCESS.
    // Không filter post status: post có thể đã CLOSED/REMOVED bởi admin — đơn vẫn phải được dọn,
    // việc revert post do scheduler quyết định theo status hiện tại.
    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.post p
            LEFT JOIN FETCH a.transaction t
            WHERE a.applicationStatus = :status
              AND a.selectedAt < :cutoff
              AND (t IS NULL OR t.status <> :successStatus)
            """)
    List<Application> findExpiredSelections(
            @Param("status") ApplicationStatus status,
            @Param("cutoff") Instant cutoff,
            @Param("successStatus") TransactionStatus successStatus
    );

    // W2: đơn ACCEPTED đã thanh toán thành công quá 24h mà applicant chưa confirm
    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.post p
            JOIN FETCH a.transaction t
            WHERE a.applicationStatus = :status
              AND t.status = :successStatus
              AND t.transactionDate < :cutoff
            """)
    List<Application> findExpiredConfirmations(
            @Param("status") ApplicationStatus status,
            @Param("successStatus") TransactionStatus successStatus,
            @Param("cutoff") Instant cutoff
    );

    // Backfill Redis expiry queue: nạp các đơn ACCEPTED đang tồn tại
    @Query("""
            SELECT a FROM Application a
            LEFT JOIN FETCH a.transaction t
            WHERE a.applicationStatus = :status
            """)
    List<Application> findAllByApplicationStatus(@Param("status") ApplicationStatus status);
}
