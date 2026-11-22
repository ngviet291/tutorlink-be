package org.group3.tutorlink.features.statistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopTutorResponse {
    private UUID tutorId;
    private String fullName;
    private double averageRating;
    private long acceptedCount;
}
