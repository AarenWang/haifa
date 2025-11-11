package org.wrj.haifa.ai.spring.toolcalling.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.wrj.haifa.ai.spring.toolcalling.config.ToolCallingProperties;
import org.wrj.haifa.ai.spring.toolcalling.model.GeoKnowledgeSummary;
import org.wrj.haifa.ai.spring.toolcalling.model.GeoLookupResponse;
import org.wrj.haifa.ai.spring.toolcalling.tool.GeoTool;
import reactor.core.publisher.Mono;

/**
 * Executes HTTP lookups against an external geography service and exposes the
 * result as a tool callable by the chat client.
 */
@Service
public class GeoKnowledgeService {

    private static final Logger logger = LoggerFactory.getLogger(GeoKnowledgeService.class);

    private final WebClient webClient;
    private final ToolCallingProperties properties;

    public GeoKnowledgeService(WebClient geoKnowledgeWebClient, ToolCallingProperties properties) {
        this.webClient = geoKnowledgeWebClient;
        this.properties = properties;
    }

    @GeoTool(name = "geo_lookup", description = "Fetch encyclopedic data about a geographic location.")
    public GeoKnowledgeSummary lookup(String location) {
        if (!StringUtils.hasText(location)) {
            return GeoKnowledgeSummary.empty();
        }

        ToolCallingProperties.GeoProperties geo = properties.getGeo();
        Duration timeout = Optional.ofNullable(geo.getTimeout()).orElse(Duration.ofSeconds(5));

        Mono<GeoLookupResponse> responseMono = this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(geo.getPath())
                        .queryParam("q", location)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(GeoLookupResponse.class);

        GeoLookupResponse apiResponse = responseMono
                .timeout(timeout)
                .doOnError(ex -> logger.warn("Geo lookup failed for '{}': {}", location, ex.getMessage()))
                .onErrorResume(ex -> Mono.just(GeoLookupResponse.empty(location)))
                .blockOptional()
                .orElse(GeoLookupResponse.empty(location));

        return new GeoKnowledgeSummary(apiResponse.title(), apiResponse.summary(), apiResponse.highlights());
    }

    public List<String> supportedTools() {
        return List.of("geo_lookup");
    }
}
