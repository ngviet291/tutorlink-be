package org.group3.tutorlink.features.chatbot.ai;

import com.google.genai.errors.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.config.PromptLoader;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.chatbot.exception.AiOverloadedException;
import org.group3.tutorlink.features.chatbot.exception.AiUnavailableException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Nhờ AI đọc tin nhắn của người dùng và cho biết họ muốn gì (intent + bộ lọc tìm bài đăng). */
@Slf4j
@Component
public class AIClient {

    // Hai mã này đều có nghĩa "thử lại sau": 503 máy chủ quá tải, 429 hết quota
    private static final Set<Integer> OVERLOADED_STATUS_CODES = Set.of(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            HttpStatus.TOO_MANY_REQUESTS.value());

    private final ChatClient chatClient;
    private final SubjectCatalog subjectCatalog;
    private final String systemPrompt;
    private final String userPrompt;
    private final String provinces;

    public AIClient(ChatClient.Builder builder,
                    ChatbotProperties properties,
                    PromptLoader promptLoader,
                    SubjectCatalog subjectCatalog) {
        // Prompt chỉ đọc file 1 lần lúc khởi động
        this.systemPrompt = promptLoader.load(properties.getPrompts().getSystem());
        this.userPrompt = promptLoader.load(properties.getPrompts().getUser());
        this.provinces = formatQuoted(properties.getProvinces());
        this.subjectCatalog = subjectCatalog;

        // Không dùng defaultSystem: danh sách môn học lấy từ DB nên phải gán lúc gọi
        // (dữ liệu mẫu nạp sau khi bean được tạo, nếu nạp ở constructor danh sách sẽ rỗng).
        this.chatClient = builder.build();
    }

    public AssistantTurn analyze(String history, String message) {
        long startedAt = System.currentTimeMillis();
        try {
            AssistantTurn turn = ask(history, message);
            log.info("AI trả lời sau {} ms (intent={})", elapsedSince(startedAt), turn.getIntent());
            return turn;
        } catch (Exception e) {
            throw translate(e, startedAt);
        }
    }

    private AssistantTurn ask(String history, String message) {
        String subjects = formatQuoted(subjectCatalog.names());

        AssistantTurn turn = chatClient.prompt()
                .system(system -> system.text(systemPrompt)
                        .param("provinces", provinces)
                        .param("subjects", subjects))
                .user(user -> user.text(userPrompt).param("history", history).param("message", message))
                .call()
                .entity(AssistantTurn.class);

        if (turn == null || turn.getIntent() == null) {
            throw new IllegalStateException("AI did not return an intent");
        }
        return turn;
    }

    // Quá tải / hết quota -> "thử lại sau", lỗi khác -> "AI không khả dụng"
    private AppException translate(Exception error, long startedAt) {
        Optional<ApiException> apiError = findApiException(error);
        if (apiError.isPresent() && OVERLOADED_STATUS_CODES.contains(apiError.get().code())) {
            log.warn("AI quá tải hoặc hết quota (HTTP {}) sau {} ms", apiError.get().code(), elapsedSince(startedAt));
            return new AiOverloadedException();
        }
        log.error("AI lỗi sau {} ms", elapsedSince(startedAt), error);
        return new AiUnavailableException();
    }

    // Spring AI có thể bọc lỗi gốc của Gemini nên phải đi dọc chuỗi cause để tìm ApiException.
    private Optional<ApiException> findApiException(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ApiException apiException) {
                return Optional.of(apiException);
            }
        }
        return Optional.empty();
    }

    // ["Toán", "Vật lý"] -> "\"Toán\", \"Vật lý\"" để đưa vào prompt
    private String formatQuoted(List<String> values) {
        return values.stream()
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(", "));
    }

    private long elapsedSince(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }
}