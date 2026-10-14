package org.group3.tutorlink.features.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.chat.dto.request.SendMessageRequest;
import org.group3.tutorlink.features.chat.dto.response.MessageResponse;
import org.group3.tutorlink.features.chat.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ApiResponse<MessageResponse> send(@RequestBody @Valid SendMessageRequest req) {
        return ApiResponse.<MessageResponse>builder()
                .code("200")
                .message("Message sent successfully")
                .data(chatService.sendMessage(req))
                .build();
    }
}
