package org.wrj.haifa.ai.deerflow.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResearchOutputFormat {
    ANSWER,
    REPORT;

    @JsonCreator
    public static ResearchOutputFormat fromString(String value) {
        if (value == null) {
            return ANSWER;
        }
        return switch (value.toLowerCase()) {
            case "answer" -> ANSWER;
            case "report" -> REPORT;
            default -> valueOf(value.toUpperCase());
        };
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
