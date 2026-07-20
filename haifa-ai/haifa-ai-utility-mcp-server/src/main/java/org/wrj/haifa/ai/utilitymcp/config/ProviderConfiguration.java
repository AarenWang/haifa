package org.wrj.haifa.ai.utilitymcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wrj.haifa.ai.utilitymcp.provider.JsonProvider;
import org.wrj.haifa.ai.utilitymcp.provider.ResilientJsonProvider;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class ProviderConfiguration {

    @Bean
    @Qualifier("openMeteoForecast")
    JsonProvider openMeteoForecast(UtilityMcpProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        return new ResilientJsonProvider("open-meteo", properties.getProviders().getOpenMeteo(), objectMapper, meterRegistry);
    }

    @Bean
    @Qualifier("openMeteoGeocoding")
    JsonProvider openMeteoGeocoding(UtilityMcpProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        return new ResilientJsonProvider("open-meteo-geocoding", properties.getProviders().getOpenMeteoGeocoding(), objectMapper, meterRegistry);
    }

    @Bean
    @Qualifier("openMeteoAirQuality")
    JsonProvider openMeteoAirQuality(UtilityMcpProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        return new ResilientJsonProvider("open-meteo-air-quality", properties.getProviders().getOpenMeteoAirQuality(), objectMapper, meterRegistry);
    }

    @Bean
    @Qualifier("frankfurter")
    JsonProvider frankfurter(UtilityMcpProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        return new ResilientJsonProvider("frankfurter", properties.getProviders().getFrankfurter(), objectMapper, meterRegistry);
    }

    @Bean
    @Qualifier("nagerDate")
    JsonProvider nagerDate(UtilityMcpProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        return new ResilientJsonProvider("nager-date", properties.getProviders().getNagerDate(), objectMapper, meterRegistry);
    }

    @Bean
    @Qualifier("wikimedia")
    JsonProvider wikimedia(UtilityMcpProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        return new ResilientJsonProvider("wikimedia", properties.getProviders().getWikimedia(), objectMapper, meterRegistry);
    }
}
