package org.wrj.haifa.ai.deerflow.graph.node;

import org.wrj.haifa.ai.deerflow.agent.ResearchDepth;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.ResearchOutputFormat;
import org.wrj.haifa.ai.deerflow.agent.ResearchTimeWindow;

import java.util.Locale;
import java.util.Map;

final class ResearchNodeStateSupport {

    private ResearchNodeStateSupport() {
    }

    static ResearchOptions researchOptions(Object raw) {
        if (raw instanceof ResearchOptions options) {
            return options;
        }
        if (raw instanceof Map<?, ?> map) {
            return new ResearchOptions(
                    enumValue(ResearchDepth.class, map.get("depth")),
                    enumValue(ResearchTimeWindow.class, map.get("timeWindow")),
                    integerValue(map.get("maxSources")),
                    booleanValue(map.get("requireCitations")),
                    enumValue(ResearchOutputFormat.class, map.get("outputFormat"))
            );
        }
        return ResearchOptions.defaults();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, Object raw) {
        if (enumType.isInstance(raw)) {
            return enumType.cast(raw);
        }
        if (raw instanceof String value && !value.isBlank()) {
            try {
                return Enum.valueOf(enumType, value.trim()
                        .replace('-', '_')
                        .toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer integerValue(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String value && !value.isBlank()) {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Boolean booleanValue(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof String value && !value.isBlank()) {
            if ("true".equalsIgnoreCase(value.trim())) {
                return true;
            }
            if ("false".equalsIgnoreCase(value.trim())) {
                return false;
            }
        }
        return null;
    }
}
