package org.wrj.haifa.ai.deerflow.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResearchDepth {
    QUICK,
    STANDARD,
    DEEP;

    @JsonCreator
    public static ResearchDepth fromString(String value) {
        if (value == null) {
            return STANDARD;
        }
        return switch (value.toLowerCase()) {
            case "quick" -> QUICK;
            case "standard" -> STANDARD;
            case "deep" -> DEEP;
            default -> valueOf(value.toUpperCase());
        };
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
