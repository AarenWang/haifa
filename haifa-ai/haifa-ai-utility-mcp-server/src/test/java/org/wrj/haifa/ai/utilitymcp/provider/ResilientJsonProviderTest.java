package org.wrj.haifa.ai.utilitymcp.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

class ResilientJsonProviderTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void retriesOneServerFailureThenCachesSuccess() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/data", exchange -> {
            int status = calls.incrementAndGet() == 1 ? 503 : 200;
            byte[] body = (status == 200 ? "{\"value\":42}" : "unavailable").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", status == 200 ? "application/json" : "text/plain");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ResilientJsonProvider provider = provider(1024);
        ProviderPayload first = provider.get("/data", Map.of("q", "safe"));
        ProviderPayload second = provider.get("/data", Map.of("q", "safe"));

        assertThat(first.body().get("value").asInt()).isEqualTo(42);
        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isTrue();
        assertThat(calls).hasValue(2);
    }

    @Test
    void rejectsWrongMimeAndOversizedBody() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/wrong", exchange -> {
            byte[] body = "<html/>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/large", exchange -> {
            byte[] body = ("{\"value\":\"" + "x".repeat(2_000) + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ResilientJsonProvider provider = provider(1024);
        assertThatThrownBy(() -> provider.get("/wrong", Map.of()))
                .isInstanceOfSatisfying(UtilityToolException.class,
                        ex -> assertThat(ex.code()).isEqualTo(UtilityErrorCode.UPSTREAM_UNAVAILABLE));
        assertThatThrownBy(() -> provider.get("/large", Map.of()))
                .isInstanceOfSatisfying(UtilityToolException.class,
                        ex -> assertThat(ex.code()).isEqualTo(UtilityErrorCode.RESULT_TOO_LARGE));
    }

    @Test
    void logsRetriesAndFinalFailureWithoutQueryValuesOrResponseBody() throws Exception {
        String sensitive = "private-query-value";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/failure", exchange -> {
            byte[] body = ("upstream-body-" + sensitive).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        Logger logger = (Logger) LoggerFactory.getLogger(ResilientJsonProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> provider(1024).get("/failure", Map.of("query", sensitive)))
                    .isInstanceOf(UtilityToolException.class);

            String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .collect(java.util.stream.Collectors.joining("\n"));
            assertThat(logs).contains("event=mcp_provider_attempt_failed", "attempt=1", "willRetry=true",
                            "attempt=2", "event=mcp_provider_request_failed", "provider=fixture",
                            "path=/failure", "code=UPSTREAM_UNAVAILABLE")
                    .doesNotContain(sensitive, "upstream-body", "?query=");
        }
        finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private ResilientJsonProvider provider(int maxBytes) {
        UtilityMcpProperties.Provider properties = new UtilityMcpProperties.Provider(
                "http://127.0.0.1:" + server.getAddress().getPort());
        properties.setAllowHttpForTests(true);
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setResponseTimeout(Duration.ofSeconds(1));
        properties.setCacheTtl(Duration.ofMinutes(1));
        properties.setMaxResponseBytes(maxBytes);
        return new ResilientJsonProvider("fixture", properties, new ObjectMapper());
    }
}
