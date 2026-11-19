package org.wrj.haifa.ai.utilitymcp.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class McpRequestLoggingFilterTest {

    @Test
    void logsConnectionInitializationAndCompletionWithoutSensitivePayload() throws Exception {
        String sensitive = "super-secret-token-value";
        String body = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
                  "protocolVersion":"2025-11-25",
                  "clientInfo":{"name":"test-client","version":"1.2.3"},
                  "secret":"%s"}}
                """.formatted(sensitive);

        List<String> messages = invoke(body, "incoming-request-1", 200);

        assertThat(messages).anyMatch(value -> value.contains("event=mcp_connection_received")
                        && value.contains("requestId=incoming-request-1"))
                .anyMatch(value -> value.contains("event=mcp_initialize_request_completed")
                        && value.contains("clientName=test-client")
                        && value.contains("requestedProtocolVersion=2025-11-25"))
                .anyMatch(value -> value.contains("event=mcp_request_completed")
                        && value.contains("rpcMethod=initialize")
                        && value.contains("status=200"));
        assertThat(String.join("\n", messages)).doesNotContain(sensitive, "Authorization", "secret");
    }

    @Test
    void logsToolNameButNeverToolArguments() throws Exception {
        String sensitive = "private-user-query";
        String body = """
                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{
                  "name":"weather_current","arguments":{"query":"%s"}}}
                """.formatted(sensitive);

        List<String> messages = invoke(body, "invalid request id with spaces", 202);

        assertThat(messages).anyMatch(value -> value.contains("rpcMethod=tools/call")
                && value.contains("toolName=weather_current") && value.contains("status=202"));
        assertThat(String.join("\n", messages)).doesNotContain(sensitive, "arguments");
    }

    private static List<String> invoke(String body, String requestId, int status) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(McpRequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
            request.setRemoteAddr("192.0.2.123");
            request.addHeader(McpRequestLoggingFilter.REQUEST_ID_HEADER, requestId);
            request.addHeader("Authorization", "Bearer should-never-be-logged");
            request.setContent(body.getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();

            new McpRequestLoggingFilter(new ObjectMapper()).doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
                wrappedRequest.getInputStream().readAllBytes();
                ((jakarta.servlet.http.HttpServletResponse) wrappedResponse).setStatus(status);
            });

            assertThat(response.getHeader(McpRequestLoggingFilter.REQUEST_ID_HEADER)).isNotBlank();
            return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        }
        finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
