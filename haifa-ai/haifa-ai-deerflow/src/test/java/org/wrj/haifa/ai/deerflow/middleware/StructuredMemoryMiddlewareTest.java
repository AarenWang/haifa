package org.wrj.haifa.ai.deerflow.middleware;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.memory.MemoryFactRecord;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryFactStore;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class StructuredMemoryMiddlewareTest {

    @Test
    void testStructuredMemoryMiddlewareInjectsMemoryFacts() {
        MemoryFactStore factStore = Mockito.mock(MemoryFactStore.class);
        MemoryFactRecord fact1 = new MemoryFactRecord(
                "f1", "default-user", null, "constraint", "Always use Java 25", "reflection", "t1", "r1", 0.9, "active", null, Instant.now(), Instant.now(), Instant.now()
        );
        MemoryFactRecord fact2 = new MemoryFactRecord(
                "f2", "default-user", null, "preference", "User prefers dark theme", "reflection", "t1", "r1", 0.8, "active", null, Instant.now(), Instant.now(), Instant.now()
        );
        when(factStore.findByUserIdAndStatus(anyString(), anyString())).thenReturn(List.of(fact1, fact2));

        StructuredMemoryMiddleware middleware = new StructuredMemoryMiddleware(factStore);

        AgentRunConfig runConfig = new AgentRunConfig("thread-1", "run-1", "test-model", false, false,
                4, Path.of("."), Map.of());
        AgentRequest request = new AgentRequest("thread-1", "Dark theme config", "test-model");
        AgentRuntimeContext context = AgentRuntimeContext.of(runConfig, request, List.of(), new DeerFlowProperties());

        ModelPrompt prompt = new ModelPrompt("Original system prompt", "User request", "test-model");
        MiddlewareChain chain = new MiddlewareChain(List.of(middleware)) {
            @Override
            public Mono<ModelPrompt> next(AgentRuntimeContext ctx) {
                return Mono.just(prompt);
            }
        };

        ModelPrompt processed = middleware.apply(context, chain).block();

        assertThat(processed).isNotNull();
        assertThat(processed.systemPrompt())
                .contains("Original system prompt")
                .contains("<system-reminder>")
                .contains("<memory>")
                .contains("- Always use Java 25")
                .contains("- User prefers dark theme")
                .contains("</memory>")
                .contains("</system-reminder>");
    }

    @Test
    void testStructuredMemoryMiddlewareDoesNotChangePromptWhenNoFacts() {
        MemoryFactStore factStore = Mockito.mock(MemoryFactStore.class);
        when(factStore.findByUserIdAndStatus(anyString(), anyString())).thenReturn(List.of());

        StructuredMemoryMiddleware middleware = new StructuredMemoryMiddleware(factStore);

        AgentRunConfig runConfig = new AgentRunConfig("thread-1", "run-1", "test-model", false, false,
                4, Path.of("."), Map.of());
        AgentRequest request = new AgentRequest("thread-1", "Hello", "test-model");
        AgentRuntimeContext context = AgentRuntimeContext.of(runConfig, request, List.of(), new DeerFlowProperties());

        ModelPrompt prompt = new ModelPrompt("Original system prompt", "User request", "test-model");
        MiddlewareChain chain = new MiddlewareChain(List.of(middleware)) {
            @Override
            public Mono<ModelPrompt> next(AgentRuntimeContext ctx) {
                return Mono.just(prompt);
            }
        };

        ModelPrompt processed = middleware.apply(context, chain).block();

        assertThat(processed).isNotNull();
        assertThat(processed.systemPrompt()).isEqualTo("Original system prompt");
    }
}
