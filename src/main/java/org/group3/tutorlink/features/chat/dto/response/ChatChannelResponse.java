package org.group3.tutorlink.features.chat.dto.response;

import lombok.*;
import org.group3.tutorlink.features.chat.enums.MediaType;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatChannelResponse {
    private UUID id;
    private Instant lastMessageAt;
    private ParticipantResponse partner; //

    private String lastMessageContent;   // null nếu tin chỉ có media
    private MediaType lastMessageMediaType; // để FE hiện "📷 Hình ảnh"
    private UUID lastMessageSenderId;  // để hiện "Bạn: ..."
    private long unreadCount;            // số tin chưa đọc của user hiện tại
}