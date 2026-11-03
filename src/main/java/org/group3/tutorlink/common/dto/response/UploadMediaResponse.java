package org.group3.tutorlink.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.common.enums.MediaType;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UploadMediaResponse {
    private String publicId;
    private String url;
    private MediaType mediaType;

}