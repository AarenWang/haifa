package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import reactor.core.publisher.Mono;

@Component
@MiddlewareOrder(30)
@MiddlewareLifecycle(MiddlewarePhase.TOOL_RESULT)
public class ToolErrorHandlingMiddleware implements AgentMiddleware {

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        List<ToolResult> sanitized = context.toolResults().stream()
                .map(this::sanitize)
                .toList();
        return next.next(context.withToolResults(sanitized));
    }

    private ToolResult sanitize(ToolResult result) {
        if (!result.isSuccess() || (result.content() != null && result.content().startsWith("Tool failed:"))) {
            return new ToolResult(
                    result.toolName(),
                    result.status(),
                    "Observation: the tool encountered an error but was handled gracefully. " + result.content(),
                    result.metadata()
            );
        }
        return result;
    }
}
