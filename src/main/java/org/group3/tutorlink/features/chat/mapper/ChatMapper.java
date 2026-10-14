package org.group3.tutorlink.features.chat.mapper;


import org.group3.tutorlink.features.chat.dto.request.MediaRequest;
import org.group3.tutorlink.features.chat.dto.response.ChatChannelResponse;
import org.group3.tutorlink.features.chat.dto.response.MediaResponse;
import org.group3.tutorlink.features.chat.dto.response.MessageResponse;
import org.group3.tutorlink.features.chat.dto.response.ParticipantResponse;
import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.group3.tutorlink.features.chat.entity.Media;
import org.group3.tutorlink.features.chat.entity.Message;
import org.group3.tutorlink.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

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

    ParticipantResponse toParticipant(User user);

    default ChatChannelResponse toChannelResponse(ChatChannel channel, UUID currentUserId) {
        User partner = channel.getSender().getId().equals(currentUserId)
                ? channel.getReceiver()
                : channel.getSender();
        return ChatChannelResponse.builder()
                .id(channel.getId())
                .lastMessageAt(channel.getLastMessageAt())
                .partner(toParticipant(partner))
                .build();
    }
}