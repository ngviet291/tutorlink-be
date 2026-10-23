package org.group3.tutorlink.features.statistics.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.repository.TopTutorRow;
import org.group3.tutorlink.features.payment.entity.RevenueReport;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;
import org.group3.tutorlink.features.payment.repository.RevenueReportRepository;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.group3.tutorlink.features.payment.repository.TransactionStatRow;
import org.group3.tutorlink.features.statistics.dto.response.AcceptedApplicationsResponse;
import org.group3.tutorlink.features.statistics.dto.response.RevenueStatisticsResponse;
import org.group3.tutorlink.features.statistics.dto.response.TopTutorResponse;
import org.group3.tutorlink.features.statistics.exception.StatisticsErrorCode;
import org.group3.tutorlink.features.statistics.service.StatisticsService;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.repository.AdminRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final int DEFAULT_TOP_TUTORS_LIMIT = 10;
    private static final int MAX_TOP_TUTORS_LIMIT = 50;

    private final TransactionRepository transactionRepository;
    private final RevenueReportRepository revenueReportRepository;
    private final AdminRepository adminRepository;
    private final ApplicationRepository applicationRepository;
    private final AppUtil appUtil;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public RevenueStatisticsResponse getRevenueStatistics(java.time.LocalDate from, java.time.LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new AppException(StatisticsErrorCode.INVALID_DATE_RANGE);
        }

        UUID adminId = appUtil.userIdFromAuthentication();
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AppException(StatisticsErrorCode.ADMIN_NOT_FOUND));
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalTransactions = 0;
        for (TransactionStatRow row : transactionRepository.summarize(start, end)) {
            if (row.getTransactionType() == TransactionType.COMMISSION
                    && row.getTransactionStatus() == TransactionStatus.SUCCESS) {
                totalRevenue = row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount();
                totalTransactions = row.getTransactionCount();
                break;
            }
        }

        RevenueReport report = RevenueReport.builder()
                .id(appUtil.generateUUID())
                .periodStart(from)
                .periodEnd(to)
                .totalRevenue(totalRevenue.doubleValue())
                .totalTransactions((int) totalTransactions)
                .generatedByAdmin(admin)
                .build();
        revenueReportRepository.save(report);

        return RevenueStatisticsResponse.builder()
                .reportId(report.getId())
                .periodStart(from)
                .periodEnd(to)
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .generatedByAdminId(adminId)
                .generatedAt(report.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public AcceptedApplicationsResponse getAcceptedApplicationsCount() {
        long total = applicationRepository.countByApplicationStatus(ApplicationStatus.ACCEPTED);
        return AcceptedApplicationsResponse.builder()
                .totalAccepted(total)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<TopTutorResponse> getTopTutors(int limit) {
        int size = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, size);

        List<TopTutorRow> rows = applicationRepository.findTopTutorsByAcceptedCount(
                ApplicationStatus.ACCEPTED, pageable);

        return rows.stream()
                .map(row -> TopTutorResponse.builder()
                        .tutorId(row.getTutorId())
                        .fullName(row.getFullName())
                        .averageRating(row.getAverageRating())
                        .acceptedCount(row.getAcceptedCount())
                        .build())
                .toList();
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? DEFAULT_TOP_TUTORS_LIMIT : Math.min(limit, MAX_TOP_TUTORS_LIMIT);
    }
}
