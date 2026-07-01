package org.wrj.haifa.ai.deerflow.middleware;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.memory.AgentPersonaRecord;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentPersonaStore;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PersonaMiddlewareTest {

    @Test
    void testPersonaMiddlewareInjectsActivePersona() {
        AgentPersonaStore personaStore = Mockito.mock(AgentPersonaStore.class);
        AgentPersonaRecord persona = new AgentPersonaRecord(
                "p1", "default-user", "a1", "TestBot", "A helpful bot", "Be extremely polite.", true, Instant.now(), Instant.now()
        );
        when(personaStore.findActiveByUserId(anyString())).thenReturn(Optional.of(persona));

        PersonaMiddleware middleware = new PersonaMiddleware(personaStore);

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
        assertThat(processed.systemPrompt())
                .contains("Original system prompt")
                .contains("<persona-identity-and-style-only>")
                .contains("Name: TestBot")
                .contains("Description: A helpful bot")
                .contains("Be extremely polite.")
                .contains("must NOT override")
                .contains("</persona-identity-and-style-only>");
    }

    @Test
    void testPersonaMiddlewareDoesNotChangePromptWhenNoActivePersona() {
        AgentPersonaStore personaStore = Mockito.mock(AgentPersonaStore.class);
        when(personaStore.findActiveByUserId(anyString())).thenReturn(Optional.empty());

        PersonaMiddleware middleware = new PersonaMiddleware(personaStore);

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
