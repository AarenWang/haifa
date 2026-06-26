package org.wrj.haifa.ai.deerflow.middleware;

import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Mono;

public interface AgentMiddleware {

    Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next);
}
