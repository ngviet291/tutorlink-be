package org.group3.tutorlink.features.review.repository;

import org.group3.tutorlink.features.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByApplicationId(UUID applicationId);

    @Query("""
            SELECT r FROM Review r
            WHERE r.tutor.id = :tutorId
              AND (:cursor IS NULL OR r.id < :cursor)
            ORDER BY r.id DESC
            """)
    List<Review> findByTutorIdAfterCursor(
            @Param("tutorId") UUID tutorId,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );
}
