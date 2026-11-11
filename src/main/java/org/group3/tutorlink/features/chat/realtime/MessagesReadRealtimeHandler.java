package org.group3.tutorlink.features.chat.realtime;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.ws.handler.EventHandler;
import org.group3.tutorlink.common.ws.realtime.RealtimeEvent;
import org.group3.tutorlink.common.ws.realtime.RealtimeEventType;
import org.group3.tutorlink.common.ws.realtime.RealtimePublisher;
import org.group3.tutorlink.features.chat.event.MessagesRead;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessagesReadRealtimeHandler implements EventHandler<MessagesRead> {

    private final RealtimePublisher publisher;

    public record Payload(UUID channelId, UUID readerId, Instant readAt) {
    }

    @Override
    public Class<MessagesRead> support() {
        return MessagesRead.class;
    }

    @Override
    public void handle(MessagesRead event) {
        publisher.toUser(event.getChannelId(), RealtimeEvent.builder()
                .type(RealtimeEventType.MESSAGE_READ)
                .data(new Payload(event.getChannelId(), event.getReaderId(), event.getReadAt()))
                .build());
    }
}