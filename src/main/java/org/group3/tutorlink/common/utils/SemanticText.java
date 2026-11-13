package org.group3.tutorlink.common.utils;

import java.util.StringJoiner;

public final class SemanticText {
    private final StringJoiner lines = new StringJoiner("\n");

    public static SemanticText create() {
        return new SemanticText();
    }

    public SemanticText line(String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + ": " + value);
        }
        return this;
    }

    public String build() {
        return lines.toString();
    }
}