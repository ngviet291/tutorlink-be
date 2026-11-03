package org.group3.tutorlink.features.tracking.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum ClientEventType {
    PAGE_VIEW("page_view"),
    APPLICATION_LIST_OPENED("application_list_opened"),
    TUTOR_PROFILE_VIEWED("tutor_profile_viewed"),
    TUTOR_FAVORITED("tutor_favorited"),
    SEARCH_PERFORMED("search_performed"),
    POST_FORM_STEP("post_form_step"),
    CLIENT_ERROR("client_error"),
    UNKNOWN("unknown");

    private final String code;

    ClientEventType(String code) {
        this.code = code;
    }

    public static Optional<ClientEventType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst();
    }
}