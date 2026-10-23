package org.group3.tutorlink.features.statistics.service;

import org.group3.tutorlink.features.statistics.dto.response.AcceptedApplicationsResponse;
import org.group3.tutorlink.features.statistics.dto.response.RevenueStatisticsResponse;
import org.group3.tutorlink.features.statistics.dto.response.TopTutorResponse;

import java.time.LocalDate;
import java.util.List;

public interface StatisticsService {

    RevenueStatisticsResponse getRevenueStatistics(LocalDate from, LocalDate to);

    AcceptedApplicationsResponse getAcceptedApplicationsCount();

    List<TopTutorResponse> getTopTutors(int limit);
}
