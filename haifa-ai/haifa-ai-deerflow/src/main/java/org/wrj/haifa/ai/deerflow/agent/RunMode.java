package org.wrj.haifa.ai.deerflow.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RunMode {
    CHAT,
    RESEARCH;

    @JsonCreator
    public static RunMode fromString(String value) {
        if (value == null) {
            return CHAT;
        }
        return switch (value.toLowerCase()) {
            case "chat" -> CHAT;
            case "research" -> RESEARCH;
            default -> valueOf(value.toUpperCase());
        };
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
