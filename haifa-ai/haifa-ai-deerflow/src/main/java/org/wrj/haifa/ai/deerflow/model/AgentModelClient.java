package org.wrj.haifa.ai.deerflow.model;

import reactor.core.publisher.Mono;

public interface AgentModelClient {

    Mono<String> generate(ModelPrompt prompt);
}
