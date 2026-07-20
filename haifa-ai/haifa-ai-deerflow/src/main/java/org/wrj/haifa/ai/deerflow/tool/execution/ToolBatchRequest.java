package org.wrj.haifa.ai.deerflow.tool.execution;

import java.util.List;
import org.wrj.haifa.ai.deerflow.run.RunCancellationToken;

public record ToolBatchRequest<T>(
        String runId,
        List<ToolExecutionTask<T>> tasks,
        int maxConcurrency,
        RunCancellationToken cancellationToken
) {
    public ToolBatchRequest {
        runId = runId == null ? "" : runId;
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency must be positive");
        }
        cancellationToken = cancellationToken == null ? new RunCancellationToken(runId) : cancellationToken;
    }
}
