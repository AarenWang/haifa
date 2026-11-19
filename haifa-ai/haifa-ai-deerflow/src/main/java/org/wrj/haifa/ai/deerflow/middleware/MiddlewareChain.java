package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Mono;

public class MiddlewareChain {

    private static final Logger log = LoggerFactory.getLogger(MiddlewareChain.class);

    private final List<AgentMiddleware> middlewares;
    private final int index;
    private final Function<AgentRuntimeContext, ModelPrompt> terminalPromptFactory;

    public MiddlewareChain(List<AgentMiddleware> middlewares) {
        this(middlewares, 0, MiddlewareChain::terminalPrompt);
    }

    /**
     * Creates a phase-specific chain that decorates an already prepared base
     * prompt. This is used by the graph model-input phase so each model step is
     * derived from the same stable run prompt instead of accumulating changes
     * on the previous step's prompt.
     */
    public MiddlewareChain(List<AgentMiddleware> middlewares, ModelPrompt basePrompt) {
        this(middlewares, 0, context -> basePrompt);
    }

    private MiddlewareChain(List<AgentMiddleware> middlewares, int index,
            Function<AgentRuntimeContext, ModelPrompt> terminalPromptFactory) {
        this.middlewares = List.copyOf(middlewares);
        this.index = index;
        this.terminalPromptFactory = terminalPromptFactory;
    }

    public Mono<ModelPrompt> next(AgentRuntimeContext context) {
        if (index >= middlewares.size()) {
            return trace("MiddlewareChain", "terminalPrompt", context,
                    () -> Mono.fromSupplier(() -> terminalPromptFactory.apply(context)));
        }
        AgentMiddleware middleware = middlewares.get(index);
        String middlewareName = middlewareName(middleware);
        return trace(middlewareName, "apply", context,
                () -> middleware.apply(context,
                        new MiddlewareChain(middlewares, index + 1, terminalPromptFactory)));
    }

    private static <T> Mono<T> trace(String middlewareName, String method,
            AgentRuntimeContext context, Supplier<Mono<T>> operationSupplier) {
        long startedAt = System.nanoTime();
        log.info("layer=middleware phase=enter middleware={} method={} runId={} threadId={}",
                middlewareName, method, runId(context), threadId(context));
        try {
            return operationSupplier.get().doFinally(signal -> log.info(
                    "layer=middleware phase=exit middleware={} method={} status={} durationMs={} runId={} threadId={}",
                    middlewareName, method, signal,
                    (System.nanoTime() - startedAt) / 1_000_000,
                    runId(context), threadId(context)));
        } catch (RuntimeException ex) {
            log.error("layer=middleware phase=exit middleware={} method={} status=sync_error durationMs={} runId={} threadId={}",
                    middlewareName, method, (System.nanoTime() - startedAt) / 1_000_000,
                    runId(context), threadId(context), ex);
            throw ex;
        }
    }

    private static String runId(AgentRuntimeContext context) {
        return context == null || context.config() == null ? "" : context.config().runId();
    }

    private static String threadId(AgentRuntimeContext context) {
        return context == null || context.config() == null ? "" : context.config().threadId();
    }

    private static String middlewareName(AgentMiddleware middleware) {
        String simpleName = middleware.getClass().getSimpleName();
        return simpleName == null || simpleName.isBlank()
                ? middleware.getClass().getName()
                : simpleName;
    }

    private static ModelPrompt terminalPrompt(AgentRuntimeContext context) {
        String customPrompt = context.properties().getSystemPrompt();
        String uploadsPath = UserDataPathResolver.VIRTUAL_UPLOADS_ROOT;
        String workspacePath = UserDataPathResolver.VIRTUAL_WORKSPACE_ROOT;
        String outputsPath = UserDataPathResolver.VIRTUAL_OUTPUTS_ROOT;

        String systemPrompt = org.wrj.haifa.ai.deerflow.prompt.LeadAgentPromptTemplate.build(
                context.config().modelName(),
                null, // soul
                null, // skillsSection will be injected by SkillActivationMiddleware
                uploadsPath,
                workspacePath,
                outputsPath,
                customPrompt
        );

        return new ModelPrompt(
                systemPrompt,
                buildUserPrompt(context.request().message(), context.toolResults()),
                context.config().modelName()
        );
    }

    private static String buildUserPrompt(String userMessage, List<org.wrj.haifa.ai.deerflow.tool.ToolResult> toolResults) {
        StringBuilder builder = new StringBuilder();
        builder.append("User request:\n").append(userMessage == null ? "" : userMessage).append("\n\n");
        if (toolResults.isEmpty()) {
            builder.append("No local tools were triggered for this request.\n");
            return builder.toString();
        }
        builder.append("Local tool observations:\n");
        for (org.wrj.haifa.ai.deerflow.tool.ToolResult result : toolResults) {
            builder.append("\n<tool name=\"").append(result.toolName()).append("\">\n")
                    .append(result.content())
                    .append("\n</tool>\n");
        }
        return builder.toString();
    }
}
