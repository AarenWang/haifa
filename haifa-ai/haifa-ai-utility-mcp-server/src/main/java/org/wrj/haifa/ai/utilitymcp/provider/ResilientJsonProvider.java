package org.wrj.haifa.ai.utilitymcp.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class ResilientJsonProvider implements JsonProvider {

    private static final Logger log = LoggerFactory.getLogger(ResilientJsonProvider.class);
    private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(30);
    private final String providerId;
    private final UtilityMcpProperties.Provider properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Semaphore bulkhead;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<URI, CacheEntry> cache = new ConcurrentHashMap<>();
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile Instant circuitOpenUntil = Instant.EPOCH;

    public ResilientJsonProvider(
            String providerId,
            UtilityMcpProperties.Provider properties,
            ObjectMapper objectMapper) {
        this(providerId, properties, objectMapper, null);
    }

    public ResilientJsonProvider(
            String providerId,
            UtilityMcpProperties.Provider properties,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.providerId = providerId;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        validateBaseUrl(properties);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.bulkhead = new Semaphore(Math.max(1, properties.getMaxConcurrent()));
    }

    @Override
    public ProviderPayload get(String path, Map<String, ?> query) {
        long started = System.nanoTime();
        String safePath = safePath(path);
        URI uri;
        try {
            uri = buildUri(path, query);
        }
        catch (RuntimeException ex) {
            record("invalid_request", started);
            log.error("event=mcp_provider_request_build_failed provider={} path={} errorType={}",
                    providerId, safePath, ex.getClass().getSimpleName());
            throw ex;
        }
        CacheEntry cached = cache.get(uri);
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            increment("cache_hit");
            record("success", started);
            return new ProviderPayload(cached.body(), uri, cached.retrievedAt(), true);
        }
        if (circuitOpenUntil.isAfter(now)) {
            increment("circuit_open");
            record("circuit_open", started);
            log.warn("event=mcp_provider_request_rejected provider={} path={} reason=circuit_open retryAfterMs={}",
                    providerId, safePath, Math.max(0, Duration.between(now, circuitOpenUntil).toMillis()));
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    providerId + " circuit is temporarily open", true);
        }
        if (!bulkhead.tryAcquire()) {
            increment("bulkhead_rejected");
            record("bulkhead_rejected", started);
            log.warn("event=mcp_provider_request_rejected provider={} path={} reason=bulkhead_rejected", providerId, safePath);
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    providerId + " concurrency limit reached", true);
        }
        try {
            ProviderPayload payload = requestWithOneRetry(uri, safePath);
            consecutiveFailures.set(0);
            cache.put(uri, new CacheEntry(payload.body(), payload.retrievedAt(),
                    Instant.now().plus(properties.getCacheTtl())));
            record("success", started);
            return payload;
        }
        catch (UtilityToolException ex) {
            record("failure", started);
            log.error("event=mcp_provider_request_failed provider={} path={} code={} retryable={} errorType={} rootCause={} detail={} durationMs={}",
                    providerId, safePath, ex.code(), ex.retryable(), ex.getClass().getSimpleName(),
                    rootCauseName(ex), safeDetail(ex.getMessage()), elapsedMillis(started));
            throw ex;
        }
        catch (RuntimeException ex) {
            record("unexpected_failure", started);
            log.error("event=mcp_provider_request_failed provider={} path={} code=UNEXPECTED retryable=false errorType={} rootCause={} durationMs={}",
                    providerId, safePath, ex.getClass().getSimpleName(), rootCauseName(ex), elapsedMillis(started));
            throw ex;
        }
        finally {
            bulkhead.release();
        }
    }

    private ProviderPayload requestWithOneRetry(URI uri, String safePath) {
        UtilityToolException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return request(uri);
            }
            catch (UtilityToolException ex) {
                last = ex;
                boolean willRetry = ex.retryable()
                        && ex.code() != UtilityErrorCode.UPSTREAM_RATE_LIMITED
                        && attempt == 0;
                log.warn("event=mcp_provider_attempt_failed provider={} path={} attempt={} maxAttempts=2 code={} retryable={} willRetry={} errorType={} rootCause={} detail={}",
                        providerId, safePath, attempt + 1, ex.code(), ex.retryable(), willRetry,
                        ex.getClass().getSimpleName(), rootCauseName(ex), safeDetail(ex.getMessage()));
                if (!willRetry) {
                    recordFailure();
                    throw ex;
                }
            }
        }
        recordFailure();
        throw last == null
                ? new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE, providerId + " request failed", true)
                : last;
    }

    private ProviderPayload request(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.getResponseTimeout())
                .header("Accept", "application/json")
                .header("User-Agent", "haifa-utility-mcp/1.0")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status == 429) {
                closeQuietly(response.body());
                throw new UtilityToolException(UtilityErrorCode.UPSTREAM_RATE_LIMITED,
                        providerId + " rate limit reached", true);
            }
            if (status < 200 || status >= 300) {
                closeQuietly(response.body());
                boolean retryable = status >= 500;
                throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                        providerId + " returned HTTP " + status, retryable);
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase(java.util.Locale.ROOT).contains("json")) {
                closeQuietly(response.body());
                throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                        providerId + " returned a non-JSON response", false);
            }
            int limit = Math.max(1024, properties.getMaxResponseBytes());
            byte[] bytes;
            try (InputStream stream = response.body()) {
                bytes = stream.readNBytes(limit + 1);
            }
            if (bytes.length > limit) {
                throw new UtilityToolException(UtilityErrorCode.RESULT_TOO_LARGE,
                        providerId + " response exceeds the configured size limit", false);
            }
            JsonNode body = objectMapper.readTree(bytes);
            return new ProviderPayload(body, uri, OffsetDateTime.now(ZoneOffset.UTC), false);
        }
        catch (java.net.http.HttpTimeoutException ex) {
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_TIMEOUT,
                    providerId + " timed out", true, ex);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_TIMEOUT,
                    providerId + " request was interrupted", true, ex);
        }
        catch (IOException ex) {
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    providerId + " request failed", true, ex);
        }
    }

    private URI buildUri(String path, Map<String, ?> query) {
        if (path == null || !path.startsWith("/") || path.contains("..")) {
            throw new IllegalArgumentException("Provider path must be an absolute safe path");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(properties.getBaseUrl()).path(path);
        if (query != null) {
            query.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(entry -> builder.queryParam(entry.getKey(), entry.getValue()));
        }
        URI uri = builder.build().encode().toUri();
        if (!properties.getBaseUrl().getHost().equalsIgnoreCase(uri.getHost())) {
            throw new IllegalStateException("Provider URI escaped the configured host");
        }
        return uri;
    }

    private void recordFailure() {
        increment("failure");
        if (consecutiveFailures.incrementAndGet() >= 3) {
            circuitOpenUntil = Instant.now().plus(CIRCUIT_OPEN_DURATION);
            consecutiveFailures.set(0);
        }
    }

    private void increment(String result) {
        if (meterRegistry != null) meterRegistry.counter("mcp.provider.request", "provider", providerId, "result", result).increment();
    }

    private void record(String result, long startedNanos) {
        if (meterRegistry == null) return;
        Timer.builder("mcp.provider.duration").tag("provider", providerId).tag("result", result)
                .register(meterRegistry).record(Duration.ofNanos(System.nanoTime() - startedNanos));
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0, Duration.ofNanos(System.nanoTime() - startedNanos).toMillis());
    }

    private static String rootCauseName(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current.getClass().getSimpleName();
    }

    private static String safePath(String path) {
        if (path == null || path.isBlank()) return "unknown";
        String value = path.replaceAll("[\\p{Cntrl}]", "?");
        int query = value.indexOf('?');
        if (query >= 0) value = value.substring(0, query);
        return value.length() <= 160 ? value : value.substring(0, 160);
    }

    private static String safeDetail(String message) {
        if (message == null || message.isBlank()) return "none";
        String value = message.replaceAll("[\\p{Cntrl}]", "?");
        return value.length() <= 200 ? value : value.substring(0, 200);
    }

    private static void validateBaseUrl(UtilityMcpProperties.Provider properties) {
        URI uri = properties.getBaseUrl();
        if (uri == null || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Provider base URL must be an absolute origin without credentials, query or fragment");
        }
        boolean loopbackTestHttp = properties.isAllowHttpForTests()
                && "http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost())
                    || "127.0.0.1".equals(uri.getHost())
                    || "[::1]".equals(uri.getHost())
                    || "::1".equals(uri.getHost()));
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !loopbackTestHttp) {
            throw new IllegalArgumentException("Provider base URL must use HTTPS");
        }
    }

    private static void closeQuietly(InputStream stream) {
        try { stream.close(); }
        catch (IOException ignored) { }
    }

    private record CacheEntry(JsonNode body, OffsetDateTime retrievedAt, Instant expiresAt) {}
}
