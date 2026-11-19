package org.group3.tutorlink.features.tracking.mapper;

import org.group3.tutorlink.features.tracking.dto.request.TrackEventItem;
import org.group3.tutorlink.features.tracking.dto.request.TrackEventsRequest;
import org.group3.tutorlink.features.tracking.entity.UserEvent;
import org.group3.tutorlink.features.tracking.enums.ClientEventType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserEventMapper {


    @Mapping(target = "anonId", source = "request.anonId")
    @Mapping(target = "sessionId", source = "request.sessionId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "name", source = "item.name", qualifiedByName = "toEventType")
    @Mapping(target = "props", source = "item.props")
    @Mapping(target = "occurredAt", source = "item.ts")
    @Mapping(target = "receivedAt", source = "receivedAt")
    @Mapping(target = "id", source = "eventId")
    UserEvent toUserEvent(TrackEventItem item,
                       TrackEventsRequest request,
                       UUID userId,
                       Instant receivedAt, UUID eventId);

    @Named("toEventType")
    default ClientEventType toEventType(String name) {
        return ClientEventType.fromCode(name).orElse(ClientEventType.UNKNOWN);
    }
}