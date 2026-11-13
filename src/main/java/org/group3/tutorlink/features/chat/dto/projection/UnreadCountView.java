package org.group3.tutorlink.features.chat.dto.projection;

import java.util.UUID;

public interface UnreadCountView {
    UUID getChannelId();
    long getTotal();
}