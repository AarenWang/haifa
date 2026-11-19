package org.wrj.haifa.ai.utilitymcp.tool;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolArguments;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

public class TimeService {

    private final Clock clock;

    public TimeService(Clock clock) {
        this.clock = clock;
    }

    public UtilityResult now(Map<String, Object> arguments) {
        String timezone = new ToolArguments(arguments).optionalString("timezone", "UTC", 64);
        ZoneId zone = zone(timezone);
        return result(clock.instant(), zone);
    }

    public UtilityResult convert(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String raw = args.requiredString("dateTime", 64);
        ZoneId from = zone(args.requiredString("fromTimezone", 64));
        ZoneId to = zone(args.requiredString("toTimezone", 64));
        String overlapPolicy = args.enumValue("overlapPolicy", "REJECT",
                java.util.Set.of("REJECT", "EARLIER_OFFSET", "LATER_OFFSET"));

        Instant instant;
        try {
            if (raw.endsWith("Z") || raw.matches(".*[+-]\\d{2}:\\d{2}$")) {
                instant = OffsetDateTime.parse(raw).toInstant();
            }
            else {
                LocalDateTime local = LocalDateTime.parse(raw);
                List<ZoneOffset> offsets = from.getRules().getValidOffsets(local);
                if (offsets.isEmpty()) {
                    ZoneOffsetTransition transition = from.getRules().getTransition(local);
                    throw UtilityToolException.invalid("dateTime falls in a daylight-saving gap"
                            + (transition == null ? "" : " near " + transition.getDateTimeAfter()));
                }
                if (offsets.size() > 1 && "REJECT".equals(overlapPolicy)) {
                    throw UtilityToolException.invalid("dateTime is ambiguous during a daylight-saving overlap; choose overlapPolicy");
                }
                ZoneOffset offset = offsets.size() == 1 || "EARLIER_OFFSET".equals(overlapPolicy)
                        ? offsets.getFirst() : offsets.getLast();
                instant = local.toInstant(offset);
            }
        }
        catch (UtilityToolException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw UtilityToolException.invalid("dateTime must be an ISO-8601 local or offset date-time");
        }

        Map<String, Object> data = new LinkedHashMap<>(result(instant, to).data());
        ZonedDateTime source = instant.atZone(from);
        data.put("sourceLocalDateTime", source.toLocalDateTime().toString());
        data.put("sourceTimezone", from.getId());
        data.put("sourceUtcOffset", source.getOffset().toString());
        return UtilityResult.local(data);
    }

    private static UtilityResult result(Instant instant, ZoneId zone) {
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

    private static ZoneId zone(String value) {
        try {
            return ZoneId.of(value);
        }
        catch (RuntimeException ex) {
            throw UtilityToolException.invalid("timezone must be a valid IANA timezone");
        }
    }
}
