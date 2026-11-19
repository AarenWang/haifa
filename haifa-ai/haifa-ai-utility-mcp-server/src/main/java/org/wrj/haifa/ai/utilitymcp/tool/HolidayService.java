package org.wrj.haifa.ai.utilitymcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolArguments;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;
import org.wrj.haifa.ai.utilitymcp.provider.JsonProvider;
import org.wrj.haifa.ai.utilitymcp.provider.ProviderPayload;

public class HolidayService {

    private static final int MIN_YEAR = 1970;
    private static final int MAX_YEAR = 2100;
    private static final Set<String> LIMITED_WORKDAY_COUNTRIES = Set.of("CN");
    private final JsonProvider provider;
    private final Clock clock;

    public HolidayService(JsonProvider provider) {
        this(provider, Clock.systemUTC());
    }

    public HolidayService(JsonProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    public UtilityResult list(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String country = country(args.requiredString("countryCode", 2));
        int year = args.intValue("year", LocalDate.now(clock).getYear(), MIN_YEAR, MAX_YEAR);
        Calendar calendar = load(country, year);
        return result(Map.of("countryCode", country, "year", year, "holidays", calendar.holidays()),
                calendar.payload(), country, false);
    }

    public UtilityResult next(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String country = country(args.requiredString("countryCode", 2));
        LocalDate from = args.date("fromDate", LocalDate.now(clock));
        int limit = args.intValue("limit", 1, 1, 20);
        int lookaheadYears = args.intValue("lookaheadYears", 2, 1, 5);
        List<Map<String, Object>> found = new ArrayList<>();
        ProviderPayload lastPayload = null;
        for (int year = from.getYear(); year <= Math.min(MAX_YEAR, from.getYear() + lookaheadYears); year++) {
            Calendar calendar = load(country, year);
            lastPayload = calendar.payload();
            calendar.holidays().stream()
                    .filter(item -> !LocalDate.parse(item.get("date").toString()).isBefore(from))
                    .limit(limit - found.size())
                    .forEach(found::add);
            if (found.size() >= limit) break;
        }
        if (lastPayload == null) throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE, "No holiday calendar was loaded", true);
        return result(Map.of("countryCode", country, "fromDate", from.toString(), "holidays", found),
                lastPayload, country, found.size() >= limit);
    }

    public UtilityResult isWorkday(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String country = country(args.requiredString("countryCode", 2));
        LocalDate date = args.date("date", null);
        if (date == null) throw UtilityToolException.invalid("date is required");
        Calendar calendar = load(country, date.getYear());
        List<Map<String, Object>> matches = calendar.holidays().stream()
                .filter(item -> item.get("date").equals(date.toString()))
                .toList();
        String status;
        if (!matches.isEmpty()) status = "PUBLIC_HOLIDAY";
        else if (isWeekend(date)) status = "WEEKEND";
        else if (LIMITED_WORKDAY_COUNTRIES.contains(country)) status = "UNKNOWN_OR_UNSUPPORTED";
        else status = "WORKDAY";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("countryCode", country);
        data.put("date", date.toString());
        data.put("status", status);
        data.put("holidays", matches);
        data.put("coverage", coverage(country));
        return result(data, calendar.payload(), country, false);
    }

