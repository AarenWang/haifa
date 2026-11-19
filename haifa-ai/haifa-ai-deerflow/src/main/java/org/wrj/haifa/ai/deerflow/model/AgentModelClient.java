package org.wrj.haifa.ai.deerflow.model;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface AgentModelClient {

    Mono<ModelResponse> generate(ModelPrompt prompt);

    default Flux<ModelResponse> streamGenerate(ModelPrompt prompt) {
        return generate(prompt).flux();
    }
}
