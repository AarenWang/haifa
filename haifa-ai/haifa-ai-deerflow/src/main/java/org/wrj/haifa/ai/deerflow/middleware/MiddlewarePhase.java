package org.wrj.haifa.ai.deerflow.middleware;

/**
 * Explicit lifecycle phase for an {@link AgentMiddleware}.
 *
 * <p>The graph runtime uses this phase to give middleware a deterministic
 * execution frequency instead of running one undifferentiated chain at
 * arbitrary orchestration boundaries.</p>
 */
public enum MiddlewarePhase {
    /** Executed once while preparing a run. */
    RUN_PREPARATION,
    /** Executed before every model invocation. */
    MODEL_INPUT,
    /** Executed at the tool-result boundary, not during prompt assembly. */
    TOOL_RESULT
}
