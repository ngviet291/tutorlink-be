package org.group3.tutorlink.features.chat.mapper;


import org.group3.tutorlink.features.chat.dto.request.MediaRequest;
import org.group3.tutorlink.features.chat.dto.response.MediaResponse;
import org.group3.tutorlink.features.chat.dto.response.MessageResponse;
import org.group3.tutorlink.features.chat.entity.Media;
import org.group3.tutorlink.features.chat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    // id, message, user được set trong service
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "type", source = "mediaType")
    Media toMedia(MediaRequest request);

    @Mapping(target = "mediaType", source = "type")
    MediaResponse toMediaResponse(Media media);

    @Mapping(target = "chatChannelId", source = "chatChannel.id")
    @Mapping(target = "senderId", source = "sender.id")
    MessageResponse toMessageResponse(Message message); // Set<Media> -> List<MediaResponse> tự sinh
}