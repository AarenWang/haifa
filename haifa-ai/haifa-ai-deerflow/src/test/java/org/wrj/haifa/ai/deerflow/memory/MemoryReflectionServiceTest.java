package org.wrj.haifa.ai.deerflow.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryCandidateStore;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryFactStore;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemoryReflectionServiceTest {

    @Test
    void testReflectExtractsAndSavesCandidates() {
        MessageStore messageStore = mock(MessageStore.class);
        MemoryFactStore factStore = mock(MemoryFactStore.class);
        MemoryCandidateStore candidateStore = mock(MemoryCandidateStore.class);
        AgentModelClient modelClient = mock(AgentModelClient.class);
        RunManager runManager = mock(RunManager.class);
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. Mock runManager
        RunRecord run = new RunRecord("run-1", "thread-1", "test-model", null, null, Map.of("mode", "chat", "userId", "test-user-123"), Instant.now(), Instant.now());
        when(runManager.find("run-1")).thenReturn(Optional.of(run));

        // 2. Mock messageStore
        MessageRecord m1 = new MessageRecord("msg-1", "thread-1", "run-1", MessageRole.USER, "Remember to use Java 25.", Map.of(), Instant.now());
        MessageRecord m2 = new MessageRecord("msg-2", "thread-1", "run-1", MessageRole.ASSISTANT, "Okay, I will remember that.", Map.of(), Instant.now());
        when(messageStore.listByRun("run-1")).thenReturn(List.of(m1, m2));

        // 3. Mock factStore
        when(factStore.findByUserIdAndStatus(anyString(), anyString())).thenReturn(List.of());

        // 4. Mock modelClient returning JSON extraction
        String jsonResponse = """
                [
                  {
                    "category": "preference",
                    "content": "User prefers Java 25",
                    "action": "ADD",
                    "targetFactId": null,
                    "sourceError": "Old wrong instruction",
                    "confidence": 0.9
                  }
                ]
                """;
        when(modelClient.generate(any(ModelPrompt.class))).thenReturn(Mono.just(new ModelResponse(jsonResponse)));

        MemoryReflectionService service = new MemoryReflectionService(
                messageStore, factStore, candidateStore, modelClient, objectMapper, runManager
        );

        // Run reflect synchronously
        service.reflect("thread-1", "run-1");

        // Verify save was called on candidateStore
        ArgumentCaptor<MemoryCandidateRecord> captor = ArgumentCaptor.forClass(MemoryCandidateRecord.class);
        verify(candidateStore, times(1)).save(captor.capture());

        MemoryCandidateRecord saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.userId()).isEqualTo("test-user-123");
        assertThat(saved.category()).isEqualTo("preference");
        assertThat(saved.content()).isEqualTo("User prefers Java 25");
        assertThat(saved.confidence()).isEqualTo(0.9);
        assertThat(saved.status()).isEqualTo("pending");
        assertThat(saved.action()).isEqualTo("ADD");
        assertThat(saved.targetFactId()).isNull();
        assertThat(saved.sourceError()).isEqualTo("Old wrong instruction");
        assertThat(saved.sourceThreadId()).isEqualTo("thread-1");
        assertThat(saved.sourceRunId()).isEqualTo("run-1");
    }
}
