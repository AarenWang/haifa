package org.wrj.haifa.ai.spring.toolcalling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MimeTypeUtils;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class GeoChatControllerIntegrationTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("haifa.ai.tool-calling.geo.base-url", () -> mockWebServer.url("/").toString());
        registry.add("haifa.ai.tool-calling.geo.path", () -> "/facts");
        registry.add("haifa.ai.tool-calling.geo.api-key", () -> "test-key");
        registry.add("haifa.ai.tool-calling.model", () -> "demo-model");
    }

    @Test
    void chatEndpointDelegatesToGeoTool() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{" +
                        "\"title\":\"Beijing\"," +
                        "\"summary\":\"Capital city of China.\"," +
                        "\"highlights\":[\"Hosts the Forbidden City\"]}")
                .addHeader("Content-Type", MimeTypeUtils.APPLICATION_JSON_VALUE));

        mockMvc.perform(post("/api/geo/chat")
                .contentType(MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("{\"message\":\"Tell me about Beijing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("Beijing")))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("demo-model")));

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("X-API-Key")).isEqualTo("test-key");
        assertThat(recordedRequest.getRequestUrl().encodedPath()).isEqualTo("/facts");
        assertThat(recordedRequest.getRequestUrl().queryParameter("q")).isEqualTo("Tell me about Beijing");
    }
}
