package org.wrj.haifa.ai.deerflow.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonMapper {

    private static final Logger log = LoggerFactory.getLogger(JsonMapper.class);

    private final ObjectMapper objectMapper;

    public JsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    public Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON metadata: {}. Returning empty map.", e.getMessage());
            return Map.of();
        }
    }

    public <T> String toJsonObject(T object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    public <T> T fromJsonObject(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
