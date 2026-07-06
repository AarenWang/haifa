package org.wrj.haifa.ai.deerflow.middleware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentClarificationStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClarificationMiddlewareTest {

    @Autowired
    private AgentClarificationStore store;

    private ClarificationMiddleware middleware;

    @BeforeEach
    void setUp() {
        store.clearAll();
        middleware = new ClarificationMiddleware(store);
    }

    @Test
    void blocksNewRunWhenPendingClarificationExists() {
        // Create a pending clarification
        store.create("thread-block", "run-1", "Are you sure?", "missing_info", "");

        AgentRunConfig config = new AgentRunConfig(
                "thread-block", "run-2", "gpt-4", true, false, 5, Path.of("."), RunMode.CHAT, ResearchOptions.defaults(), Map.of()
        );
        AgentRequest request = new AgentRequest("thread-block", "Hello", "gpt-4");
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), new DeerFlowProperties(), List.of());

        MiddlewareChain chain = new MiddlewareChain(List.of());

        Mono<ModelPrompt> result = middleware.apply(context, chain);

        StepVerifier.create(result)
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void injectsAnswerWhenResumed() {
        // Create answered clarification
        ClarificationRecord record = store.create("thread-resume", "run-1", "Are you sure?", "missing_info", "");
        store.answer(record.clarificationId(), "Yes, absolutely");

        AgentRunConfig config = new AgentRunConfig(
                "thread-resume", "run-2", "gpt-4", true, false, 5, Path.of("."), RunMode.CHAT, ResearchOptions.defaults(),
                Map.of("clarificationId", record.clarificationId()) // config contains clarificationId matching resumed run
        );
        AgentRequest request = new AgentRequest("thread-resume", "Hello", "gpt-4");
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), new DeerFlowProperties(), List.of());

        // Simple downstream middleware chain that returns a prompt with empty prompts
        MiddlewareChain chain = new MiddlewareChain(List.of(
                (ctx, next) -> Mono.just(new ModelPrompt("System content", "User content", "gpt-4"))
        ));

        Mono<ModelPrompt> result = middleware.apply(context, chain);

        StepVerifier.create(result)
                .assertNext(prompt -> {
                    assertThat(prompt.userPrompt()).contains("<clarification_answer>");
                    assertThat(prompt.userPrompt()).contains("Question: Are you sure?");
                    assertThat(prompt.userPrompt()).contains("Answer: Yes, absolutely");
                })
                .verifyComplete();
    }
}
