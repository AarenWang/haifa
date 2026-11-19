package org.wrj.haifa.ai.deerflow.graph;

public class GraphExecutorRejectedException extends RuntimeException {

    private final String runId;
    private final GraphExecutionManager.ExecutorStatus executorStatus;

    public GraphExecutorRejectedException(String runId, GraphExecutionManager.ExecutorStatus executorStatus,
            Throwable cause) {
        super("Graph task rejected: runId=" + (runId == null ? "" : runId)
                + ", executor=" + executorStatus.name()
                + ", active=" + executorStatus.activeCount()
                + ", queue=" + executorStatus.queueSize(), cause);
        this.runId = runId == null ? "" : runId;
        this.executorStatus = executorStatus;
    }

    public String getRunId() {
        return runId;
    }

    public GraphExecutionManager.ExecutorStatus getExecutorStatus() {
        return executorStatus;
    }
}
