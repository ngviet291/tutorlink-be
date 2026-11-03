package org.group3.tutorlink.features.chatbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.chatbot.dto.request.ChatMessageRequest;
import org.group3.tutorlink.features.chatbot.dto.response.ChatMessageResponse;
import org.group3.tutorlink.features.chatbot.dto.response.ChatResponse;
import org.group3.tutorlink.features.chatbot.dto.response.ConversationResponse;
import org.group3.tutorlink.features.chatbot.enums.ChatbotResponseCode;
import org.group3.tutorlink.features.chatbot.service.ChatbotService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    // POST /v1/chatbot/messages - Guest + Student + Tutor (đã mở public trong SecurityConfig)
    @PostMapping("/messages")
    public ApiResponse<ChatResponse> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal Jwt jwt            // null nếu là guest
    ) {
        UUID userId = (jwt == null) ? null : UUID.fromString(jwt.getSubject());
        return ApiResponse.<ChatResponse>builder()
                .code(ChatbotResponseCode.MESSAGE_SENT.getCode())
                .message(ChatbotResponseCode.MESSAGE_SENT.getMessage())
                .data(chatbotService.sendMessage(request, userId))
                .build();
    }

    // GET /v1/chatbot/conversations - đã đăng nhập
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> getConversations() {
        return ApiResponse.<List<ConversationResponse>>builder()
                .code(ChatbotResponseCode.GET_CONVERSATIONS.getCode())
                .message(ChatbotResponseCode.GET_CONVERSATIONS.getMessage())
                .data(chatbotService.getMyConversations())
                .build();
    }

    // GET /v1/chatbot/conversations/{id}/messages - đã đăng nhập
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> getMessages(@PathVariable UUID id) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .code(ChatbotResponseCode.GET_MESSAGES.getCode())
                .message(ChatbotResponseCode.GET_MESSAGES.getMessage())
                .data(chatbotService.getMessages(id))
                .build();
    }

    // DELETE /v1/chatbot/conversations/{id} - đã đăng nhập
    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> deleteConversation(@PathVariable UUID id) {
        chatbotService.deleteConversation(id);
        return ApiResponse.<Void>builder()
                .code(ChatbotResponseCode.CONVERSATION_DELETED.getCode())
                .message(ChatbotResponseCode.CONVERSATION_DELETED.getMessage())
                .build();
    }
}
