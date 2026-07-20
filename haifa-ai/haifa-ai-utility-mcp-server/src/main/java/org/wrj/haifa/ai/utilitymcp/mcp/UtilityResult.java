package org.wrj.haifa.ai.utilitymcp.mcp;

import java.time.OffsetDateTime;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public record UtilityResult(Map<String, Object> data, Map<String, Object> meta) {

    public UtilityResult {
        data = data == null ? Map.of() : Map.copyOf(data);
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    public static UtilityResult local(Map<String, Object> data) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", "haifa-utility");
        meta.put("retrievedAt", OffsetDateTime.now().toString());
        meta.put("cached", false);
        meta.put("partial", false);
        return new UtilityResult(data, meta);
    }

    public static UtilityResult external(
            Map<String, Object> data,
            String source,
            URI sourceUrl,
            OffsetDateTime retrievedAt,
            boolean cached,
            boolean partial,
            String observedAt,
            Map<String, Object> units) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", source);
        if (sourceUrl != null) meta.put("sourceUrl", sourceUrl.toString());
        if (observedAt != null && !observedAt.isBlank()) meta.put("observedAt", observedAt);
        meta.put("retrievedAt", (retrievedAt == null ? OffsetDateTime.now() : retrievedAt).toString());
        if (units != null && !units.isEmpty()) meta.put("units", Map.copyOf(units));
        meta.put("cached", cached);
        meta.put("partial", partial);
        return new UtilityResult(data, meta);
    }

    public Map<String, Object> asMap() {
        return Map.of("data", data, "meta", meta);
    }
}
