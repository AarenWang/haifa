package org.wrj.haifa.ai.deerflow.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResearchTimeWindow {
    LATEST,
    LAST_30_DAYS,
    LAST_YEAR,
    ALL_TIME;

    @JsonCreator
    public static ResearchTimeWindow fromString(String value) {
        if (value == null) {
            return LATEST;
        }
        return switch (value.toLowerCase()) {
            case "latest" -> LATEST;
            case "last_30_days", "last-30-days" -> LAST_30_DAYS;
            case "last_year", "last-year" -> LAST_YEAR;
            case "all_time", "all-time" -> ALL_TIME;
            default -> valueOf(value.toUpperCase());
        };
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
