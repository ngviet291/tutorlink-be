package org.group3.tutorlink.features.chatbot.ai;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.config.PromptLoader;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import com.google.genai.errors.ApiException;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.chatbot.exception.AiOverloadedException;
import org.group3.tutorlink.features.chatbot.exception.AiUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AIClient {

    private final ChatClient chatClient;
    private final String userPrompt;

    public AIClient(ChatClient.Builder builder,
                    ChatbotProperties properties,
                    PromptLoader promptLoader) {
        // Prompt chỉ đọc file 1 lần lúc khởi động
        String systemPrompt = promptLoader.load(properties.getPrompts().getSystem());
        this.userPrompt = promptLoader.load(properties.getPrompts().getUser());

        String provinces = formatProvinces(properties.getProvinces());

        this.chatClient = builder
                .defaultSystem(system -> system.text(systemPrompt).param("provinces", provinces))
                .build();
    }

    public AssistantTurn analyze(String history, String message) {
        long startedAt = System.currentTimeMillis();
        try {
            AssistantTurn turn = ask(history, message);
            log.info("Gemini trả lời sau {} ms (intent={})", elapsedSince(startedAt), turn.getIntent());
            return turn;
        } catch (Exception e) {
            throw translate(e, startedAt);
        }
    }

    // 503 -> "máy chủ quá tải" (người dùng nên thử lại sau), lỗi khác -> "AI không khả dụng"
    private AppException translate(Exception error, long startedAt) {
        if (isOverloaded(error)) {
            log.warn("Gemini quá tải (HTTP 503) sau {} ms", elapsedSince(startedAt));
            return new AiOverloadedException();
        }
        log.error("Gemini lỗi sau {} ms", elapsedSince(startedAt), error);
        return new AiUnavailableException();
    }

    // Spring AI có thể bọc lỗi gốc của Gemini nên phải đi dọc chuỗi cause để tìm ServerException 503.
    private boolean isOverloaded(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ApiException api && api.code() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                return true;
            }
        }
        return false;
    }

    private AssistantTurn ask(String history, String message) {
        AssistantTurn turn = chatClient.prompt()
                .user(user -> user.text(userPrompt).param("history", history).param("message", message))
                .call()
                .entity(AssistantTurn.class);

        if (turn == null || turn.getIntent() == null) {
            throw new IllegalStateException("AI did not return an intent");
        }
        return turn;
    }

    // ["Ho Chi Minh City", "Ha Noi"] -> "\"Ho Chi Minh City\", \"Ha Noi\"" để đưa vào prompt
    private String formatProvinces(List<String> provinces) {
        return provinces.stream()
                .map(province -> "\"" + province + "\"")
                .collect(Collectors.joining(", "));
    }

    private long elapsedSince(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }
}