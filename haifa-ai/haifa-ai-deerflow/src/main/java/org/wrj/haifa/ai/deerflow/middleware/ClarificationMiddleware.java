package org.wrj.haifa.ai.deerflow.middleware;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStatus;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStore;
import reactor.core.publisher.Mono;

/**
 * Middleware that manages clarification intercepting and resume prompt injection.
 * 1. Blocks new runs if there is a pending clarification.
 * 2. Injects user clarification answer into the prompt for resumed runs.
 */
@Component
@MiddlewareOrder(18) // after persona, before todo injection
public class ClarificationMiddleware implements AgentMiddleware {

    private final ClarificationStore clarificationStore;

    public ClarificationMiddleware(ClarificationStore clarificationStore) {
        this.clarificationStore = clarificationStore;
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain chain) {
        if (clarificationStore == null) {
            return chain.next(context);
        }

        String threadId = context.config().threadId();

        // 1. Block duplicate runs if there is a pending clarification
        Optional<ClarificationRecord> pending = clarificationStore.findPending(threadId);
        if (pending.isPresent()) {
            return Mono.error(new IllegalStateException(
                    "Cannot start a new run on thread " + threadId + " because there is a pending clarification question. Please answer it first."
            ));
        }

        // 2. Inject clarification answer block if this is a resumed run
        Optional<ClarificationRecord> answered = findAnsweredForResume(context);
        return chain.next(context).map(prompt -> {
            if (answered.isEmpty()) {
                return prompt;
            }
            String resumeBlock = """

                    <clarification_answer>
                    User answered a previous clarification question.
                    Question: %s
                    Answer: %s
                    Continue the original task using this answer.
                    </clarification_answer>
                    """.formatted(answered.get().question(), answered.get().answer()).trim();
            
            String userPrompt = prompt.userPrompt();
            String updatedUserPrompt = (userPrompt == null || userPrompt.isBlank())
                    ? resumeBlock
                    : userPrompt + "\n\n" + resumeBlock;

            return new ModelPrompt(
                    prompt.systemPrompt(),
                    updatedUserPrompt,
                    prompt.modelName()
            );
        });
    }

    private Optional<ClarificationRecord> findAnsweredForResume(AgentRuntimeContext context) {
        if (context.config() == null || context.config().metadata() == null) {
            return Optional.empty();
        }
        Object clarificationIdObj = context.config().metadata().get("clarificationId");
        if (clarificationIdObj instanceof String clarificationId) {
            return clarificationStore.find(clarificationId)
                    .filter(c -> c.status() == ClarificationStatus.ANSWERED);
        }
        return Optional.empty();
    }
}
