package org.wrj.haifa.ai.utilitymcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

final class JsonSupport {

    private JsonSupport() {}

    static JsonNode required(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            throw malformed("missing field: " + field);
        }
        return value;
    }

    static String text(JsonNode node, String field) {
        JsonNode value = required(node, field);
        if (!value.isValueNode()) throw malformed("field is not scalar: " + field);
        return value.asText();
    }

    static String optionalText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    static Map<String, Object> scalarObject(JsonNode object) {
        if (object == null || !object.isObject()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        object.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isNumber()) result.put(entry.getKey(), value.numberValue());
            else if (value.isBoolean()) result.put(entry.getKey(), value.booleanValue());
            else if (value.isTextual()) result.put(entry.getKey(), value.textValue());
        });
        return result;
    }

    static List<Object> scalarArray(JsonNode array, int limit) {
        if (array == null || !array.isArray()) return List.of();
        List<Object> result = new ArrayList<>();
        for (JsonNode value : array) {
            if (result.size() >= limit) break;
            if (value.isNumber()) result.add(value.numberValue());
            else if (value.isBoolean()) result.add(value.booleanValue());
            else if (value.isTextual()) result.add(value.textValue());
            else if (value.isNull()) result.add(null);
        }
        return result;
    }

    static UtilityToolException malformed(String detail) {
        return new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                "Upstream returned an invalid response (" + detail + ")", true);
    }
}
