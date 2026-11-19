package org.wrj.haifa.ai.utilitymcp.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

class ResilientJsonProviderTest {

    private HttpServer server;
    private ServerSocket proxyServer;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
        if (proxyServer != null) {
            try { proxyServer.close(); }
            catch (java.io.IOException ignored) { }
        }
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

    @Test
    void routesProviderSelectedByCommaSeparatedListThroughConfiguredProxy() throws Exception {
        AtomicReference<String> requestedUri = new AtomicReference<>();
        proxyServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        Thread proxyThread = Thread.ofPlatform().start(() -> {
            try (Socket socket = proxyServer.accept()) {
                String connectRequest = readHeaders(socket.getInputStream());
                assertThat(connectRequest).startsWith("CONNECT 127.0.0.1:1 HTTP/1.1");
                socket.getOutputStream().write(
                        "HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();

                requestedUri.set(readHeaders(socket.getInputStream()));
                byte[] body = "{\"proxied\":true}".getBytes(StandardCharsets.UTF_8);
                String headers = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
                        + body.length + "\r\nConnection: close\r\n\r\n";
                socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().write(body);
                socket.getOutputStream().flush();
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        UtilityMcpProperties.Provider properties = new UtilityMcpProperties.Provider("http://127.0.0.1:1");
        properties.setAllowHttpForTests(true);
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setResponseTimeout(Duration.ofSeconds(1));
        UtilityMcpProperties.Proxy proxy = new UtilityMcpProperties.Proxy();
        proxy.setUrl("http://127.0.0.1:" + proxyServer.getLocalPort());
        proxy.setProviders("frankfurter, wikimedia");
        ResilientJsonProvider provider = new ResilientJsonProvider(
                "wikimedia", properties, new ObjectMapper(), null, proxy);

        ProviderPayload result = provider.get("/proxied", Map.of("q", "value"));
        proxyThread.join(Duration.ofSeconds(2));

        assertThat(result.body().get("proxied").asBoolean()).isTrue();
        assertThat(requestedUri.get()).contains("GET /proxied?q=value HTTP/1.1");
    }

    private static String readHeaders(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int matched = 0;
        while (matched < 4) {
            int value = input.read();
            if (value < 0) throw new java.io.IOException("Unexpected end of proxy request");
            bytes.write(value);
            matched = switch (matched) {
                case 0 -> value == '\r' ? 1 : 0;
                case 1 -> value == '\n' ? 2 : 0;
                case 2 -> value == '\r' ? 3 : 0;
                case 3 -> value == '\n' ? 4 : 0;
                default -> 0;
            };
        }
        return bytes.toString(StandardCharsets.US_ASCII);
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
