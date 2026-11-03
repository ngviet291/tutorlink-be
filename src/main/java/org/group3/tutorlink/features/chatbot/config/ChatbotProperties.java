package org.group3.tutorlink.features.chatbot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.group3.tutorlink.features.post.enums.PostType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** Toàn bộ cấu hình của chatbot nằm ở application.properties, prefix "app.chatbot". */
@Data
@Validated
@ConfigurationProperties(prefix = "app.chatbot")
public class ChatbotProperties {

    /** Số bài đăng tối đa trả về cho guest. */
    @Min(0)
    private int guestPostLimit;

    /** Số bài đăng tối đa trả về cho thành viên. */
    @Min(1)
    private int memberPostLimit;

    /** Lấy dư bao nhiêu bài từ DB rồi mới xếp theo khu vực. */
    @Min(1)
    private int candidatePoolSize;

    /** Chỉ gửi bấy nhiêu tin nhắn gần nhất cho Gemini để prompt không phình to. */
    @Min(1)
    private int historyLimit;

    @Min(10)
    private int conversationTitleMaxLength;

    @NotNull
    private PostType defaultPostType;

    @NotEmpty
    private List<String> provinces;

    @NotNull
    @Valid
    private ChatbotPrompts prompts;
}
