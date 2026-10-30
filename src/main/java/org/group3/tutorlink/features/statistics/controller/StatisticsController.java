package org.group3.tutorlink.features.statistics.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.statistics.dto.response.*;
import org.group3.tutorlink.features.statistics.enums.StatisticsResponseCode;
import org.group3.tutorlink.features.statistics.enums.TimeDetail;
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

    // GET /v1/statistics/revenue/series?from=&to=&timeDetail=WEEK — ADMIN
    @GetMapping("/revenue")
    public ApiResponse<List<RevenuePointResponse>> getRevenueSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") TimeDetail timeDetail) {

        return ApiResponse.<List<RevenuePointResponse>>builder()
                .code(StatisticsResponseCode.GET_REVENUE_SERIES.getCode())
                .message(StatisticsResponseCode.GET_REVENUE_SERIES.getMessage())
                .data(statisticsService.getRevenueSeries(from, to, timeDetail))
                .build();
    }

    // GET /v1/statistics/applications/accepted/series?from=&to=&timeDetail=MONTH — ADMIN
    @GetMapping("/applications/accepted/series")
    public ApiResponse<List<AcceptedPointResponse>> getAcceptedSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") TimeDetail timeDetail) {

        return ApiResponse.<List<AcceptedPointResponse>>builder()
                .code(StatisticsResponseCode.GET_ACCEPTED_SERIES.getCode())
                .message(StatisticsResponseCode.GET_ACCEPTED_SERIES.getMessage())
                .data(statisticsService.getAcceptedSeries(from, to, timeDetail))
                .build();
    }

    // GET /v1/statistics/top-tutors?limit=&from=&to= — ADMIN (from/to không bắt buộc)
    @GetMapping("/top-tutors")
    public ApiResponse<List<TopTutorResponse>> getTopTutors(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ApiResponse.<List<TopTutorResponse>>builder()
                .code(StatisticsResponseCode.GET_TOP_TUTORS.getCode())
                .message(StatisticsResponseCode.GET_TOP_TUTORS.getMessage())
                .data(statisticsService.getTopTutors(limit, from, to))
                .build();
    }
}
