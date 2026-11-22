package org.group3.tutorlink.features.chat.dto.response;

import lombok.*;
import org.group3.tutorlink.features.chat.enums.MediaType;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {
    private UUID id;
    private String publicId;
    private String url;
    private MediaType mediaType;
}