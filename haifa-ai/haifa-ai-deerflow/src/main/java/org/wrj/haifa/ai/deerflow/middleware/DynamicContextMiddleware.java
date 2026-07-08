package org.wrj.haifa.ai.deerflow.middleware;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver;
import reactor.core.publisher.Mono;

@Component
@MiddlewareOrder(10)
public class DynamicContextMiddleware implements AgentMiddleware {

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        return next.next(context)
                .map(prompt -> {
                    String dynamicContext = buildDynamicContext(context);
                    String baseSystem = prompt.systemPrompt();
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
                - User uploads: %s
                - Workspace root: %s
                - Output files: %s
                - Use the workspace for temporary work. Save final deliverables under the output files directory. Do not write to uploads; uploads is written by the frontend upload service.
                """.formatted(
                        ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        UserDataPathResolver.VIRTUAL_UPLOADS_ROOT,
                        UserDataPathResolver.VIRTUAL_WORKSPACE_ROOT,
                        UserDataPathResolver.VIRTUAL_OUTPUTS_ROOT
                );
    }
}
