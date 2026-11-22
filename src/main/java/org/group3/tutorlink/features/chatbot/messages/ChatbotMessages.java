package org.group3.tutorlink.features.chatbot.messages;

import org.group3.tutorlink.features.chatbot.enums.ChatbotMessageKey;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

// Đọc từ chatbot-messages.properties.
@Component
public class ChatbotMessages {

    private static final String BUNDLE = "chatbot-messages";

    private final MessageSource messageSource;

    public ChatbotMessages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename(BUNDLE);
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        this.messageSource = source;
    }

    public String get(ChatbotMessageKey key, Object... args) {
        return messageSource.getMessage(key.getCode(), args, Locale.ROOT);
    }
}
