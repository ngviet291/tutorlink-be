package org.group3.tutorlink.features.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.chat.dto.request.SendMessageRequest;
import org.group3.tutorlink.features.chat.dto.response.ChatChannelResponse;
import org.group3.tutorlink.features.chat.dto.response.MessageResponse;
import org.group3.tutorlink.features.chat.enums.ChatResponseCode;
import org.group3.tutorlink.features.chat.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ApiResponse<MessageResponse> send(@RequestBody @Valid SendMessageRequest req) {
        return ApiResponse.<MessageResponse>builder()
                .code(ChatResponseCode.SEND_MESSAGE_SUCCESS.getCode())
                .message(ChatResponseCode.SEND_MESSAGE_SUCCESS.getMessage())
                .data(chatService.sendMessage(req))
                .build();
    }

    @GetMapping("/channels")
    public ApiResponse<CursorResponse<ChatChannelResponse>> getAllChannels(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<CursorResponse<ChatChannelResponse>>builder()
                .code(ChatResponseCode.GET_CHANNELS_SUCCESS.getCode())
                .message(ChatResponseCode.GET_CHANNELS_SUCCESS.getMessage())
                .data(chatService.getAllChatChannels(cursor, size))
                .build();
    }

    @GetMapping("/channels/{channelId}")
    public ApiResponse<ChatChannelResponse> getChannelById(@PathVariable UUID channelId) {
        return ApiResponse.<ChatChannelResponse>builder()
                .code(ChatResponseCode.GET_CHANNEL_SUCCESS.getCode())
                .message(ChatResponseCode.GET_CHANNEL_SUCCESS.getMessage())
                .data(chatService.getChatChannelById(channelId))
                .build();
    }

    @GetMapping("/channels/with/{userId}")
    public ApiResponse<ChatChannelResponse> getChannelWithUser(@PathVariable UUID userId) {
        return ApiResponse.<ChatChannelResponse>builder()
                .code(ChatResponseCode.GET_CHANNEL_SUCCESS.getCode())
                .message(ChatResponseCode.GET_CHANNEL_SUCCESS.getMessage())
                .data(chatService.getChatChannelWithUser(userId))
                .build();
    }

    @GetMapping("/channels/{channelId}/messages")
    public ApiResponse<CursorResponse<MessageResponse>> getAllMessages(
            @PathVariable UUID channelId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.<CursorResponse<MessageResponse>>builder()
                .code(ChatResponseCode.GET_MESSAGES_SUCCESS.getCode())
                .message(ChatResponseCode.GET_MESSAGES_SUCCESS.getMessage())
                .data(chatService.getAllMessages(channelId, cursor, size))
                .build();
    }

    @PatchMapping("/channels/{channelId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID channelId) {
        chatService.markChannelAsRead(channelId);
        return ApiResponse.<Void>builder()
                .code(ChatResponseCode.MARK_READ_SUCCESS.getCode())
                .message(ChatResponseCode.MARK_READ_SUCCESS.getMessage())
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.<Long>builder()
                .code(ChatResponseCode.GET_UNREAD_COUNT_SUCCESS.getCode())
                .message(ChatResponseCode.GET_UNREAD_COUNT_SUCCESS.getMessage())
                .data(chatService.getUnreadCount())
                .build();
    }
}