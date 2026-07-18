package org.wrj.haifa.ai.deerflow.middleware;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlock;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlockPlacement;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlockType;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheability;
import org.wrj.haifa.ai.deerflow.model.cache.PromptStability;
import org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver;
import reactor.core.publisher.Mono;

@Component
@MiddlewareOrder(10)
@MiddlewareLifecycle(MiddlewarePhase.MODEL_INPUT)
public class DynamicContextMiddleware implements AgentMiddleware {

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        return next.next(context)
                .map(prompt -> {
                    String staticWorkspacePolicy = buildStaticWorkspacePolicy();
                    String dynamicDateEnvelope = buildDynamicDateEnvelope();

                    String baseSystem = prompt.systemPrompt();
                    String updatedSystem = (baseSystem == null || baseSystem.isBlank())
                            ? staticWorkspacePolicy
                            : baseSystem + "\n\n" + staticWorkspacePolicy;

                    String baseUser = prompt.userPrompt();
                    String updatedUser = (baseUser == null || baseUser.isBlank())
                            ? dynamicDateEnvelope
                            : baseUser + "\n\n" + dynamicDateEnvelope;

                    PromptBlock workspaceBlock = new PromptBlock(
                            PromptBlockType.WORKSPACE_POLICY,
                            PromptBlockPlacement.SYSTEM_PREFIX,
                            PromptStability.GLOBAL,
                            PromptCacheability.CACHEABLE,
                            "v1",
                            staticWorkspacePolicy
                    );

                    PromptBlock dynamicBlock = new PromptBlock(
                            PromptBlockType.DYNAMIC_CONTEXT,
                            PromptBlockPlacement.DYNAMIC_TAIL,
                            PromptStability.TURN,
                            PromptCacheability.NOT_CACHEABLE,
                            "v1",
                            dynamicDateEnvelope
                    );

                    java.util.List<PromptBlock> blocks = new java.util.ArrayList<>(prompt.promptBlocks());
                    blocks.add(workspaceBlock);
                    blocks.add(dynamicBlock);

                    return prompt
                            .withSystemPrompt(updatedSystem)
                            .withUserPrompt(updatedUser)
                            .withPromptBlocks(blocks);
                });
    }

    private String buildStaticWorkspacePolicy() {
        return """
                [Workspace policy]
                - User uploads: %s
                - Workspace root: %s
                - Output files: %s
                - Use the workspace for temporary work. Save final deliverables under the output files directory. Do not write to uploads; uploads is written by the frontend upload service.
                """.formatted(
                        UserDataPathResolver.VIRTUAL_UPLOADS_ROOT,
                        UserDataPathResolver.VIRTUAL_WORKSPACE_ROOT,
                        UserDataPathResolver.VIRTUAL_OUTPUTS_ROOT
                ).trim();
    }

    private String buildDynamicDateEnvelope() {
        return """
                <runtime_context dynamic="true">
                Current date: %s
                </runtime_context>
                """.formatted(LocalDate.now().toString()).trim();
    }
}
