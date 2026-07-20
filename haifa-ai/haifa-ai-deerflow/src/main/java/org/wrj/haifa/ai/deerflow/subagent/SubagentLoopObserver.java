package org.wrj.haifa.ai.deerflow.subagent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.loop.DefaultAgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;

/**
 * Captures a subagent's sources and evidence under its parent run.
 *
 * <p>This is intentionally a top-level type rather than an inner class of {@link SubagentRuntime}.
 * It keeps hot-reload and packaged deployments from depending on a generated nested-class filename.
 */
final class SubagentLoopObserver extends DefaultAgentLoopObserver {

    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final String parentThreadId;
    private final String parentRunId;
    private final List<String> evidenceIds = new ArrayList<>();
    private final List<String> sourceIds = new ArrayList<>();

    SubagentLoopObserver(ResearchRuntimeSupport researchRuntimeSupport,
                         String parentThreadId, String parentRunId, String subagentRunId) {
        super(null);
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.parentThreadId = parentThreadId;
        this.parentRunId = parentRunId;
    }

    @Override
    public String onToolCompleted(AgentRunConfig runConfig, ToolCall toolCall, ToolCallResult toolResult,
                                  List<AgentEvent> events, AtomicInteger seq, List<String> history) {
        if (researchRuntimeSupport == null || toolResult.status() != ToolCallResult.Status.SUCCESS) {
            return super.onToolCompleted(runConfig, toolCall, toolResult, events, seq, history);
        }

        if ("web_search".equals(toolCall.toolName())) {
            var ingestion = researchRuntimeSupport.ingestSearchResults(parentThreadId, parentRunId, toolResult.result());
            for (var registration : ingestion.registrations()) {
                if (!registration.deduplicated()) {
                    sourceIds.add(registration.source().sourceId());
                }
            }
            return ingestion.observation();
        }

        if ("web_fetch".equals(toolCall.toolName())) {
            String url = extractUrl(toolCall.arguments());
            if (!url.isBlank()) {
                var fetchResult = researchRuntimeSupport.ingestFetchedContent(
                        parentThreadId, parentRunId, url, toolResult.result());
                if (!fetchResult.registration().deduplicatedByUrl()
                        && !fetchResult.registration().deduplicatedByContentHash()) {
                    sourceIds.add(fetchResult.registration().stored().source().sourceId());
                }
                for (EvidenceItem evidence : fetchResult.evidenceItems()) {
                    evidenceIds.add(evidence.evidenceId());
                }
                return fetchResult.observation();
            }
        }

        return super.onToolCompleted(runConfig, toolCall, toolResult, events, seq, history);
    }

    List<String> getEvidenceIds() {
        return List.copyOf(evidenceIds);
    }

    List<String> getSourceIds() {
        return List.copyOf(sourceIds);
    }

    private String extractUrl(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }
        String key = "\"url\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return "";
        }
        int colon = json.indexOf(':', keyIndex);
        if (colon < 0) {
            return "";
        }
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return "";
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return "";
        }
        return json.substring(firstQuote + 1, secondQuote).trim();
    }
}
