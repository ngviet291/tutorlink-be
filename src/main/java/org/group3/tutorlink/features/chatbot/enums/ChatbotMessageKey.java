package org.group3.tutorlink.features.chatbot.enums;

import lombok.Getter;

@Getter
public enum ChatbotMessageKey {
    ASK_SUBJECT("chatbot.reply.ask-subject"),
    NOT_UNDERSTOOD("chatbot.reply.not-understood"),
    NO_RESULT("chatbot.reply.no-result"),
    RESULT_RELAXED("chatbot.reply.result-relaxed"),
    RESULT_FOR_GUEST("chatbot.reply.result-guest"),
    RESULT_FOR_MEMBER("chatbot.reply.result-member"),
    HISTORY_EMPTY("chatbot.history.empty"),
    HISTORY_USER("chatbot.history.user"),
    HISTORY_ASSISTANT("chatbot.history.assistant");

    private final String code;

    ChatbotMessageKey(String code) {
        this.code = code;
    }
}