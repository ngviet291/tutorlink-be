package org.group3.tutorlink.features.tracking.mapper;

import org.group3.tutorlink.features.tracking.entity.SystemLog;
import org.group3.tutorlink.features.tracking.event.SystemActionOccurred;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SystemLogMapper {

    @Mapping(target = "id", source = "id")
    SystemLog toSystemLog(SystemActionOccurred event, UUID id);
}