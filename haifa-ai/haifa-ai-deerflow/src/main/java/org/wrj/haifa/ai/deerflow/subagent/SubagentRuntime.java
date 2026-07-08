package org.wrj.haifa.ai.deerflow.subagent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.DefaultAgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.agent.loop.NoopAgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.ToolOutputBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;

import reactor.core.publisher.Flux;

/**
 * Runtime for executing subagent tasks.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Inherits parent thread/run context (threadId, model, active skills)</li>
 *   <li>Restricts tool set to allowed tools (intersection with parent permissions)</li>
 *   <li>Prevents recursive subagent nesting (excludes {@code task} from subagent tools)</li>
 *   <li>Registers subagent sources/evidence under parent threadId so they are visible to parent quality gate</li>
 *   <li>Applies same tool output budget middleware as parent</li>
 *   <li>Returns structured {@link SubagentResult} with evidenceIds, sourceIds, tokenUsage</li>
 * </ul>
 */
@Component
public class SubagentRuntime implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(SubagentRuntime.class);

    private final AgentModelClient modelClient;
    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final SubagentRegistry subagentRegistry;
    private final DeerFlowProperties properties;
    private final ToolOutputBudgetMiddleware toolOutputBudgetMiddleware;
    private org.springframework.context.ApplicationContext applicationContext;

    // In-memory tracking of active subagent executions per parent run
    private final ConcurrentHashMap<String, Set<String>> activeSubagentTasks = new ConcurrentHashMap<>();

    public SubagentRuntime(AgentModelClient modelClient,
                           ResearchRuntimeSupport researchRuntimeSupport,
                           SubagentRegistry subagentRegistry,
                           DeerFlowProperties properties,
                           ToolOutputBudgetMiddleware toolOutputBudgetMiddleware) {
        this.modelClient = modelClient;
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.subagentRegistry = subagentRegistry;
        this.properties = properties;
        this.toolOutputBudgetMiddleware = toolOutputBudgetMiddleware;
    }

    private ToolPolicyService getToolPolicyService() {
        return applicationContext.getBean(ToolPolicyService.class);
    }

    @Override
    public void setApplicationContext(org.springframework.context.ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Execute a subagent task.
     *
     * @param description     short description for logging/display
     * @param prompt          detailed prompt for the subagent
     * @param subagentType    type of subagent (e.g. "general-purpose", "bash")
     * @param parentThreadId  parent thread id (inherited)
     * @param parentRunId     parent run id (for tracking)
     * @param allowedTools    optional explicit allowed tools (intersected with parent policy)
     * @param modelOverride   optional model override (default inherits parent model)
     * @param timeoutOverride optional timeout override in seconds
     * @param maxTurnsOverride optional max turns override
     * @param parentMode      parent run mode (CHAT or RESEARCH)
     * @param activeSkills    parent active skills (for policy filtering)
     * @return structured subagent result
     */
    public SubagentResult execute(String description, String prompt, String subagentType,
                                   String parentThreadId, String parentRunId,
                                   List<String> allowedTools, String modelOverride,
                                   Integer timeoutOverride, Integer maxTurnsOverride,
                                   RunMode parentMode, List<org.wrj.haifa.ai.deerflow.skill.Skill> activeSkills) {
        long startTime = System.currentTimeMillis();
        String taskId = "sub-" + UUID.randomUUID().toString().substring(0, 8);

        // Track active subagent for this parent run
        activeSubagentTasks.computeIfAbsent(parentRunId, k -> ConcurrentHashMap.newKeySet()).add(taskId);

        try {
            // 1. Resolve subagent config
            SubagentConfig config = subagentRegistry.getConfig(subagentType);
            if (config == null) {
                String available = String.join(", ", subagentRegistry.getAvailableNames());
                return SubagentResult.failed(taskId, parentRunId,
                        "Unknown subagent type '" + subagentType + "'. Available: " + available, 0);
            }

            // 2. Resolve effective model
            String effectiveModel = modelOverride != null && !modelOverride.isBlank()
                    ? modelOverride
                    : ("inherit".equals(config.model()) ? properties.getModel() : config.model());

            // 3. Build filtered tool set
            List<AgentTool> filteredTools = buildFilteredTools(config, allowedTools, parentMode, activeSkills);
            if (filteredTools.isEmpty()) {
                return SubagentResult.failed(taskId, parentRunId,
                        "No tools available for subagent after policy filtering", 0);
            }

            // 4. Build subagent run config
            String subagentRunId = taskId;
            AgentRunConfig subagentRunConfig = new AgentRunConfig(
                    parentThreadId,
                    subagentRunId,
                    effectiveModel,
                    false, // thinking enabled
                    false, // plan mode
                    maxTurnsOverride != null ? maxTurnsOverride : config.maxTurns(),
                    Path.of(properties.getWorkspaceRoot()),
                    parentMode,
                    org.wrj.haifa.ai.deerflow.agent.ResearchOptions.defaults(),
                    Map.of("parentRunId", parentRunId, "subagentType", subagentType,
                            "description", description, "taskId", taskId)
            );

            // 5. Build system prompt for subagent
            String subagentSystemPrompt = buildSubagentSystemPrompt(config, parentThreadId, parentRunId, description);

            // 6. Build loop config
            LoopConfig loopConfig = new LoopConfig(
                    maxTurnsOverride != null ? maxTurnsOverride : config.maxTurns(),
                    Math.max(5, (maxTurnsOverride != null ? maxTurnsOverride : config.maxTurns()) * 2),
                    (timeoutOverride != null ? timeoutOverride : config.timeoutSeconds()) * 1000L,
                    org.wrj.haifa.ai.deerflow.agent.ResearchOptions.defaults()
            );

            // 7. Create a filtered tool registry for this subagent
            ToolRegistry subagentToolRegistry = new FilteredToolRegistry(filteredTools);

            // 8. Create observer that captures sources/evidence and registers them under the parent run.
            SubagentLoopObserver subagentObserver = new SubagentLoopObserver(
                    researchRuntimeSupport, parentThreadId, parentRunId, subagentRunId);

            // 9. Build and run the subagent loop
            AgentLoop subagentLoop = new AgentLoop(
                    modelClient,
                    subagentToolRegistry,
                    null, // modelStepStore - not persisted for subagent
                    null, // toolCallStore - not persisted for subagent
                    null, // agentLoopRunStore - not persisted for subagent
                    subagentObserver,
                    toolOutputBudgetMiddleware
            );

            AtomicInteger seq = new AtomicInteger();
            Flux<AgentEvent> eventFlux = subagentLoop.run(
                    loopConfig,
                    subagentRunConfig,
                    subagentSystemPrompt,
                    prompt,
                    seq,
                    getToolPolicyService(),
                    activeSkills,
                    List.of()
            );

            // Block to collect all events and extract result
            List<AgentEvent> events = eventFlux.collectList().block();
            long duration = System.currentTimeMillis() - startTime;

            // 10. Extract final answer and collect sources/evidence from observer
            String finalAnswer = extractFinalAnswer(events);
            List<String> evidenceIds = subagentObserver.getEvidenceIds();
            List<String> sourceIds = subagentObserver.getSourceIds();
            Map<String, Integer> tokenUsage = estimateTokenUsage(events);

            // 11. Remove from active tracking
            Set<String> active = activeSubagentTasks.get(parentRunId);
            if (active != null) {
                active.remove(taskId);
            }

            // 12. Check if run failed
            if (events != null) {
                boolean runFailed = events.stream().anyMatch(e -> e.type() == AgentEventType.RUN_FAILED);
                if (runFailed) {
                    String error = events.stream()
                            .filter(e -> e.type() == AgentEventType.RUN_FAILED)
                            .map(AgentEvent::content)
                            .findFirst().orElse("Subagent run failed");
                    return SubagentResult.failed(taskId, parentRunId, error, duration);
                }
            }

            return SubagentResult.success(taskId, parentRunId, finalAnswer, evidenceIds, sourceIds, tokenUsage, duration);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Subagent execution failed. taskId={}, parentRunId={}, subagentType={}",
                    taskId, parentRunId, subagentType, ex);
            Set<String> active = activeSubagentTasks.get(parentRunId);
            if (active != null) {
                active.remove(taskId);
            }
            return SubagentResult.failed(taskId, parentRunId, "Subagent execution error: " + ex.getMessage(), duration);
        }
    }

    /**
     * Return the number of currently active subagent tasks for a parent run.
     */
    public int activeCount(String parentRunId) {
        Set<String> active = activeSubagentTasks.get(parentRunId);
        return active == null ? 0 : active.size();
    }

    /**
     * Return all active subagent task IDs for a parent run.
     */
    public Set<String> activeTaskIds(String parentRunId) {
        Set<String> active = activeSubagentTasks.get(parentRunId);
        return active == null ? Set.of() : Set.copyOf(active);
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private List<AgentTool> buildFilteredTools(SubagentConfig config, List<String> allowedTools,
                                                RunMode parentMode, List<org.wrj.haifa.ai.deerflow.skill.Skill> activeSkills) {
        Set<String> configAllowed = config.allowedTools() != null && !config.allowedTools().isEmpty()
                ? Set.copyOf(config.allowedTools())
                : null;
        Set<String> configDisallowed = Set.copyOf(config.disallowedTools());
        Set<String> explicitAllowed = allowedTools != null && !allowedTools.isEmpty()
                ? Set.copyOf(allowedTools)
                : null;

        List<AgentTool> result = new ArrayList<>();
        ToolRegistry toolRegistry = applicationContext.getBean(ToolRegistry.class);
        for (AgentTool tool : toolRegistry.tools()) {
            String toolName = tool.name();

            // Always exclude task to prevent recursive nesting
            if ("task".equals(toolName) || configDisallowed.contains(toolName)) {
                continue;
            }

            // Apply policy check (parent's policy)
            ToolPolicyService toolPolicy = getToolPolicyService();
            if (toolPolicy != null && !toolPolicy.evaluateTool(toolName, activeSkills, parentMode).allowed()) {
                continue;
            }

            // Apply config allowlist
            if (configAllowed != null && !configAllowed.contains(toolName)) {
                continue;
            }

            // Apply explicit allowed tools intersection
            if (explicitAllowed != null && !explicitAllowed.contains(toolName)) {
                continue;
            }

            result.add(tool);
        }
        return result;
    }

    private String buildSubagentSystemPrompt(SubagentConfig config, String parentThreadId, String parentRunId, String description) {
        StringBuilder sb = new StringBuilder();
        if (config.systemPrompt() != null && !config.systemPrompt().isBlank()) {
            sb.append(config.systemPrompt()).append("\n\n");
        }
        sb.append("You are a subagent executing a focused task.\n");
        sb.append("Task description: ").append(description).append("\n");
        sb.append("Parent thread: ").append(parentThreadId).append("\n");
        sb.append("CRITICAL: You do NOT have access to the `task` tool. Do not attempt to delegate further.\n");
        sb.append("Return a structured summary of your findings with all sources and evidence clearly listed.\n");
        
        sb.append(buildThreadMemorySnapshot(parentThreadId));
        
        return sb.toString();
    }

    private String buildThreadMemorySnapshot(String threadId) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n<thread_research_memory>\n");

        try {
            var planStore = applicationContext.getBean(org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore.class);
            var plans = planStore.findByThreadId(threadId);
            if (plans != null && !plans.isEmpty()) {
                var plan = plans.get(plans.size() - 1);
                sb.append("Research Topic: ").append(plan.topic()).append("\n");
                sb.append("Plan Dimensions:\n");
                for (var dim : plan.dimensions()) {
                    sb.append("  * [").append(dim.status().name()).append("] ").append(dim.title()).append("\n");
                }
            }
        } catch (Exception e) {
            // ignore
        }

        try {
            var sourceRegistry = applicationContext.getBean(org.wrj.haifa.ai.deerflow.research.SourceRegistry.class);
            var sources = sourceRegistry.listByThread(threadId);
            if (!sources.isEmpty()) {
                sb.append("Registered Sources (do not fetch again):\n");
                int limit = 5;
                for (int i = 0; i < Math.min(sources.size(), limit); i++) {
                    var src = sources.get(i);
                    sb.append("  * [").append(src.sourceId()).append("] ").append(src.title()).append(" (").append(src.url()).append(")\n");
                }
                if (sources.size() > limit) {
                    sb.append("  * ... and ").append(sources.size() - limit).append(" more sources.\n");
                }
            }
        } catch (Exception e) {
            // ignore
        }

        try {
            var evidenceStore = applicationContext.getBean(org.wrj.haifa.ai.deerflow.research.EvidenceStore.class);
            var evidence = evidenceStore.listByThread(threadId);
            if (!evidence.isEmpty()) {
                sb.append("Evidence Store Summary:\n");
                int limit = 5;
                for (int i = 0; i < Math.min(evidence.size(), limit); i++) {
                    var item = evidence.get(i);
                    sb.append("  * [").append(item.evidenceId()).append("] (Source: ").append(item.sourceId()).append("): ").append(item.claim()).append("\n");
                }
                if (evidence.size() > limit) {
                    sb.append("  * ... and ").append(evidence.size() - limit).append(" more evidence.\n");
                }
            }
        } catch (Exception e) {
            // ignore
        }

        sb.append("</thread_research_memory>\n");
        return sb.toString();
    }

    private String extractFinalAnswer(List<AgentEvent> events) {
        if (events == null) {
            return "";
        }
        // Find the last MODEL_COMPLETED event with content
        for (int i = events.size() - 1; i >= 0; i--) {
            AgentEvent evt = events.get(i);
            if (evt.type() == AgentEventType.MODEL_COMPLETED) {
                if (evt.content() != null && !evt.content().isBlank()) {
                    return evt.content();
                }
            }
        }
        // Check if there is a final MODEL_DELTA event containing full accumulated response
        for (int i = events.size() - 1; i >= 0; i--) {
            AgentEvent evt = events.get(i);
            if (evt.type() == AgentEventType.MODEL_DELTA && evt.metadata() != null && evt.metadata().containsKey("modelDurationMs")) {
                if (evt.content() != null && !evt.content().isBlank()) {
                    return evt.content();
                }
            }
        }
        // Fallback: concatenate all streaming MODEL_DELTA events (which do not contain modelDurationMs)
        String joined = events.stream()
                .filter(e -> e.type() == AgentEventType.MODEL_DELTA && (e.metadata() == null || !e.metadata().containsKey("modelDurationMs")))
                .map(AgentEvent::content)
                .collect(Collectors.joining(""));
        if (!joined.isBlank()) {
            return joined;
        }
        // Last fallback: concatenate all MODEL_DELTA events
        return events.stream()
                .filter(e -> e.type() == AgentEventType.MODEL_DELTA)
                .map(AgentEvent::content)
                .collect(Collectors.joining("\n"));
    }

    private Map<String, Integer> estimateTokenUsage(List<AgentEvent> events) {
        if (events == null) {
            return Map.of();
        }
        int totalChars = events.stream()
                .mapToInt(e -> e.content() == null ? 0 : e.content().length())
                .sum();
        // Rough heuristic: ~4 chars per token
        int estimatedTokens = totalChars / 4;
        return Map.of("estimated_total_tokens", estimatedTokens);
    }

    // ---------------------------------------------------------------------------
    // FilteredToolRegistry - wraps a subset of tools for subagent use
    // ---------------------------------------------------------------------------

    static class FilteredToolRegistry extends ToolRegistry {
        private final List<AgentTool> filteredTools;

        FilteredToolRegistry(List<AgentTool> filteredTools) {
            super(List.of()); // empty base - we override tools()
            this.filteredTools = List.copyOf(filteredTools);
        }

        @Override
        public List<AgentTool> tools() {
            return filteredTools;
        }

        @Override
        public List<AgentTool> plan(String userMessage, int maxTools) {
            return filteredTools.stream()
                    .filter(tool -> tool.supports(userMessage))
                    .limit(Math.max(0, maxTools))
                    .toList();
        }
    }

    // ---------------------------------------------------------------------------
    // SubagentLoopObserver - captures sources/evidence for parent thread
    // ---------------------------------------------------------------------------

    static class SubagentLoopObserver extends DefaultAgentLoopObserver {

        private final ResearchRuntimeSupport researchRuntimeSupport;
        private final String parentThreadId;
        private final String parentRunId;
        private final String subagentRunId;
        private final List<String> evidenceIds = new ArrayList<>();
        private final List<String> sourceIds = new ArrayList<>();

        SubagentLoopObserver(ResearchRuntimeSupport researchRuntimeSupport,
                             String parentThreadId, String parentRunId, String subagentRunId) {
            super(null);
            this.researchRuntimeSupport = researchRuntimeSupport;
            this.parentThreadId = parentThreadId;
            this.parentRunId = parentRunId;
            this.subagentRunId = subagentRunId;
        }

        @Override
        public String onToolCompleted(AgentRunConfig runConfig, ToolCall toolCall, ToolCallResult toolResult,
                                      List<AgentEvent> events, AtomicInteger seq, List<String> history) {
            if (researchRuntimeSupport == null || toolResult.status() != ToolCallResult.Status.SUCCESS) {
                return super.onToolCompleted(runConfig, toolCall, toolResult, events, seq, history);
            }

            // Register search results under parent run so quality gate/report delivery can read them.
            if ("web_search".equals(toolCall.toolName())) {
                var ingestion = researchRuntimeSupport.ingestSearchResults(parentThreadId, parentRunId, toolResult.result());
                for (var reg : ingestion.registrations()) {
                    if (!reg.deduplicated()) {
                        sourceIds.add(reg.source().sourceId());
                    }
                }
                return ingestion.observation();
            }

            // Register fetched content under parent run so quality gate/report delivery can read it.
            if ("web_fetch".equals(toolCall.toolName())) {
                String url = extractUrl(toolCall.arguments());
                if (!url.isBlank()) {
                    var fetchResult = researchRuntimeSupport.ingestFetchedContent(parentThreadId, parentRunId, url, toolResult.result());
                    if (!fetchResult.registration().deduplicatedByUrl() && !fetchResult.registration().deduplicatedByContentHash()) {
                        sourceIds.add(fetchResult.registration().stored().source().sourceId());
                    }
                    for (EvidenceItem ev : fetchResult.evidenceItems()) {
                        evidenceIds.add(ev.evidenceId());
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
            int keyIdx = json.indexOf(key);
            if (keyIdx < 0) {
                return "";
            }
            int colon = json.indexOf(':', keyIdx);
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
}