    public UtilityResult addWorkdays(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String country = country(args.requiredString("countryCode", 2));
        LocalDate start = args.date("startDate", null);
        if (start == null) throw UtilityToolException.invalid("startDate is required");
        int businessDays = args.intValue("businessDays", 0, -366, 366);
        boolean includeStart = "INCLUDE_IF_WORKDAY".equals(args.enumValue("inclusionPolicy", "EXCLUDE_START",
                Set.of("EXCLUDE_START", "INCLUDE_IF_WORKDAY")));
        if (LIMITED_WORKDAY_COUNTRIES.contains(country)) {
            throw new UtilityToolException(UtilityErrorCode.UNSUPPORTED_VALUE,
                    "workday_add is not reliable for " + country + " because substitute workdays are not represented by Nager.Date", false);
        }
        if (businessDays == 0) {
            return UtilityResult.local(Map.of("countryCode", country, "startDate", start.toString(),
                    "businessDays", 0, "resultDate", start.toString(), "coverage", coverage(country)));
        }
        int direction = Integer.signum(businessDays);
        int remaining = Math.abs(businessDays);
        LocalDate cursor = start;
        Map<Integer, Set<LocalDate>> holidayDates = new LinkedHashMap<>();
        int scanned = 0;
        if (includeStart && isWorkday(country, cursor, holidayDates)) remaining--;
        while (remaining > 0) {
            if (++scanned > 1_500) throw UtilityToolException.invalid("requested workday range exceeds the search limit");
            cursor = cursor.plusDays(direction);
            if (isWorkday(country, cursor, holidayDates)) remaining--;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("countryCode", country);
        data.put("startDate", start.toString());
        data.put("businessDays", businessDays);
        data.put("inclusionPolicy", includeStart ? "INCLUDE_IF_WORKDAY" : "EXCLUDE_START");
        data.put("resultDate", cursor.toString());
        data.put("coverage", coverage(country));
        return UtilityResult.local(data);
    }

    private boolean isWorkday(String country, LocalDate date, Map<Integer, Set<LocalDate>> cache) {
        if (isWeekend(date)) return false;
        Set<LocalDate> holidays = cache.computeIfAbsent(date.getYear(), year -> {
            Set<LocalDate> values = new LinkedHashSet<>();
            load(country, year).holidays().forEach(item -> values.add(LocalDate.parse(item.get("date").toString())));
            return values;
        });
        return !holidays.contains(date);
    }

    private Calendar load(String country, int year) {
        ProviderPayload payload = provider.get("/api/v3/PublicHolidays/" + year + "/" + country, Map.of());
        if (!payload.body().isArray()) throw JsonSupport.malformed("holiday response is not an array");
        List<Map<String, Object>> holidays = new ArrayList<>();
        for (JsonNode item : payload.body()) {
            Map<String, Object> holiday = new LinkedHashMap<>();
            holiday.put("date", JsonSupport.text(item, "date"));
            holiday.put("name", JsonSupport.text(item, "name"));
            String localName = JsonSupport.optionalText(item, "localName");
            if (localName != null) holiday.put("localName", localName);
            if (item.has("global")) holiday.put("global", item.path("global").asBoolean());
            if (item.path("types").isArray()) holiday.put("types", JsonSupport.scalarArray(item.path("types"), 20));
            if (item.path("counties").isArray()) holiday.put("subdivisions", JsonSupport.scalarArray(item.path("counties"), 100));
            holidays.add(holiday);
        }
        holidays.sort(Comparator.comparing(item -> item.get("date").toString()));
        return new Calendar(List.copyOf(holidays), payload);
    }

    private static UtilityResult result(Map<String, Object> data, ProviderPayload payload, String country, boolean partial) {
        Map<String, Object> withCoverage = new LinkedHashMap<>(data);
        withCoverage.putIfAbsent("coverage", coverage(country));
        return UtilityResult.external(withCoverage, "nager-date", payload.sourceUri(), payload.retrievedAt(),
                payload.cached(), partial, null, Map.of());
    }

    private static Map<String, Object> coverage(String country) {
        return Map.of(
                "weekendPolicy", "SATURDAY_SUNDAY",
                "publicHolidayProvider", "Nager.Date",
                "substituteWorkdaysCovered", !LIMITED_WORKDAY_COUNTRIES.contains(country),
                "limitations", LIMITED_WORKDAY_COUNTRIES.contains(country)
                        ? "Substitute workdays and make-up working weekends are not represented reliably."
                        : "Regional, financial-market and employer-specific calendars may differ.");
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static String country(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{2}")) throw UtilityToolException.invalid("countryCode must contain two letters");
        return normalized;
    }

    private record Calendar(List<Map<String, Object>> holidays, ProviderPayload payload) {}
}
