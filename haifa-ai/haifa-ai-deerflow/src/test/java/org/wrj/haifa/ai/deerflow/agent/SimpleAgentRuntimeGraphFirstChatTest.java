package org.wrj.haifa.ai.deerflow.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.config.GraphRuntimeMode;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class SimpleAgentRuntimeGraphFirstChatTest {

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

        // Enable Active Chat Graph mode
        properties.getGraph().setEnabled(true);
        properties.getGraph().setMode(GraphRuntimeMode.ACTIVE_CHAT);
    }

    @AfterEach
    void tearDown() {
        properties.getGraph().setEnabled(originalGraphEnabled);
        properties.getGraph().setMode(originalGraphMode);
    }

    @Test
    void verifiesDuplicateAssistantMessageInActiveChatGraph() {
        String threadId = "thread-first-chat-" + UUID.randomUUID();
        when(modelClient.streamGenerate(any()))
                .thenReturn(Mono.just(new ModelResponse("Hello from active chat graph")).flux());

        AgentRequest request = new AgentRequest(threadId, "Hello", null);
        List<AgentEvent> events = agentRuntime.stream(request).collectList().block();

        assertThat(events).isNotEmpty();

        List<MessageRecord> messages = messageStore.listByThread(threadId);

        // Output size of assistant messages to show the duplication
        List<MessageRecord> assistantMsgs = messages.stream()
                .filter(m -> m.role() == MessageRole.ASSISTANT)
                .toList();

        System.out.println("--- ASSISTANT MESSAGE COUNT: " + assistantMsgs.size());
        for (MessageRecord m : assistantMsgs) {
            System.out.println("Content: " + m.content());
        }

        // Assert exactly one assistant message was saved to message store
        assertThat(assistantMsgs).hasSize(1);
        assertThat(assistantMsgs.get(0).content()).isEqualTo("Hello from active chat graph");
    }
}
