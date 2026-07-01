package org.wrj.haifa.ai.deerflow.middleware;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchClarificationStore;
import reactor.core.publisher.Mono;

/**
 * Middleware that intercepts pending clarifications from a previous run.
 * If the user has answered a clarification question on this thread, the
 * original message is merged with the user's clarification reply and the
 * pending state is consumed so the run can resume normally.
 */
@Component
@MiddlewareOrder(18) // after persona, before todo injection
public class ClarificationMiddleware implements AgentMiddleware {

    private final ResearchClarificationStore clarificationStore;

    public ClarificationMiddleware(ResearchClarificationStore clarificationStore) {
        this.clarificationStore = clarificationStore;
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain chain) {
        String threadId = context.config().threadId();
        if (clarificationStore == null) {
            return chain.next(context);
        }

        var pending = clarificationStore.consume(threadId);
        if (pending.isPresent()) {
            var clarification = pending.get();
            String effectiveMessage = """
                    %s

                    <clarification_answer>
                    澄清问题：%s
                    用户回答：%s
                    请基于该回答继续完成原始任务。
                    </clarification_answer>
                    """.formatted(
                    clarification.originalMessage(),
                    clarification.question(),
                    context.request().message()
            ).trim();
            AgentRequest effectiveRequest = new AgentRequest(
                    context.request().threadId(),
                    effectiveMessage,
                    context.request().model(),
                    context.request().uploadedFileIds(),
                    context.request().mode(),
                    context.request().researchOptions(),
                    context.request().userId()
            );
            return chain.next(context.withRequest(effectiveRequest));
        }

        return chain.next(context);
    }
}
