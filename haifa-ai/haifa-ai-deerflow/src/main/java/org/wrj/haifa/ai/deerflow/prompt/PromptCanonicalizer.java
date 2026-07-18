package org.wrj.haifa.ai.deerflow.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;

public class PromptCanonicalizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String canonicalizeText(String text) {
        if (text == null) {
            return "";
        }
        // Normalize CRLF / CR to LF
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        // Trim trailing whitespace per line
        String[] lines = normalized.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(lines[i].stripTrailing());
        }
        return sb.toString();
    }

    public static String canonicalizeToolDefinitions(List<ModelToolDefinition> toolDefinitions) {
        if (toolDefinitions == null || toolDefinitions.isEmpty()) {
            return "";
        }
        List<ModelToolDefinition> sorted = new ArrayList<>(toolDefinitions);
        sorted.sort((a, b) -> {
            String nameA = a.name() == null ? "" : a.name();
            String nameB = b.name() == null ? "" : b.name();
            return nameA.compareTo(nameB);
        });

        StringBuilder sb = new StringBuilder();
        for (ModelToolDefinition def : sorted) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("tool: ").append(def.name() == null ? "" : def.name()).append("\n");
            sb.append("desc: ").append(canonicalizeText(def.description())).append("\n");
            sb.append("schema: ").append(canonicalizeJsonSchema(def.inputSchema()));
        }
        return sb.toString();
    }

    public static String canonicalizeJsonSchema(String schemaJson) {
        if (!StringUtils.hasText(schemaJson)) {
            return "{}";
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(schemaJson);
            JsonNode sortedNode = sortJsonNodeKeys(node);
            return OBJECT_MAPPER.writeValueAsString(sortedNode);
        } catch (Exception e) {
            return schemaJson.trim();
        }
    }

    private static JsonNode sortJsonNodeKeys(JsonNode node) {
        if (node == null) {
            return OBJECT_MAPPER.nullNode();
        }
        if (node.isObject()) {
            ObjectNode sortedObject = OBJECT_MAPPER.createObjectNode();
            Map<String, JsonNode> keyMap = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                keyMap.put(field.getKey(), sortJsonNodeKeys(field.getValue()));
            }
            keyMap.forEach(sortedObject::set);
            return sortedObject;
        } else if (node.isArray()) {
            ArrayNode sortedArray = OBJECT_MAPPER.createArrayNode();
            for (JsonNode element : node) {
                sortedArray.add(sortJsonNodeKeys(element));
            }
            return sortedArray;
        } else {
            return node;
        }
    }

    public static String sha256Hex(String input) {
        if (input == null) {
            input = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

    public static String shortHash(String fullHash) {
        if (fullHash == null || fullHash.isBlank()) {
            return "";
        }
        return fullHash.length() > 12 ? fullHash.substring(0, 12) : fullHash;
    }
}
