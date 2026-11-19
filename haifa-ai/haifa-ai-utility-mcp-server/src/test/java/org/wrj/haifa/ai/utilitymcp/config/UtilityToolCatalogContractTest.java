package org.wrj.haifa.ai.utilitymcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.utilitymcp.provider.JsonProvider;
import org.wrj.haifa.ai.utilitymcp.provider.ProviderPayload;
import org.wrj.haifa.ai.utilitymcp.tool.CalculatorService;
import org.wrj.haifa.ai.utilitymcp.tool.CurrencyService;
import org.wrj.haifa.ai.utilitymcp.tool.HolidayService;
import org.wrj.haifa.ai.utilitymcp.tool.MicrosoftLearnService;
import org.wrj.haifa.ai.utilitymcp.tool.TimeService;
import org.wrj.haifa.ai.utilitymcp.tool.UnitConversionService;
import org.wrj.haifa.ai.utilitymcp.tool.WeatherService;
import org.wrj.haifa.ai.utilitymcp.tool.WikipediaService;

class UtilityToolCatalogContractTest {

    @Test
    void publishesExactlyTheNineteenVersionedUtilityContracts() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var emptyBody = mapper.readTree("{}");
        JsonProvider empty = (path, query) -> new ProviderPayload(emptyBody,
                URI.create("https://fixture.example" + path), OffsetDateTime.now(), false);
        UtilityToolConfiguration configuration = new UtilityToolConfiguration();
        var catalog = configuration.utilityToolCatalog(
                new TimeService(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)),
                new CalculatorService(),
                new UnitConversionService(),
                new WeatherService(empty, empty, empty),
                new CurrencyService(empty),
                new HolidayService(empty),
                new WikipediaService(empty),
                new MicrosoftLearnService((toolName, arguments) ->
                        org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult.local(Map.of("content", "fixture"))));

        assertThat(catalog.tools()).hasSize(19);
        assertThat(catalog.tools().stream().map(tool -> tool.contract().name()).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(Set.of(
                        "location_search", "weather_current", "weather_forecast", "air_quality",
                        "time_now", "time_convert", "currency_rate", "currency_convert",
                        "holiday_list", "holiday_next", "workday_is_workday", "workday_add",
                        "calculate", "unit_convert", "wikipedia_search", "wikipedia_summary",
                        "microsoft_docs_search", "microsoft_docs_fetch", "microsoft_code_sample_search"));
        assertThat(catalog.tools()).allSatisfy(tool -> {
            assertThat(tool.contract().inputSchema().type()).isEqualTo("object");
            assertThat(tool.contract().inputSchema().additionalProperties()).isFalse();
            assertThat(tool.contract().outputSchema()).containsEntry("type", "object");
            assertThat(tool.contract().annotations().readOnlyHint()).isTrue();
            assertThat(tool.contract().meta()).containsEntry("contractVersion", "v1");
        });

        @SuppressWarnings("unchecked")
        Map<String, java.util.List<String>> golden = mapper.readValue(
                getClass().getResourceAsStream("/contracts/utility-tools-v1.json"), Map.class);
        Map<String, java.util.List<String>> actual = new java.util.TreeMap<>();
        catalog.tools().forEach(tool -> actual.put(tool.contract().name(),
                tool.contract().inputSchema().properties().keySet().stream().sorted().toList()));
        assertThat(actual).isEqualTo(golden);
    }
}
