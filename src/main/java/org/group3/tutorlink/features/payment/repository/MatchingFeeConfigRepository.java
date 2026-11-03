package org.group3.tutorlink.features.payment.repository;

import org.group3.tutorlink.features.payment.entity.MatchingFeeConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchingFeeConfigRepository extends JpaRepository<MatchingFeeConfig, UUID> {

    // Lấy tất cả cấu hình, sắp xếp theo ngày hiệu lực giảm dần
    List<MatchingFeeConfig> findAllByOrderByEffectiveFromDesc(Pageable pageable);

    List<MatchingFeeConfig> findByIdLessThanOrderByEffectiveFromDesc(UUID cursor, Pageable pageable);

    // Tìm cấu hình đang active
    @Query("""
            SELECT fc FROM MatchingFeeConfig fc
            WHERE fc.effectiveFrom <= :date
            AND (fc.effectiveTo IS NULL OR fc.effectiveTo > :date)
            ORDER BY fc.effectiveFrom DESC
            LIMIT 1
            """)
    Optional<MatchingFeeConfig> findActiveConfigOn(@Param("date") LocalDate date);

    // Tìm các cấu hình có khoảng thời gian hiệu lực chồng lấn
    @Query("""
            SELECT fc FROM MatchingFeeConfig fc
            WHERE fc.effectiveFrom < :endDate
            AND (fc.effectiveTo IS NULL OR fc.effectiveTo > :startDate)
            """)
    List<MatchingFeeConfig> findOverlappingConfigs(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
