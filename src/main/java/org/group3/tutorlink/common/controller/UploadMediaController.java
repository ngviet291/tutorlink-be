package org.group3.tutorlink.common.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.dto.response.UploadMediaResponse;
import org.group3.tutorlink.common.enums.CommonResponseCode;
import org.group3.tutorlink.common.enums.MediaType;
import org.group3.tutorlink.common.service.MediaStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/upload-media")
@RequiredArgsConstructor
public class UploadMediaController {

    private final MediaStorageService mediaStorageService;

    @PostMapping("/{chat-channel-id}/{media-type}")
    public ApiResponse<UploadMediaResponse> uploadMedia(
            @PathVariable("chat-channel-id") String chatChannelId,
            @PathVariable("media-type") @Valid MediaType mediaType,
            @RequestPart("file") MultipartFile file)  {

        return ApiResponse.<UploadMediaResponse>builder()
                .code(CommonResponseCode.UPLOAD_MEDIA_SUCCESS.getCode())
                .message(CommonResponseCode.UPLOAD_MEDIA_SUCCESS.getMessage())
                .data(mediaStorageService.uploadSync(file, chatChannelId, mediaType))
                .build();
    }


}
