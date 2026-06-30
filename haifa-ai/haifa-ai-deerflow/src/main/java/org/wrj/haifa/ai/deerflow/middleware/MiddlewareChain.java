package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Mono;

public class MiddlewareChain {

    private final List<AgentMiddleware> middlewares;
    private final int index;

    public MiddlewareChain(List<AgentMiddleware> middlewares) {
        this(middlewares, 0);
    }

    private MiddlewareChain(List<AgentMiddleware> middlewares, int index) {
        this.middlewares = List.copyOf(middlewares);
        this.index = index;
    }

    public Mono<ModelPrompt> next(AgentRuntimeContext context) {
        if (index >= middlewares.size()) {
            return Mono.just(terminalPrompt(context));
        }
        return middlewares.get(index).apply(context, new MiddlewareChain(middlewares, index + 1));
    }

    private static ModelPrompt terminalPrompt(AgentRuntimeContext context) {
        String customPrompt = context.properties().getSystemPrompt();
        String uploadsPath = context.properties().getUploadsRoot();
        String workspacePath = context.properties().getWorkspaceRoot();
        String outputsPath = context.properties().getOutputsRoot();

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
