package org.wrj.haifa.ai.deerflow.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.config.GraphRuntimeMode;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.provider.*;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class SimpleAgentRuntimeGraphFirstResearchTest {

    @Autowired
    private SimpleAgentRuntime agentRuntime;

    @Autowired
    private DeerFlowProperties properties;

    @Autowired
    private MessageStore messageStore;

    @MockitoBean
    private AgentModelClient modelClient;

    private boolean originalGraphEnabled;
    private GraphRuntimeMode originalGraphMode;

    @BeforeEach
    void setUp() {
        if (properties.getGraph() != null) {
            originalGraphEnabled = properties.getGraph().isEnabled();
            originalGraphMode = properties.getGraph().getMode();
        } else {
            originalGraphEnabled = false;
            originalGraphMode = GraphRuntimeMode.OFF;
        }

        // Enable Active Research Graph mode
        properties.getGraph().setEnabled(true);
        properties.getGraph().setMode(GraphRuntimeMode.ACTIVE_RESEARCH);
    }

    @AfterEach
    void tearDown() {
        properties.getGraph().setEnabled(originalGraphEnabled);
        properties.getGraph().setMode(originalGraphMode);
    }

    @Test
    void verifiesDuplicateReportArtifactInActiveResearchGraph() {
        String threadId = "thread-first-research-" + UUID.randomUUID();

        // Mock model returns for planner (not-json will trigger fallback planner) and synthesizer
        when(modelClient.generate(any()))
                .thenReturn(
                        Mono.just(new ModelResponse("not-json-plan")),
                        Mono.just(new ModelResponse("Mocked comprehensive final research report content.")));

        AgentRequest request = new AgentRequest(threadId, "research about deepseek", "zhipu");
        request = new AgentRequest(threadId, "research about deepseek", "zhipu", List.of(), RunMode.RESEARCH, ResearchOptions.defaults());

        List<AgentEvent> events = agentRuntime.stream(request).collectList().block();

        assertThat(events).isNotEmpty();

        // Let's count ARTIFACT_CREATED events to verify if there's a double write
        List<AgentEvent> artifactCreatedEvents = events.stream()
                .filter(e -> e.type() == AgentEventType.ARTIFACT_CREATED)
                .toList();

        System.out.println("--- ARTIFACT CREATED EVENT COUNT: " + artifactCreatedEvents.size());
        for (AgentEvent e : artifactCreatedEvents) {
            System.out.println("Content: " + e.content() + ", Metadata: " + e.metadata());
        }

        // Assert exactly one report artifact was created
        assertThat(artifactCreatedEvents).hasSize(1);

        List<MessageRecord> messages = messageStore.listByThread(threadId);
        List<MessageRecord> assistantMsgs = messages.stream()
                .filter(m -> m.role() == MessageRole.ASSISTANT)
                .toList();

        // Assert exactly one assistant message was saved to message store
        assertThat(assistantMsgs).hasSize(1);
        assertThat(assistantMsgs.get(0).content()).contains("Artifact");
    }

    private static WebSearchProvider searchProvider() {
        return new WebSearchProvider() {
            @Override
            public WebSearchProviderType type() {
                return WebSearchProviderType.ALIYUN;
            }

            @Override
            public String search(String query, int maxResults) {
                return "1. [DeepSeek Overview] https://example.com/deepseek\nSummary: Software adoption and engineering overview.";
            }
        };
    }

    private static WebFetchProvider fetchProvider() {
        return new WebFetchProvider() {
            @Override
            public WebFetchProviderType type() {
                return WebFetchProviderType.ALIYUN;
            }

            @Override
            public String fetch(String url) {
                return "DeepSeek engineering is highly cost-effective and performs well in benchmarks.";
            }
        };
    }

    @TestConfiguration
    static class OfflineProviderConfig {

        @Bean
        @Primary
        WebSearchProviderRegistry testSearchRegistry() {
            return new WebSearchProviderRegistry(List.of(searchProvider()));
        }

        @Bean
        @Primary
        WebFetchProviderRegistry testFetchRegistry() {
            return new WebFetchProviderRegistry(List.of(fetchProvider()));
        }
    }
}
