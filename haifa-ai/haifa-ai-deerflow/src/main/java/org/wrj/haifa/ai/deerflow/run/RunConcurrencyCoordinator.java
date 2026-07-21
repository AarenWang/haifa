package org.wrj.haifa.ai.deerflow.run;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

/** Atomically applies the configured same-thread run policy before returning a new run. */
@Component
public class RunConcurrencyCoordinator {
    private static final Logger log = LoggerFactory.getLogger(RunConcurrencyCoordinator.class);
    private final RunManager runManager;
    private final RunCancellationService cancellationService;
    private final DeerFlowProperties properties;
    private final ConcurrentHashMap<String, Object> threadLocks = new ConcurrentHashMap<>();

    public RunConcurrencyCoordinator(RunManager runManager, RunCancellationService cancellationService,
            DeerFlowProperties properties) {
        this.runManager = runManager;
        this.cancellationService = cancellationService;
        this.properties = properties;
    }

    public RunRecord create(String threadId, String modelName, Map<String, Object> metadata) {
        Object lock = threadLocks.computeIfAbsent(threadId, ignored -> new Object());
        synchronized (lock) {
            if (properties.getRunConcurrencyMode() == RunConcurrencyMode.SINGLE_ACTIVE_RUN) {
                for (String runId : runManager.supersedeActiveRuns(threadId)) {
                    cancellationService.requestCancel(runId, "SUPERSEDED_BY_NEWER_RUN");
                    log.info("Superseded active run before creating replacement. threadId={}, runId={}",
                            threadId, runId);
                }
            }
            return runManager.create(threadId, modelName, metadata);
        }
    }
}
