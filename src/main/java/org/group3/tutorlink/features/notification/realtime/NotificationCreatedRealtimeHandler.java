package org.group3.tutorlink.features.notification.realtime;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.ws.handler.EventHandler;
import org.group3.tutorlink.common.ws.realtime.RealtimeEvent;
import org.group3.tutorlink.common.ws.realtime.RealtimeEventType;
import org.group3.tutorlink.common.ws.realtime.RealtimePublisher;
import org.group3.tutorlink.features.notification.event.NotificationCreated;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCreatedRealtimeHandler implements EventHandler<NotificationCreated> {

    private final RealtimePublisher publisher;

    @Override
    public Class<NotificationCreated> support() {
        return NotificationCreated.class;
    }

    @Override
    public void handle(NotificationCreated event) {
        RealtimeEvent<?> payload = RealtimeEvent.builder()
                .type(RealtimeEventType.NOTIFICATION_NEW)
                .data(event.getNotification())
                .build();
        publisher.toUser(event.getRecipientId(), payload);
    }
}