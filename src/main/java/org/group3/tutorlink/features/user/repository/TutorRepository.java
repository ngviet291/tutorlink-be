package org.group3.tutorlink.features.user.repository;

import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface TutorRepository extends JpaRepository<Tutor, UUID> {

    @Query("""
            SELECT t FROM Tutor t
            WHERE t.verificationStatus = :status
              AND t.userStatus = org.group3.tutorlink.features.user.enums.UserStatus.ACTIVE

              AND (
                  :subjectName IS NULL
                  OR EXISTS (
                      SELECT 1
                      FROM Tutor tutor
                      JOIN tutor.subjects subject
                      WHERE tutor.id = t.id
                        AND LOWER(subject.name) LIKE
                            LOWER(CONCAT('%', :subjectName, '%'))
                  )
              )

              AND (:province IS NULL OR
                   LOWER(t.address.province) LIKE
                   LOWER(CONCAT('%', :province, '%')))

              AND (:minRate IS NULL OR t.hourlyRate >= :minRate)
              AND (:maxRate IS NULL OR t.hourlyRate <= :maxRate)
              AND (:minRating IS NULL OR t.averageRating >= :minRating)
              AND (:minExperience IS NULL OR t.experienceYears >= :minExperience)
              AND (:cursor IS NULL OR t.id < :cursor)

            ORDER BY t.id DESC
            """)
    List<Tutor> findApprovedTutors(
            @Param("status") VerificationStatus status,
            @Param("subjectName") String subjectName,
            @Param("province") String province,
            @Param("minRate") Double minRate,
            @Param("maxRate") Double maxRate,
            @Param("minRating") Double minRating,
            @Param("minExperience") Integer minExperience,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );
}