package org.group3.tutorlink.common.ws.realtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
@Getter
@AllArgsConstructor
@Builder
public class RealtimeEvent<T> {

    private final RealtimeEventType type;
    @Builder.Default
    private final Instant occurredAt = Instant.now();
    private final T data;

}