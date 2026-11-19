package org.wrj.haifa.ai.deerflow.subagent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.AgentExecutionHook;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionToolCall;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionToolResult;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;

/** Captures child Graph research facts under the parent run. */
final class SubagentExecutionHook implements AgentExecutionHook {
    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final String parentThreadId;
    private final String parentRunId;
    private final List<String> evidenceIds = new ArrayList<>();
    private final List<String> sourceIds = new ArrayList<>();

    SubagentExecutionHook(ResearchRuntimeSupport support, String parentThreadId, String parentRunId) {
        this.researchRuntimeSupport = support;
        this.parentThreadId = parentThreadId;
        this.parentRunId = parentRunId;
    }

    @Override
    public String afterTool(AgentRunConfig config, ExecutionToolCall call, ExecutionToolResult result,
            List<AgentEvent> events, AtomicInteger sequence, List<String> history) {
        if (researchRuntimeSupport == null || result.status() != ExecutionToolResult.Status.SUCCESS) {
            return null;
        }
        if ("web_search".equals(call.toolName())) {
            var ingestion = researchRuntimeSupport.ingestSearchResults(parentThreadId, parentRunId, result.result());
            ingestion.registrations().stream().filter(item -> !item.deduplicated())
                    .forEach(item -> sourceIds.add(item.source().sourceId()));
            return ingestion.observation();
        }
        if ("web_fetch".equals(call.toolName())) {
            String url = extractUrl(call.arguments());
            if (!url.isBlank()) {
                var fetch = researchRuntimeSupport.ingestFetchedContent(
                        parentThreadId, parentRunId, url, result.result());
                if (!fetch.registration().deduplicatedByUrl()
                        && !fetch.registration().deduplicatedByContentHash()) {
                    sourceIds.add(fetch.registration().stored().source().sourceId());
                }
                for (EvidenceItem evidence : fetch.evidenceItems()) {
                    evidenceIds.add(evidence.evidenceId());
                }
                return fetch.observation();
            }
        }
        return null;
    }

    List<String> evidenceIds() { return List.copyOf(evidenceIds); }
    List<String> sourceIds() { return List.copyOf(sourceIds); }

    private static String extractUrl(String json) {
        if (json == null || json.isBlank()) return "";
        int key = json.indexOf("\"url\"");
        int colon = key < 0 ? -1 : json.indexOf(':', key);
        int first = colon < 0 ? -1 : json.indexOf('"', colon + 1);
        int second = first < 0 ? -1 : json.indexOf('"', first + 1);
        return second < 0 ? "" : json.substring(first + 1, second).trim();
    }
}
