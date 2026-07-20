package org.wrj.haifa.ai.utilitymcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolResultFactory;
import org.wrj.haifa.ai.utilitymcp.mcp.SimpleUtilityTool;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolSchemas;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityTool;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolCatalog;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;
import org.wrj.haifa.ai.utilitymcp.tool.CalculatorService;
import org.wrj.haifa.ai.utilitymcp.tool.CurrencyService;
import org.wrj.haifa.ai.utilitymcp.tool.HolidayService;
import org.wrj.haifa.ai.utilitymcp.tool.TimeService;
import org.wrj.haifa.ai.utilitymcp.tool.UnitConversionService;
import org.wrj.haifa.ai.utilitymcp.tool.WeatherService;
import org.wrj.haifa.ai.utilitymcp.tool.WikipediaService;
import org.wrj.haifa.ai.utilitymcp.provider.JsonProvider;

@Configuration
public class UtilityToolConfiguration {

    @Bean
    Clock utilityClock() {
        return Clock.systemUTC();
    }

    @Bean
    TimeService timeService(Clock utilityClock) {
        return new TimeService(utilityClock);
    }

    @Bean
    CalculatorService calculatorService() {
        return new CalculatorService();
    }

    @Bean
    UnitConversionService unitConversionService() {
        return new UnitConversionService();
    }

    @Bean
    WeatherService weatherService(
            @Qualifier("openMeteoForecast") JsonProvider forecast,
            @Qualifier("openMeteoGeocoding") JsonProvider geocoding,
            @Qualifier("openMeteoAirQuality") JsonProvider airQuality) {
        return new WeatherService(forecast, geocoding, airQuality);
    }

    @Bean
    CurrencyService currencyService(@Qualifier("frankfurter") JsonProvider provider) {
        return new CurrencyService(provider);
    }

    @Bean
    HolidayService holidayService(@Qualifier("nagerDate") JsonProvider provider, Clock utilityClock) {
        return new HolidayService(provider, utilityClock);
    }

    @Bean
    WikipediaService wikipediaService(@Qualifier("wikimedia") JsonProvider provider) {
        return new WikipediaService(provider);
    }

