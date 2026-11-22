package org.group3.tutorlink.features.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotRequest {
    @NotNull
    private DayOfWeek dayOfWeek;
    @NotNull private Instant startTime;
    @NotNull private Instant endTime;
}