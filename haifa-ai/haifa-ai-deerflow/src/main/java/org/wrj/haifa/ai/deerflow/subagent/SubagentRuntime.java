package org.wrj.haifa.ai.deerflow.subagent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.ToolOutputBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;


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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private SubgraphRunner subgraphRunner;

    void setSubgraphRunner(SubgraphRunner subgraphRunner) {
        this.subgraphRunner = subgraphRunner;
    }

    // In-memory tracking of active subagent executions per parent run
    private final ConcurrentHashMap<String, Set<String>> activeSubagentTasks = new ConcurrentHashMap<>();

    // A provider request failure is deterministic for the remainder of one parent run. Remember it
    // so the parent cannot keep dispatching the same invalid subagent request in a tight loop.
    private final ConcurrentHashMap<String, String> providerFailureReasons = new ConcurrentHashMap<>();

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
     * @param parentModelName parent model name to inherit when subagent model is inherit/default
     * @param allowedTools    optional explicit allowed tools (intersected with parent policy)
     * @param modelOverride   optional model override (default inherits parent model)
     * @param timeoutOverride optional timeout override in seconds
     * @param maxTurnsOverride optional max turns override
     * @param parentMode      parent run mode (CHAT or RESEARCH)
     * @param activeSkills    parent active skills (for policy filtering)
     * @return structured subagent result
     */
    public SubagentResult execute(String description, String prompt, String subagentType,
                                   String parentThreadId, String parentRunId, String parentModelName,
                                   List<String> allowedTools, String modelOverride,
                                   Integer timeoutOverride, Integer maxTurnsOverride,
                                   RunMode parentMode, List<org.wrj.haifa.ai.deerflow.skill.Skill> activeSkills) {
        long startTime = System.currentTimeMillis();
        String taskId = "sub-" + UUID.randomUUID().toString().substring(0, 8);

        // Track active subagent for this parent run
        if (!reserveActiveTask(parentRunId, taskId)) {
            return SubagentResult.failed(taskId, parentRunId,
                    "Subagent concurrency limit exceeded: max " + properties.getSubagentMaxConcurrent(), 0);
        }
        log.info("subagent_diagnostic phase=created taskId={} parentRunId={} subagentType={} parentModel={} "
                        + "modelOverride={} activeCount={}",
                taskId, parentRunId, subagentType, safeLogValue(parentModelName), safeLogValue(modelOverride),
                activeCount(parentRunId));

        try {
            // 1. Resolve subagent config
            SubagentConfig config = subagentRegistry.getConfig(subagentType);
            if (config == null) {
                String available = String.join(", ", subagentRegistry.getAvailableNames());
                return SubagentResult.failed(taskId, parentRunId,
                        "Unknown subagent type '" + subagentType + "'. Available: " + available, 0);
            }

            // 2. Resolve effective model
            String effectiveModel = resolveEffectiveModel(modelOverride, parentModelName, config.model());

            // 3. Build filtered tool set
            List<AgentTool> filteredTools = buildFilteredTools(config, allowedTools, parentMode, activeSkills);
            log.info("subagent_diagnostic phase=prepared taskId={} parentRunId={} subagentType={} effectiveModel={} "
                            + "filteredTools={} activeCount={}",
                    taskId, parentRunId, subagentType, safeLogValue(effectiveModel),
                    filteredTools.stream().map(AgentTool::name).toList(), activeCount(parentRunId));
            if (filteredTools.isEmpty()) {
                return SubagentResult.failed(taskId, parentRunId,
                        "No tools available for subagent after policy filtering", 0);
            }

            // 4. Build child prompt and neutral execution hook.
            String subagentSystemPrompt = buildSubagentSystemPrompt(config, parentThreadId, parentRunId, description);
            SubagentExecutionHook hook = new SubagentExecutionHook(
                    researchRuntimeSupport, parentThreadId, parentRunId);
            if (subgraphRunner == null) {
                return SubagentResult.failed(taskId, parentRunId,
                        "SubgraphRunner is not available; child Legacy Loop fallback is disabled", 0);
            }
            int maxTurns = maxTurnsOverride != null ? maxTurnsOverride : config.maxTurns();
            long timeoutMs = (timeoutOverride != null ? timeoutOverride : config.timeoutSeconds()) * 1000L;
            SubagentResult result = subgraphRunner.execute(new SubgraphRunner.ChildRequest(
                    parentRunId, taskId, parentThreadId, effectiveModel, subagentType,
                    subagentSystemPrompt + "\n\n<task>\n" + prompt + "\n</task>",
                    filteredTools.stream().map(AgentTool::name).collect(java.util.stream.Collectors.toSet()),
                    activeSkills, maxTurns, timeoutMs, 1), hook);
            if (!result.isSuccess()) {
                recordProviderFailure(parentRunId, taskId, subagentType, effectiveModel, filteredTools, result.error());
            }
            return result;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Subagent execution failed. taskId={}, parentRunId={}, subagentType={}",
                    taskId, parentRunId, subagentType, ex);
            String error = "Subagent execution error: " + ex.getMessage();
            recordProviderFailure(parentRunId, taskId, subagentType, null, List.of(), error);
            return SubagentResult.failed(taskId, parentRunId, error, duration);
        } finally {
            releaseActiveTask(parentRunId, taskId);
        }
    }

    /**
     * Return the number of currently active subagent tasks for a parent run.
     */
    public int activeCount(String parentRunId) {
        Set<String> active = activeSubagentTasks.get(parentRunId);
        return active == null ? 0 : active.size();
    }

    private boolean reserveActiveTask(String parentRunId, String taskId) {
        Set<String> active = activeSubagentTasks.computeIfAbsent(parentRunId, ignored -> ConcurrentHashMap.newKeySet());
        synchronized (active) {
            if (active.size() >= properties.getSubagentMaxConcurrent()) {
                return false;
            }
            return active.add(taskId);
        }
    }

    /**
     * Return all active subagent task IDs for a parent run.
     */
    public Set<String> activeTaskIds(String parentRunId) {
        Set<String> active = activeSubagentTasks.get(parentRunId);
        return active == null ? Set.of() : Set.copyOf(active);
    }

    /**
     * Whether this parent run has already received a deterministic provider request failure.
     * New {@code task} calls should be rejected rather than retried until the next parent run.
     */
    public boolean hasProviderConfigurationFailure(String parentRunId) {
        return providerFailureReasons.containsKey(parentRunId);
    }

    /**
     * Safe, model-facing explanation for the subagent dispatch circuit breaker.
     */
    public String providerConfigurationFailureReason(String parentRunId) {
        return providerFailureReasons.getOrDefault(parentRunId,
                "A previous subagent provider request failed. Do not retry subagent dispatch in this run.");
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private String resolveEffectiveModel(String modelOverride, String parentModelName, String configModel) {
        if (hasExplicitModel(modelOverride)) {
            return modelOverride.trim();
        }
        if (hasExplicitModel(configModel)) {
            return configModel.trim();
        }
        if (hasExplicitModel(parentModelName)) {
            return parentModelName.trim();
        }
        return properties.getModel();
    }

    private boolean hasExplicitModel(String modelName) {
        return modelName != null && !modelName.isBlank() && !"inherit".equalsIgnoreCase(modelName.trim());
    }

    private void releaseActiveTask(String parentRunId, String taskId) {
        activeSubagentTasks.computeIfPresent(parentRunId, (runId, active) -> {
            active.remove(taskId);
            return active.isEmpty() ? null : active;
        });
        log.info("subagent_diagnostic phase=slot_released taskId={} parentRunId={} activeCount={}",
                taskId, parentRunId, activeCount(parentRunId));
    }

    private void recordProviderFailure(String parentRunId, String taskId, String subagentType,
                                       String effectiveModel, List<AgentTool> filteredTools, String error) {
        if (!isProviderConfigurationFailure(error)) {
            return;
        }
        String reason = "Subagent dispatch circuit is open because an earlier provider request returned "
                + providerStatus(error) + ". Do not call the task tool again in this run; return the provider "
                + "configuration failure to the user.";
        providerFailureReasons.putIfAbsent(parentRunId, reason);
        log.warn("subagent_diagnostic phase=provider_failure taskId={} parentRunId={} subagentType={} effectiveModel={} "
                        + "filteredTools={} providerStatus={} error={}",
                taskId, parentRunId, subagentType, safeLogValue(effectiveModel),
                filteredTools.stream().map(AgentTool::name).toList(), providerStatus(error), safeErrorSummary(error));
    }

    private boolean isProviderConfigurationFailure(String error) {
        if (error == null || error.isBlank()) {
            return false;
        }
        String normalized = error.toLowerCase(Locale.ROOT);
        return normalized.contains("model call failed")
                && (normalized.contains("400 bad request")
                || normalized.contains("401 unauthorized")
                || normalized.contains("403 forbidden")
                || normalized.contains("404 not found"));
    }

    private String providerStatus(String error) {
        String normalized = error == null ? "" : error.toLowerCase(Locale.ROOT);
        for (String status : List.of("400", "401", "403", "404")) {
            if (normalized.contains(status)) {
                return "HTTP " + status;
            }
        }
        return "an HTTP error";
    }

    private String safeErrorSummary(String error) {
        if (error == null) {
            return "";
        }
        String sanitized = error.replaceAll("(?i)(authorization|api[-_ ]?key)\\s*[:=]\\s*[^,\\s]+", "$1=[REDACTED]");
        return sanitized.length() > 500 ? sanitized.substring(0, 500) + "..." : sanitized;
    }

    private String safeLogValue(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

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

}
