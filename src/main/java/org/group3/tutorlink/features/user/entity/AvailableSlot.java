package org.group3.tutorlink.features.user.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class AvailableSlot {

    private DayOfWeek dayOfWeek;

    private Instant startTime;

    private Instant endTime;
}