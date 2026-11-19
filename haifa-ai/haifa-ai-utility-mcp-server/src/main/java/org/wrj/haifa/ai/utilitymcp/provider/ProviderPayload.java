package org.wrj.haifa.ai.utilitymcp.provider;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.OffsetDateTime;

public record ProviderPayload(JsonNode body, URI sourceUri, OffsetDateTime retrievedAt, boolean cached) {}
