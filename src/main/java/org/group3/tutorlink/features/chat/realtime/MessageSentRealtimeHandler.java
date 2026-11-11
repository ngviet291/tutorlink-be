package org.group3.tutorlink.features.chat.realtime;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.ws.handler.EventHandler;
import org.group3.tutorlink.common.ws.realtime.RealtimeEvent;
import org.group3.tutorlink.common.ws.realtime.RealtimeEventType;
import org.group3.tutorlink.common.ws.realtime.RealtimePublisher;
import org.group3.tutorlink.features.chat.event.MessageSent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageSentRealtimeHandler implements EventHandler<MessageSent> {

    private final RealtimePublisher publisher;

    @Override
    public Class<MessageSent> support() {
        return MessageSent.class;
    }

    @Override
    public void handle(MessageSent event) {
        RealtimeEvent<?> payload = RealtimeEvent.builder()
                .type(RealtimeEventType.MESSAGE_NEW)
                .data(event.getMessage())
                .build();
        publisher.toUser(event.getReceiverId(), payload);
        // Đồng bộ các tab/thiết bị khác của người gửi, FE dedupe theo message.id
        publisher.toUser(event.getSenderId(), payload);
    }
}