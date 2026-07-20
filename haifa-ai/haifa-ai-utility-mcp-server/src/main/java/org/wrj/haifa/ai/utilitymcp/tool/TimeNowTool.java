package org.wrj.haifa.ai.utilitymcp.tool;

import io.modelcontextprotocol.spec.McpSchema;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityTool;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

public class TimeNowTool implements UtilityTool {

    private final Clock clock;

    public TimeNowTool(Clock clock) {
        this.clock = clock;
    }

    @Override
    public McpSchema.Tool contract() {
        Map<String, Object> timezone = Map.of(
                "type", "string",
                "minLength", 1,
                "maxLength", 64,
                "description", "IANA timezone, for example Asia/Shanghai");
        McpSchema.JsonSchema input = new McpSchema.JsonSchema(
                "object", Map.of("timezone", timezone), List.of(), false, null, null);
        Map<String, Object> output = Map.of("type", "object");
        return McpSchema.Tool.builder()
                .name("time_now")
                .title("Current time")
                .description("Return the current instant and local time in a validated IANA timezone. Defaults to UTC.")
                .inputSchema(input)
                .outputSchema(output)
                .annotations(new McpSchema.ToolAnnotations("Current time", true, false, true, false, false))
                .build();
    }

    @Override
    public UtilityResult execute(Map<String, Object> arguments) {
        String timezone = stringArg(arguments, "timezone", "UTC");
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);
        }
        catch (RuntimeException ex) {
            throw UtilityToolException.invalid("timezone must be a valid IANA timezone");
        }
        Instant instant = clock.instant();
        ZonedDateTime local = instant.atZone(zone);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("instant", instant.toString());
        data.put("localDateTime", local.toLocalDateTime().toString());
        data.put("localDate", local.toLocalDate().toString());
        data.put("localTime", local.toLocalTime().toString());
        data.put("timezone", zone.getId());
        data.put("utcOffset", local.getOffset().toString());
        data.put("dayOfWeek", local.getDayOfWeek().name());
        return UtilityResult.local(data);
    }

    private static String stringArg(Map<String, Object> arguments, String name, String defaultValue) {
        if (arguments == null || arguments.get(name) == null) {
            return defaultValue;
        }
        Object value = arguments.get(name);
        if (!(value instanceof String text) || text.isBlank() || text.length() > 64) {
            throw UtilityToolException.invalid(name + " must be a non-empty string of at most 64 characters");
        }
        return text;
    }
}
