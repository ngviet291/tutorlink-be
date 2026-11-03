package org.group3.tutorlink.features.statistics.service;

import org.group3.tutorlink.features.statistics.dto.response.*;
import org.group3.tutorlink.features.statistics.enums.TimeDetail;

import java.time.LocalDate;
import java.util.List;

public interface StatisticsService {

    List<RevenuePointResponse> getRevenueSeries(LocalDate from, LocalDate to, TimeDetail timeDetail);

    List<AcceptedPointResponse> getAcceptedSeries(LocalDate from, LocalDate to, TimeDetail timeDetail);

    List<TopTutorResponse> getTopTutors(int limit, LocalDate from, LocalDate to);
}
