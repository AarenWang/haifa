package org.wrj.haifa.ai.deerflow.agent;

import reactor.core.publisher.Flux;

public interface AgentRuntime {

    Flux<AgentEvent> stream(AgentRequest request);
}
