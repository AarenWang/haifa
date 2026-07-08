package org.wrj.haifa.ai.deerflow.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wrj.haifa.ai.deerflow.artifact.ReportWriteResult;
import org.wrj.haifa.ai.deerflow.memory.MemoryReflectionService;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GraphLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(GraphLifecycleService.class);

    private final RunManager runManager;
    private final ThreadManager threadManager;
    private final MessageStore messageStore;
    private final MemoryReflectionService memoryReflectionService;

    @Autowired
    public GraphLifecycleService(RunManager runManager,
                                 ThreadManager threadManager,
                                 MessageStore messageStore,
                                 @Autowired(required = false) MemoryReflectionService memoryReflectionService) {
        this.runManager = runManager;
        this.threadManager = threadManager;
        this.messageStore = messageStore;
        this.memoryReflectionService = memoryReflectionService;
    }

    public void completeChat(String runId, String threadId, String finalAnswer, int steps, int toolCount) {
        log.info("Completing chat run via GraphLifecycleService. runId={}, threadId={}, steps={}, toolCount={}",
                runId, threadId, steps, toolCount);
        this.runManager.markCompleted(runId);
        this.threadManager.touch(threadId);
        this.messageStore.add(threadId, runId, MessageRole.ASSISTANT, finalAnswer,
                Map.of("toolCount", toolCount, "mode", "chat", "steps", steps));
        if (this.memoryReflectionService != null) {
            this.memoryReflectionService.reflectAsync(threadId, runId);
        }
    }

    public void completeResearch(String runId, String threadId, ReportWriteResult result, int toolCount) {
        log.info("Completing research run via GraphLifecycleService. runId={}, threadId={}, toolCount={}",
                runId, threadId, toolCount);
        this.runManager.markCompleted(runId);
        this.threadManager.touch(threadId);

        String summary = buildArtifactSummary(result);
        this.messageStore.add(threadId, runId, MessageRole.ASSISTANT, summary,
                Map.of("mode", "research",
                        "artifactId", result.artifact().artifactId(),
                        "filename", result.artifact().filename(),
                        "toolCount", toolCount));

        if (this.memoryReflectionService != null) {
            this.memoryReflectionService.reflectAsync(threadId, runId);
        }
    }

    public void failRun(String runId, String threadId, String errorMessage) {
        log.warn("Failing run via GraphLifecycleService. runId={}, threadId={}, error={}", runId, threadId, errorMessage);
        this.runManager.markFailed(runId, errorMessage);
        this.threadManager.touch(threadId);
        this.messageStore.add(threadId, runId, MessageRole.SYSTEM, errorMessage,
                Map.of("status", "FAILED", "errorType", "GraphException"));
    }

    private static String buildArtifactSummary(ReportWriteResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summary\n\n").append(result.summary())
          .append("\n\nLimitations\n\n").append(result.limitations());

        List<String> suggestions = generateSuggestions();
        sb.append("\n\nFollow-up Suggestions\n\n");
        for (String sug : suggestions) {
            sb.append("- ").append(sug).append("\n");
        }

        sb.append("\n\nArtifact\n\n- [").append(result.artifact().filename())
          .append("](/api/deerflow/artifacts/").append(result.artifact().artifactId()).append("/download)");
        return sb.toString();
    }

    private static List<String> generateSuggestions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Expand the research to other related technologies or industries.");
        suggestions.add("Compare these findings with a historical or geographical case study.");
        return suggestions;
    }
}
