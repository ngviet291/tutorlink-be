package org.group3.tutorlink.common.ws.handler;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.ws.event.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EventDispatcher {

    private final Map<Class<?>, List<EventHandler<?>>> handlers;

    public EventDispatcher(List<EventHandler<?>> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.groupingBy(EventHandler::support));
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void dispatch(T event) {
        List<EventHandler<?>> matched = handlers.getOrDefault(event.getClass(), List.of());
        for (EventHandler<?> handler : matched) {
            try {
                ((EventHandler<T>) handler).handle(event);
            } catch (Exception e) {
                // Một handler lỗi không được chặn các handler còn lại
                log.error("Handler {} failed for event {}",
                        handler.getClass().getSimpleName(), event.getClass().getSimpleName(), e);
            }
        }
    }
}