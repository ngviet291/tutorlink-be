package org.group3.tutorlink.features.chatbot.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.chatbot.enums.ChatIntent;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Kết quả AI phân tích tin nhắn: intent, câu trả lời (nếu không phải tìm kiếm) và bộ lọc tìm bài đăng
public class AssistantTurn {
    private ChatIntent intent;
    private String message;
    private PostSearchFilters filters;
}
