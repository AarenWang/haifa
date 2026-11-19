package org.wrj.haifa.ai.deerflow.middleware;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Mono;

@Component
@MiddlewareOrder(1)
@MiddlewareLifecycle(MiddlewarePhase.MODEL_INPUT)
public class TokenBudgetMiddleware implements AgentMiddleware {

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        int budget = context.properties().getCharBudget();
        if (budget <= 0) {
            return next.next(context);
        }
        return next.next(context).map(prompt -> {
            long total = estimatePromptChars(prompt);
            if (total > budget) {
                String msg = "Request + tool observations exceed the character budget ("
                        + total + "/" + budget + "). Please simplify your request.";
                return new ModelPrompt(
                        prompt.systemPrompt(),
                        "BUDGET_EXCEEDED: " + msg,
                        prompt.modelName()
                );
            }
            return prompt;
        });
    }

    private long estimatePromptChars(ModelPrompt prompt) {
        long total = prompt.systemPrompt() == null ? 0 : prompt.systemPrompt().length();
        total += prompt.userPrompt() == null ? 0 : prompt.userPrompt().length();
        return total;
    }
}
