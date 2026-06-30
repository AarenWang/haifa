package org.wrj.haifa.ai.deerflow.middleware;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MiddlewareChainTest {

    @Test
    void middlewaresExecuteInOrder() {
        StringBuilder trace = new StringBuilder();

        AgentMiddleware first = (ctx, next) -> {
            trace.append("first-before;");
            return next.next(ctx).doOnNext(p -> trace.append("first-after;"));
        };

        AgentMiddleware second = (ctx, next) -> {
            trace.append("second-before;");
            return next.next(ctx).doOnNext(p -> trace.append("second-after;"));
        };

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("test");
        properties.setWorkspaceRoot(".");
        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "hi", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(first, second));
        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.userPrompt()).contains("User request:");
                    assertThat(trace.toString()).isEqualTo("first-before;second-before;second-after;first-after;");
                })
                .verifyComplete();
    }

    @Test
    void dynamicContextMiddlewareInjectsContext() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("You are helpful.");
        properties.setWorkspaceRoot(".");
        properties.setCharBudget(0);

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "hi", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(
                new DynamicContextMiddleware(),
                new TokenBudgetMiddleware(),
                new ToolErrorHandlingMiddleware()
        ));

        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.systemPrompt()).contains("You are helpful.");
                    assertThat(prompt.systemPrompt()).contains("[Dynamic context]");
                    assertThat(prompt.systemPrompt()).contains("Current date/time:");
                    assertThat(prompt.systemPrompt()).contains("Workspace root:");
                })
                .verifyComplete();
    }

    @Test
    void tokenBudgetMiddlewareReturnsBudgetExceededWhenOverLimit() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("sys");
        properties.setWorkspaceRoot(".");
        properties.setCharBudget(10);

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "this is a long message", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(
                new DynamicContextMiddleware(),
                new TokenBudgetMiddleware(),
                new ToolErrorHandlingMiddleware()
        ));

        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.userPrompt()).startsWith("BUDGET_EXCEEDED:");
                })
                .verifyComplete();
    }

    @Test
    void tokenBudgetMiddlewarePassesThroughWhenUnderLimit() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("sys");
        properties.setWorkspaceRoot(".");
        properties.setCharBudget(1000);

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "hi", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(
                new DynamicContextMiddleware(),
                new TokenBudgetMiddleware(),
                new ToolErrorHandlingMiddleware()
        ));

        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.userPrompt()).doesNotStartWith("BUDGET_EXCEEDED:");
                    assertThat(prompt.userPrompt()).contains("User request:");
                })
                .verifyComplete();
    }

    @Test
    void tokenBudgetMiddlewareDisabledWhenBudgetIsZero() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("sys");
        properties.setWorkspaceRoot(".");
        properties.setCharBudget(0);

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "a very long message that would exceed any small budget", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(
                new TokenBudgetMiddleware()
        ));

        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.userPrompt()).doesNotStartWith("BUDGET_EXCEEDED:");
                })
                .verifyComplete();
    }

    @Test
    void toolErrorHandlingMiddlewareSanitizesFailedResults() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("sys");
        properties.setWorkspaceRoot(".");

        List<ToolResult> results = List.of(
                ToolResult.of("ok_tool", "fine"),
                ToolResult.of("bad_tool", "Tool failed: something went wrong")
        );

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "hi", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, results, properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(
                new ToolErrorHandlingMiddleware()
        ));

        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.userPrompt()).contains("fine");
                    assertThat(prompt.userPrompt()).contains("handled gracefully");
                    assertThat(prompt.userPrompt()).doesNotContain("<tool name=\"bad_tool\">\nTool failed:");
                })
                .verifyComplete();
    }
}
