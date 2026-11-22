package org.group3.tutorlink.common.ws.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompRealtimePublisher implements RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void toUser(UUID userId, RealtimeEvent<?> event) {
        // Principal name của STOMP = JWT subject = userId
        messagingTemplate.convertAndSendToUser(
                userId.toString(), RealtimeDestinations.USER_EVENTS, event);
    }

    @Override
    public void toTopic(String destination, RealtimeEvent<?> event) {
        messagingTemplate.convertAndSend(destination, event);
    }
}