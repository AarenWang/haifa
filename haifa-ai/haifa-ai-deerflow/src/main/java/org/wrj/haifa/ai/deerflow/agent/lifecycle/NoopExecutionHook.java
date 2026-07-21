package org.wrj.haifa.ai.deerflow.agent.lifecycle;

public final class NoopExecutionHook implements AgentExecutionHook {
    public static final NoopExecutionHook INSTANCE = new NoopExecutionHook();
    private NoopExecutionHook() { }
}
