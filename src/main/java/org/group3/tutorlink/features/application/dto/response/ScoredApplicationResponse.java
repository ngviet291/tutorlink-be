package org.group3.tutorlink.features.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.application.enums.MatchWarning;

import java.util.List;
import java.util.Map;

/**
 * Một lượt ứng tuyển kèm điểm xếp hạng.
 * score/reason là null khi không chạy chấm điểm (<= 5 ứng viên).
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScoredApplicationResponse {
    private ApplicationResponse application;
    private Double score;
    //    private MatchBreakdown breakdown;
    private Map<String, Double> breakdown;
    private List<MatchWarning> warnings;
}