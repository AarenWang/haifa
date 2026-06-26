package org.wrj.haifa.ai.deerflow.middleware;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Mono;

@Component
@MiddlewareOrder(10)
public class DynamicContextMiddleware implements AgentMiddleware {

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        return next.next(context)
                .map(prompt -> {
                    String dynamicContext = buildDynamicContext(context);
                    String baseSystem = context.properties().getSystemPrompt();
                    String updatedSystem = (baseSystem == null || baseSystem.isBlank())
                            ? dynamicContext
                            : baseSystem + "\n\n" + dynamicContext;
                    return new ModelPrompt(updatedSystem, prompt.userPrompt(), prompt.modelName());
                });
    }

    private String buildDynamicContext(AgentRuntimeContext context) {
        return """
                [Dynamic context]
                - Current date/time: %s
                - Workspace root: %s
                """.formatted(
                        ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        context.config().workspaceRoot().toAbsolutePath()
                );
    }
}
