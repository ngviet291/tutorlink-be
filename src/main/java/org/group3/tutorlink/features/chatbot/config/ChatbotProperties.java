package org.group3.tutorlink.features.chatbot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.group3.tutorlink.features.post.enums.PostType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "app.chatbot")
public class ChatbotProperties {

    // Số bài đăng tối đa trả về cho guest / thành viên
    @Min(0)
    private int guestPostLimit;

    @Min(1)
    private int memberPostLimit;

    // Lấy dư bao nhiêu bài từ DB rồi mới xếp theo khu vực
    @Min(1)
    private int candidatePoolSize;

    // Chỉ gửi bấy nhiêu tin nhắn gần nhất cho AI
    @Min(1)
    private int historyLimit;

    @Min(10)
    private int conversationTitleMaxLength;

    @NotNull
    private PostType defaultPostType;

    // Danh sách môn học đưa vào prompt được cache bấy nhiêu lâu
    @NotNull
    private Duration subjectsCacheTtl;

    @NotNull
    @Valid
    private ChatbotPrompts prompts;
}