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
import org.group3.tutorlink.features.statistics.dto.Period;
import org.group3.tutorlink.features.statistics.dto.response.AcceptedPointResponse;
import org.group3.tutorlink.features.statistics.dto.response.RevenuePointResponse;
import org.group3.tutorlink.features.statistics.dto.response.TopTutorResponse;
import org.group3.tutorlink.features.statistics.enums.TimeDetail;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final int DEFAULT_TOP_TUTORS_LIMIT = 10;
    private static final int MAX_TOP_TUTORS_LIMIT = 50;
    private static final long MAX_DAYS_FOR_DAILY = 366;

    private final TransactionRepository transactionRepository;
    private final RevenueReportRepository revenueReportRepository;
    private final AdminRepository adminRepository;
    private final ApplicationRepository applicationRepository;
    private final AppUtil appUtil;

    // Doanh thu (phí hoa hồng) theo từng mốc thời gian
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<RevenuePointResponse> getRevenueSeries(LocalDate from, LocalDate to, TimeDetail timeDetail) {
        List<RevenuePointResponse> result = new ArrayList<>();

        for (Period period : splitPeriods(from, to, timeDetail)) {
            BigDecimal totalRevenue = BigDecimal.ZERO;
            long totalTransactions = 0;
            for (TransactionStatRow row : transactionRepository.summarize(
                    toStartInstant(period.getStartDate()), toEndInstant(period.getEndDate()))) {
                if (row.getTransactionType() == TransactionType.COMMISSION
                        && row.getTransactionStatus() == TransactionStatus.SUCCESS) {
                    totalRevenue = row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount();
                    totalTransactions = row.getTransactionCount();
                    break;
                }
            }

            result.add(RevenuePointResponse.builder()
                    .periodStart(period.getStartDate())
                    .periodEnd(period.getEndDate())
                    .totalRevenue(totalRevenue)
                    .totalTransactions(totalTransactions)
                    .build());
        }
        return result;
    }

    // Số lượt nhận lớp thành công theo từng mốc thời gian
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<AcceptedPointResponse> getAcceptedSeries(LocalDate from, LocalDate to, TimeDetail timeDetail) {
        List<AcceptedPointResponse> result = new ArrayList<>();

        for (Period period : splitPeriods(from, to, timeDetail)) {
            long totalAccepted = applicationRepository.countByStatusBetween(
                    ApplicationStatus.ACCEPTED,
                    toStartInstant(period.getStartDate()),
                    toEndInstant(period.getEndDate()));

            result.add(AcceptedPointResponse.builder()
                    .periodStart(period.getStartDate())
                    .periodEnd(period.getEndDate())
                    .totalAccepted(totalAccepted)
                    .build());
        }
        return result;
    }

    // Top gia sư (from/to có thể null = lấy tất cả thời gian)
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<TopTutorResponse> getTopTutors(int limit, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(StatisticsErrorCode.INVALID_DATE_RANGE);
        }
        Instant start = (from == null) ? Instant.EPOCH : toStartInstant(from);
        Instant end = (to == null) ? Instant.now().plus(1, ChronoUnit.DAYS) : toEndInstant(to);

        Pageable pageable = PageRequest.of(0, normalizeLimit(limit));

        List<TopTutorRow> rows = applicationRepository.findTopTutorsByAcceptedCount(
                ApplicationStatus.ACCEPTED, start, end, pageable);

        return rows.stream()
                .map(row -> TopTutorResponse.builder()
                        .tutorId(row.getTutorId())
                        .fullName(row.getFullName())
                        .averageRating(row.getAverageRating())
                        .acceptedCount(row.getAcceptedCount())
                        .build())
                .toList();
    }

    // Chiafrom..to thành  đoạn:ngày / tuần / tháng / năm
    private List<Period> splitPeriods(LocalDate from, LocalDate to, TimeDetail timeDetail) {
        if (from == null || to == null || timeDetail == null || from.isAfter(to)) {
            throw new AppException(StatisticsErrorCode.INVALID_DATE_RANGE);
        }
        if (timeDetail == TimeDetail.DAY && ChronoUnit.DAYS.between(from, to) > MAX_DAYS_FOR_DAILY) {
            throw new AppException(StatisticsErrorCode.RANGE_TOO_LARGE);
        }

        List<Period> periods = new ArrayList<>();
        LocalDate start = from;
        while (!start.isAfter(to)) {
            LocalDate end = switch (timeDetail) {
                case DAY -> start;
                case WEEK -> start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                case MONTH -> start.with(TemporalAdjusters.lastDayOfMonth());
                case YEAR -> start.with(TemporalAdjusters.lastDayOfYear());
            };
            if (end.isAfter(to)) {
                end = to;
            }
            periods.add(new Period(start, end));
            start = end.plusDays(1);
        }
        return periods;
    }

    private Instant toStartInstant(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant toEndInstant(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? DEFAULT_TOP_TUTORS_LIMIT : Math.min(limit, MAX_TOP_TUTORS_LIMIT);
    }
}