    @Bean
    UtilityToolCatalog utilityToolCatalog(
            TimeService timeService,
            CalculatorService calculatorService,
            UnitConversionService unitConversionService,
            WeatherService weatherService,
            CurrencyService currencyService,
            HolidayService holidayService,
            WikipediaService wikipediaService) {
        List<UtilityTool> tools = new ArrayList<>();
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("time_now", "Current time",
                        "Return the current instant and local time in a validated IANA timezone. Defaults to UTC.",
                        ToolSchemas.object(Map.of("timezone", ToolSchemas.string("IANA timezone", 64))), false),
                timeService::now));
        Map<String, Object> timeConvert = new LinkedHashMap<>();
        timeConvert.put("dateTime", ToolSchemas.string("ISO-8601 local or offset date-time", 64));
        timeConvert.put("fromTimezone", ToolSchemas.string("Source IANA timezone", 64));
        timeConvert.put("toTimezone", ToolSchemas.string("Target IANA timezone", 64));
        timeConvert.put("overlapPolicy", ToolSchemas.enumeration("DST overlap handling",
                "REJECT", "EARLIER_OFFSET", "LATER_OFFSET"));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("time_convert", "Convert time",
                        "Convert an ISO-8601 date-time between IANA timezones with explicit DST overlap handling.",
                        ToolSchemas.object(timeConvert, "dateTime", "fromTimezone", "toTimezone"), false),
                timeService::convert));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("calculate", "Calculate expression",
                        "Evaluate a bounded arithmetic expression using a safe BigDecimal parser. No code execution.",
                        ToolSchemas.object(Map.of("expression", ToolSchemas.string("Arithmetic expression", 512)), "expression"), false),
                calculatorService::calculate));
        Map<String, Object> unit = new LinkedHashMap<>();
        unit.put("value", Map.of("type", List.of("number", "string"), "description", "Decimal value"));
        unit.put("fromUnit", ToolSchemas.string("Source canonical unit or supported alias", 32));
        unit.put("toUnit", ToolSchemas.string("Target canonical unit or supported alias", 32));
        unit.put("scale", ToolSchemas.integer("Output decimal scale", 0, 30, 12));
        unit.put("roundingMode", ToolSchemas.string("java.math.RoundingMode name", 32));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("unit_convert", "Convert units",
                        "Convert values between supported units of the same dimension, including affine temperatures.",
                        ToolSchemas.object(unit, "value", "fromUnit", "toUnit"), false),
                unitConversionService::convert));
        addWeatherTools(tools, weatherService);
        addCurrencyTools(tools, currencyService);
        addHolidayTools(tools, holidayService);
        addWikipediaTools(tools, wikipediaService);
        return new UtilityToolCatalog(tools);
    }

    private static void addWeatherTools(List<UtilityTool> tools, WeatherService service) {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("query", ToolSchemas.string("Place name to search", 200));
        location.put("limit", ToolSchemas.integer("Maximum locations", 1, 20, 5));
        location.put("language", ToolSchemas.string("Result language code", 8));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("location_search", "Search locations",
                        "Resolve a place name to bounded Open-Meteo location records.",
                        ToolSchemas.object(location, "query"), true), service::locationSearch));

        Map<String, Object> coordinates = coordinates();
        coordinates.put("unitSystem", ToolSchemas.enumeration("Unit system", "METRIC", "IMPERIAL"));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("weather_current", "Current weather",
                        "Return current weather fields and units for validated coordinates.",
                        ToolSchemas.object(coordinates, "latitude", "longitude"), true), service::current));

        Map<String, Object> forecast = new LinkedHashMap<>(coordinates());
        forecast.put("unitSystem", ToolSchemas.enumeration("Unit system", "METRIC", "IMPERIAL"));
        forecast.put("days", ToolSchemas.integer("Forecast days", 1, 16, 7));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("weather_forecast", "Weather forecast",
                        "Return a bounded daily Open-Meteo forecast with explicit units.",
                        ToolSchemas.object(forecast, "latitude", "longitude"), true), service::forecast));

        Map<String, Object> air = coordinates();
        air.put("forecastHours", ToolSchemas.integer("Forecast hours", 1, 120, 24));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("air_quality", "Air quality",
                        "Return bounded hourly pollutant and named AQI values from Open-Meteo.",
                        ToolSchemas.object(air, "latitude", "longitude"), true), service::airQuality));
    }

    private static void addCurrencyTools(List<UtilityTool> tools, CurrencyService service) {
        Map<String, Object> rate = new LinkedHashMap<>();
        rate.put("baseCurrency", ToolSchemas.string("Three-letter base currency code", 3));
        rate.put("quoteCurrency", ToolSchemas.string("Three-letter quote currency code", 3));
        rate.put("date", ToolSchemas.string("Optional historical date in yyyy-MM-dd", 10));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("currency_rate", "Currency rate",
                        "Return a Frankfurter reference exchange rate and its effective date; not trading advice.",
                        ToolSchemas.object(rate, "baseCurrency", "quoteCurrency"), true), service::rate));

        Map<String, Object> convert = new LinkedHashMap<>(rate);
        convert.put("amount", Map.of("type", List.of("number", "string"), "description", "Decimal amount"));
        convert.put("scale", ToolSchemas.integer("Output decimal scale", 0, 18, 6));
        convert.put("roundingMode", ToolSchemas.string("java.math.RoundingMode name", 32));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("currency_convert", "Convert currency",
                        "Convert an amount with a Frankfurter reference rate and explicit BigDecimal rounding.",
                        ToolSchemas.object(convert, "amount", "baseCurrency", "quoteCurrency"), true), service::convert));
    }

    private static void addHolidayTools(List<UtilityTool> tools, HolidayService service) {
        Map<String, Object> list = new LinkedHashMap<>();
        list.put("countryCode", ToolSchemas.string("ISO alpha-2 country code", 2));
        list.put("year", ToolSchemas.integer("Calendar year", 1970, 2100, 2026));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("holiday_list", "List public holidays",
                        "List bounded Nager.Date public holidays for a country and year.",
                        ToolSchemas.object(list, "countryCode", "year"), true), service::list));

        Map<String, Object> next = new LinkedHashMap<>();
        next.put("countryCode", ToolSchemas.string("ISO alpha-2 country code", 2));
        next.put("fromDate", ToolSchemas.string("Inclusive start date in yyyy-MM-dd", 10));
        next.put("limit", ToolSchemas.integer("Maximum holidays", 1, 20, 1));
        next.put("lookaheadYears", ToolSchemas.integer("Maximum calendar years to scan", 1, 5, 2));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("holiday_next", "Find next holiday",
                        "Find the next bounded set of public holidays on or after a date.",
                        ToolSchemas.object(next, "countryCode", "fromDate"), true), service::next));

        Map<String, Object> workday = new LinkedHashMap<>();
        workday.put("countryCode", ToolSchemas.string("ISO alpha-2 country code", 2));
        workday.put("date", ToolSchemas.string("Date in yyyy-MM-dd", 10));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("workday_is_workday", "Check workday",
                        "Classify a date using weekends and Nager.Date coverage, reporting unsupported substitute-workday cases.",
                        ToolSchemas.object(workday, "countryCode", "date"), true), service::isWorkday));

        Map<String, Object> add = new LinkedHashMap<>();
        add.put("countryCode", ToolSchemas.string("ISO alpha-2 country code", 2));
        add.put("startDate", ToolSchemas.string("Start date in yyyy-MM-dd", 10));
        add.put("businessDays", ToolSchemas.integer("Signed business-day offset", -366, 366, 0));
        add.put("inclusionPolicy", ToolSchemas.enumeration("Start-date inclusion", "EXCLUDE_START", "INCLUDE_IF_WORKDAY"));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("workday_add", "Add workdays",
                        "Add a bounded signed number of workdays using a cached yearly holiday calendar.",
                        ToolSchemas.object(add, "countryCode", "startDate", "businessDays"), true), service::addWorkdays));
    }

    private static void addWikipediaTools(List<UtilityTool> tools, WikipediaService service) {
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("query", ToolSchemas.string("Wikipedia search query", 300));
        search.put("language", ToolSchemas.enumeration("Wikipedia language", "en", "zh", "de", "fr", "es", "ja"));
        search.put("limit", ToolSchemas.integer("Maximum results", 1, 20, 5));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("wikipedia_search", "Search Wikipedia",
                        "Search Wikimedia and return bounded page identities, snippets and canonical URLs.",
                        ToolSchemas.object(search, "query"), true), service::search));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("title", ToolSchemas.string("Wikipedia page title or key, not a URL", 300));
        summary.put("language", ToolSchemas.enumeration("Wikipedia language", "en", "zh", "de", "fr", "es", "ja"));
        tools.add(new SimpleUtilityTool(
                ToolSchemas.tool("wikipedia_summary", "Read Wikipedia summary",
                        "Read bounded plain text for a Wikimedia page identity and return a canonical citation URL.",
                        ToolSchemas.object(summary, "title"), true), service::summary));
    }

    private static Map<String, Object> coordinates() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("latitude", ToolSchemas.number("Latitude in decimal degrees", -90, 90));
        result.put("longitude", ToolSchemas.number("Longitude in decimal degrees", -180, 180));
        result.put("timezone", ToolSchemas.string("IANA timezone", 64));
        return result;
    }

    @Bean
    ToolResultFactory toolResultFactory(ObjectMapper objectMapper, UtilityMcpProperties properties) {
        return new ToolResultFactory(objectMapper, properties);
    }

    @Bean
    List<McpStatelessServerFeatures.SyncToolSpecification> utilityToolSpecifications(
            UtilityToolCatalog catalog,
            ToolResultFactory results) {
        Set<String> names = new HashSet<>();
        return catalog.tools().stream()
                .map(tool -> {
                    if (!names.add(tool.contract().name())) {
                        throw new IllegalStateException("Duplicate utility tool: " + tool.contract().name());
                    }
                    return McpStatelessServerFeatures.SyncToolSpecification.builder()
                            .tool(tool.contract())
                            .callHandler((context, request) -> {
                                try {
                                    return results.success(tool.execute(request.arguments()));
                                }
                                catch (UtilityToolException ex) {
                                    return results.error(ex);
                                }
                                catch (RuntimeException ex) {
                                    return results.error(ex);
                                }
                            })
                            .build();
                })
                .toList();
    }
}
