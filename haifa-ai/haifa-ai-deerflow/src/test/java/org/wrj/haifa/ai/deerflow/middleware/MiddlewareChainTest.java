package org.wrj.haifa.ai.deerflow.middleware;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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
    void logsMiddlewareEntryAndExitWithMiddlewareLayer() {
        Logger logger = (Logger) LoggerFactory.getLogger(MiddlewareChain.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            DeerFlowProperties properties = new DeerFlowProperties();
            properties.setSystemPrompt("test");
            properties.setWorkspaceRoot(".");
            AgentRunConfig config = new AgentRunConfig(
                    "thread-log", "run-log", "model", false, false, 4, Path.of("."), java.util.Map.of());
            AgentRuntimeContext context = AgentRuntimeContext.of(
                    config, new AgentRequest("thread-log", "hello", null), List.of(), properties);

            new MiddlewareChain(List.of(new DynamicContextMiddleware())).next(context).block();

            List<String> messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
            assertThat(messages).anyMatch(message -> message.contains(
                    "layer=middleware phase=enter middleware=DynamicContextMiddleware method=apply runId=run-log threadId=thread-log"));
            assertThat(messages).anyMatch(message -> message.contains(
                    "layer=middleware phase=exit middleware=DynamicContextMiddleware method=apply status=onComplete"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

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
    void dynamicContextMiddlewarePreservesExistingSystemPromptAugmentations() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("Base system");
        properties.setWorkspaceRoot(".");

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "hi", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        AgentMiddleware prependSection = (ctx, next) -> next.next(ctx)
                .map(prompt -> new ModelPrompt(prompt.systemPrompt() + "\n\n[Active skills]\n- research",
                        prompt.userPrompt(), prompt.modelName()));

        MiddlewareChain chain = new MiddlewareChain(List.of(
                prependSection,
                new DynamicContextMiddleware()
        ));

        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.systemPrompt()).contains("Base system");
                    assertThat(prompt.systemPrompt()).contains("[Active skills]");
                    assertThat(prompt.systemPrompt()).contains("[Dynamic context]");
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
        properties.setCharBudget(10000);

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

    @Test
    void tokenBudgetMiddlewareMeasuresFinalEnrichedPrompt() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("base system");
        properties.setWorkspaceRoot(".");
        properties.setSkillsEnabled(true);
        // Set budget small enough that base passes, but base + dynamic context + skills fails
        properties.setCharBudget(200);

        org.wrj.haifa.ai.deerflow.skill.SkillStorage mockStorage = new org.wrj.haifa.ai.deerflow.skill.FileSystemSkillStorage(null, null);
        org.wrj.haifa.ai.deerflow.skill.SlashSkillResolver mockResolver = new org.wrj.haifa.ai.deerflow.skill.SlashSkillResolver(mockStorage);

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "hi", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        // Chain with budget first (order 1), then skills (order 5), then dynamic context (order 10)
        MiddlewareChain chain = new MiddlewareChain(List.of(
                new TokenBudgetMiddleware(),
                new SkillActivationMiddleware(mockResolver, mockStorage, properties),
                new DynamicContextMiddleware()
        ));

        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.userPrompt()).startsWith("BUDGET_EXCEEDED:");
                    assertThat(prompt.userPrompt()).contains("exceed the character budget");
                })
                .verifyComplete();
    }
}
