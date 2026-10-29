package org.group3.tutorlink.common.ws.handler;


import org.group3.tutorlink.common.ws.event.DomainEvent;

public interface EventHandler<T extends DomainEvent> {

    Class<T> support();

    void handle(T event);
}