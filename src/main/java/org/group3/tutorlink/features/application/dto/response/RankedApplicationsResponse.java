package org.group3.tutorlink.features.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RankedApplicationsResponse {
    private boolean ranked;                          // true: > 5 ứng viên và đã chấm điểm
    private List<ScoredApplicationResponse> top;     // tối đa 5 gia sư phù hợp nhất
    private List<ScoredApplicationResponse> others;   // phần còn lại, sắp theo điểm giảm dần
}