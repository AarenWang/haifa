package org.wrj.haifa.ai.utilitymcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolArguments;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.provider.JsonProvider;
import org.wrj.haifa.ai.utilitymcp.provider.ProviderPayload;

public class WeatherService {

    private static final Set<String> UNIT_SYSTEMS = Set.of("METRIC", "IMPERIAL");
    private final JsonProvider forecast;
    private final JsonProvider geocoding;
    private final JsonProvider airQuality;

    public WeatherService(JsonProvider forecast, JsonProvider geocoding, JsonProvider airQuality) {
        this.forecast = forecast;
        this.geocoding = geocoding;
        this.airQuality = airQuality;
    }

    public UtilityResult locationSearch(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String query = args.requiredString("query", 200);
        int limit = args.intValue("limit", 5, 1, 20);
        String language = language(args.optionalString("language", "en", 8));
        ProviderPayload payload = geocoding.get("/v1/search", Map.of(
                "name", query, "count", limit, "language", language, "format", "json"));
        JsonNode results = payload.body().path("results");
        List<Map<String, Object>> locations = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode item : results) {
                if (locations.size() >= limit) break;
                Map<String, Object> location = new LinkedHashMap<>();
                put(location, "id", item.get("id"));
                put(location, "name", item.get("name"));
                put(location, "country", item.get("country"));
                put(location, "countryCode", item.get("country_code"));
                put(location, "admin1", item.get("admin1"));
                put(location, "latitude", item.get("latitude"));
                put(location, "longitude", item.get("longitude"));
                put(location, "timezone", item.get("timezone"));
                if (!location.isEmpty()) locations.add(location);
            }
        }
        return external(Map.of("locations", locations), payload, null, Map.of(), locations.size() >= limit);
    }

    public UtilityResult current(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        double latitude = args.doubleValue("latitude", -90, 90);
        double longitude = args.doubleValue("longitude", -180, 180);
        String timezone = args.optionalString("timezone", "UTC", 64);
        String units = args.enumValue("unitSystem", "METRIC", UNIT_SYSTEMS);
        Map<String, Object> query = baseQuery(latitude, longitude, timezone, units);
        query.put("current", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m");
        ProviderPayload payload = forecast.get("/v1/forecast", query);
        JsonNode current = JsonSupport.required(payload.body(), "current");
        Map<String, Object> data = JsonSupport.scalarObject(current);
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("timezone", payload.body().path("timezone").asText(timezone));
        Map<String, Object> unitMap = JsonSupport.scalarObject(payload.body().path("current_units"));
        return external(data, payload, JsonSupport.optionalText(current, "time"), unitMap, false);
    }

    public UtilityResult forecast(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        double latitude = args.doubleValue("latitude", -90, 90);
        double longitude = args.doubleValue("longitude", -180, 180);
        String timezone = args.optionalString("timezone", "UTC", 64);
        String units = args.enumValue("unitSystem", "METRIC", UNIT_SYSTEMS);
        int days = args.intValue("days", 7, 1, 16);
        Map<String, Object> query = baseQuery(latitude, longitude, timezone, units);
        query.put("forecast_days", days);
        query.put("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max");
        ProviderPayload payload = forecast.get("/v1/forecast", query);
        JsonNode daily = JsonSupport.required(payload.body(), "daily");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("timezone", payload.body().path("timezone").asText(timezone));
        Map<String, Object> dailyData = new LinkedHashMap<>();
        daily.fields().forEachRemaining(entry -> dailyData.put(entry.getKey(), JsonSupport.scalarArray(entry.getValue(), days)));
        data.put("daily", dailyData);
        Map<String, Object> unitMap = JsonSupport.scalarObject(payload.body().path("daily_units"));
        return external(data, payload, null, unitMap, false);
    }

    public UtilityResult airQuality(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        double latitude = args.doubleValue("latitude", -90, 90);
        double longitude = args.doubleValue("longitude", -180, 180);
        String timezone = args.optionalString("timezone", "UTC", 64);
        int hours = args.intValue("forecastHours", 24, 1, 120);
        ProviderPayload payload = airQuality.get("/v1/air-quality", Map.of(
                "latitude", latitude,
                "longitude", longitude,
                "timezone", timezone,
                "forecast_hours", hours,
                "hourly", "pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,ozone,us_aqi,european_aqi"));
        JsonNode hourly = JsonSupport.required(payload.body(), "hourly");
        Map<String, Object> values = new LinkedHashMap<>();
        hourly.fields().forEachRemaining(entry -> values.put(entry.getKey(), JsonSupport.scalarArray(entry.getValue(), hours)));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("timezone", payload.body().path("timezone").asText(timezone));
        data.put("hourly", values);
        return external(data, payload, null, JsonSupport.scalarObject(payload.body().path("hourly_units")), false);
    }

    private static Map<String, Object> baseQuery(double latitude, double longitude, String timezone, String unitSystem) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("latitude", latitude);
        query.put("longitude", longitude);
        query.put("timezone", timezone);
        if ("IMPERIAL".equals(unitSystem)) {
            query.put("temperature_unit", "fahrenheit");
            query.put("wind_speed_unit", "mph");
            query.put("precipitation_unit", "inch");
        }
        return query;
    }

    private static String language(String language) {
        if (!language.matches("[A-Za-z]{2,3}([_-][A-Za-z]{2})?")) {
            throw org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException.invalid("language is invalid");
        }
        return language;
    }

    private static UtilityResult external(Map<String, Object> data, ProviderPayload payload,
            String observedAt, Map<String, Object> units, boolean partial) {
        return UtilityResult.external(data, "open-meteo", payload.sourceUri(), payload.retrievedAt(),
                payload.cached(), partial, observedAt, units);
    }

    private static void put(Map<String, Object> target, String name, JsonNode value) {
        if (value == null || value.isNull()) return;
        if (value.isNumber()) target.put(name, value.numberValue());
        else if (value.isBoolean()) target.put(name, value.booleanValue());
        else target.put(name, value.asText());
    }
}
