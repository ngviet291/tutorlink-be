package org.group3.tutorlink.features.review.repository;

import org.group3.tutorlink.features.application.dto.projection.ReviewCountRow;
import org.group3.tutorlink.features.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("""
            select avg(r.rating) from Review r
            where r.tutor.id = :tutorId
              AND NOT EXISTS (SELECT 1 FROM ReviewComplaint c WHERE c.review = r
                  AND c.status = org.group3.tutorlink.features.review.enums.ComplaintStatus.APPROVED)
            """)
    Double averageRatingByTutorId(@Param("tutorId") UUID tutorId);
    boolean existsByApplicationId(UUID applicationId);

    @Query("""
            select count(r) from Review r
            where r.tutor.id = :tutorId
              AND NOT EXISTS (SELECT 1 FROM ReviewComplaint c WHERE c.review = r
                  AND c.status = org.group3.tutorlink.features.review.enums.ComplaintStatus.APPROVED)
            """)
    long countByTutorId(@Param("tutorId") UUID tutorId);

    @Query("""
            SELECT r FROM Review r
            WHERE r.tutor.id = :tutorId
              AND (:cursor IS NULL OR r.id < :cursor)
              AND NOT EXISTS (SELECT 1 FROM ReviewComplaint c WHERE c.review = r
                  AND c.status = org.group3.tutorlink.features.review.enums.ComplaintStatus.APPROVED)
            ORDER BY r.id DESC
            """)
    List<Review> findByTutorIdAfterCursor(
            @Param("tutorId") UUID tutorId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );


    @Query("""
    SELECT r.tutor.id AS tutorId, COUNT(r) AS cnt
    FROM Review r
    WHERE r.tutor.id IN :tutorIds
      AND NOT EXISTS (SELECT 1 FROM ReviewComplaint c WHERE c.review = r
                  AND c.status = org.group3.tutorlink.features.review.enums.ComplaintStatus.APPROVED)
    GROUP BY r.tutor.id
    """)
    List<ReviewCountRow> countByTutorIds(@Param("tutorIds") Collection<UUID> tutorIds);
}