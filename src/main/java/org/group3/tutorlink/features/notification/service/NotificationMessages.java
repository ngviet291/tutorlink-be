package org.group3.tutorlink.features.notification.service;

import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;

/** Lấy tiêu đề, nội dung thông báo từ messages.properties (không hardcode chữ trong Java). */
@Component
public class NotificationMessages {

    private static final String KEY_PREFIX = "notification.";
    private static final String TITLE_SUFFIX = ".title";
    private static final String CONTENT_SUFFIX = ".content";

    private final MessageSource messageSource;
    private final Locale locale;
    private final String moneyFormat;

    public NotificationMessages(MessageSource messageSource,
                                @Value("${app.notification.locale}") String localeTag,
                                @Value("${app.notification.money-format}") String moneyFormat) {
        this.messageSource = messageSource;
        this.locale = Locale.forLanguageTag(localeTag);
        this.moneyFormat = moneyFormat;
        checkAllKeysDefined();
    }

    public String title(NotificationMessage message) {
        return messageSource.getMessage(KEY_PREFIX + message.getKey() + TITLE_SUFFIX, null, locale);
    }

    /** Điền {0}, {1}... theo thứ tự args. Truyền chuỗi để số không bị tự thêm dấu phân cách. */
    public String content(NotificationMessage message, Object... args) {
        Object[] texts = Arrays.stream(args).map(arg -> String.valueOf(arg)).toArray();
        return messageSource.getMessage(KEY_PREFIX + message.getKey() + CONTENT_SUFFIX, texts, locale);
    }

    public String money(BigDecimal amount) {
        return String.format(locale, moneyFormat, amount);
    }

    private void checkAllKeysDefined() {
        for (NotificationMessage message : NotificationMessage.values()) {
            title(message);
            messageSource.getMessage(KEY_PREFIX + message.getKey() + CONTENT_SUFFIX, new Object[0], locale);
        }
    }
}