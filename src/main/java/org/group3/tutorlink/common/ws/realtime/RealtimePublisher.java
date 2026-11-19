package org.group3.tutorlink.common.ws.realtime;

import java.util.UUID;

public interface RealtimePublisher {

    void toUser(UUID userId, RealtimeEvent<?> event);

    void toTopic(String destination, RealtimeEvent<?> event);
}