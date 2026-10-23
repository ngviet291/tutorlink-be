package org.group3.tutorlink.features.statistics.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.statistics.dto.response.AcceptedApplicationsResponse;
import org.group3.tutorlink.features.statistics.dto.response.RevenueStatisticsResponse;
import org.group3.tutorlink.features.statistics.dto.response.TopTutorResponse;
import org.group3.tutorlink.features.statistics.enums.StatisticsResponseCode;
import org.group3.tutorlink.features.statistics.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    // 1. GET /v1/statistics/revenue?from=&to= — ADMIN
    @GetMapping("/revenue")
    public ApiResponse<RevenueStatisticsResponse> getRevenueStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ApiResponse.<RevenueStatisticsResponse>builder()
                .code(StatisticsResponseCode.GET_REVENUE_STATISTICS.getCode())
                .message(StatisticsResponseCode.GET_REVENUE_STATISTICS.getMessage())
                .data(statisticsService.getRevenueStatistics(from, to))
                .build();
    }

    // 2. GET /v1/statistics/applications/accepted — ADMIN
    @GetMapping("/applications/accepted")
    public ApiResponse<AcceptedApplicationsResponse> getAcceptedApplicationsCount() {
        return ApiResponse.<AcceptedApplicationsResponse>builder()
                .code(StatisticsResponseCode.GET_ACCEPTED_APPLICATIONS_COUNT.getCode())
                .message(StatisticsResponseCode.GET_ACCEPTED_APPLICATIONS_COUNT.getMessage())
                .data(statisticsService.getAcceptedApplicationsCount())
                .build();
    }

    // 3. GET /v1/statistics/top-tutors?limit= — ADMIN
    @GetMapping("/top-tutors")
    public ApiResponse<List<TopTutorResponse>> getTopTutors(
            @RequestParam(defaultValue = "10") int limit) {

        return ApiResponse.<List<TopTutorResponse>>builder()
                .code(StatisticsResponseCode.GET_TOP_TUTORS.getCode())
                .message(StatisticsResponseCode.GET_TOP_TUTORS.getMessage())
                .data(statisticsService.getTopTutors(limit))
                .build();
    }
}
