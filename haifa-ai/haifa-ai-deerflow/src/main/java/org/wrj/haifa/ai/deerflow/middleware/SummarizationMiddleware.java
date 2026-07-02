package org.wrj.haifa.ai.deerflow.middleware;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import reactor.core.publisher.Mono;

/**
 * Summarization middleware inspired by Python deer-flow SummarizationMiddleware.
 * <p>
 * When conversation history exceeds configured thresholds (message count OR character count),
 * older messages are compressed into a summary by a lightweight LLM. The summary is stored
 * as a special SYSTEM message with {@code isSummary=true} and {@code name=summary} metadata.
 * <p>
 * Key features ported from Python deer-flow:
 * <ul>
 *   <li>Dual-condition trigger: messages count OR character length (OR logic)</li>
 *   <li>Keep-last-N policy for recent messages</li>
 *   <li>Lightweight summary model support ({@code summaryModelName})</li>
 *   <li>Skill rescue: detects tool results that read skill files under skillsRoot and preserves them</li>
 *   <li>Dynamic context preservation: prevents system/context reminders from being compressed away</li>
 *   <li>Hidden summary format: summary message is tagged with {@code name=summary} for frontend filtering</li>
 * </ul>
 *
 * <p>Architecture differences from Python:
 * <ul>
 *   <li>No LangGraph AgentState / RemoveMessage; summary is persisted to {@link MessageStore} (SQLite)</li>
 *   <li>No tiktoken; character counts are used as a proxy</li>
 *   <li>Skill rescue is simplified because Java does not have LangChain tool_call_id pairing,
 *       but we detect read_file results under skillsRoot via content heuristics</li>
 * </ul>
 */
@Component
@MiddlewareOrder(8)
public class SummarizationMiddleware implements AgentMiddleware {

    private static final Logger log = LoggerFactory.getLogger(SummarizationMiddleware.class);

    private final MessageStore messageStore;
    private final AgentModelClient modelClient;
    private final DeerFlowProperties properties;

    public SummarizationMiddleware(MessageStore messageStore, AgentModelClient modelClient, DeerFlowProperties properties) {
        this.messageStore = messageStore;
        this.modelClient = modelClient;
        this.properties = properties;
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        DeerFlowProperties.Summarization config = properties.getSummarization();

        String threadId = context.config().threadId();
        String runId = context.config().runId();

        List<MessageRecord> allMessages = messageStore.listByThread(threadId);
        if (allMessages.size() <= 1) {
            return next.next(context);
        }

        // The last message in allMessages is the current user request.
        // We summarize only messages BEFORE the current request.
        List<MessageRecord> historyMessages = allMessages.subList(0, allMessages.size() - 1);

        // Find the latest summary message in the thread
        Optional<MessageRecord> latestSummaryOpt = findLatestSummary(historyMessages);

        List<MessageRecord> activeMessages;
        if (latestSummaryOpt.isPresent()) {
            MessageRecord latestSummary = latestSummaryOpt.get();
            int summaryIndex = historyMessages.indexOf(latestSummary);
            activeMessages = new ArrayList<>();
            activeMessages.add(latestSummary);
            if (summaryIndex >= 0 && summaryIndex < historyMessages.size() - 1) {
                activeMessages.addAll(historyMessages.subList(summaryIndex + 1, historyMessages.size()));
            }
        } else {
            activeMessages = new ArrayList<>(historyMessages);
        }

        // Check if compression threshold is exceeded (dual-condition OR logic)
        int messageCountThreshold = config.getTriggerMessages() > 0 ? config.getTriggerMessages() : 10;
        int charLengthThreshold = config.getTriggerChars() > 0 ? config.getTriggerChars() : 8_000;
        int keepLastN = config.getKeepMessages() > 0 ? config.getKeepMessages() : 4;

        long totalChars = activeMessages.stream().mapToLong(m -> m.content() == null ? 0 : m.content().length()).sum();

        if (config.isEnabled() && (activeMessages.size() > messageCountThreshold || totalChars > charLengthThreshold)) {
            log.info("Thread history exceeds threshold (count={}, chars={}). Compressing...", activeMessages.size(), totalChars);

            // Separate messages to summarize from messages to keep
            int keepIndex = Math.max(0, activeMessages.size() - keepLastN);
            List<MessageRecord> toSummarize = activeMessages.subList(0, keepIndex);
            List<MessageRecord> toKeep = activeMessages.subList(keepIndex, activeMessages.size());

            // Skill rescue: preserve skill-loading tool results (Python _partition_with_skill_rescue)
            PartitionResult partition = partitionWithSkillRescue(toSummarize, toKeep, config);

            // Dynamic context preservation: rescue context-reminder messages
            PartitionResult partition2 = preserveDynamicContextReminders(partition.toSummarize(), partition.toKeep());

            return compressHistory(threadId, runId, partition2.toSummarize(), config)
                    .flatMap(summaryRecord -> {
                        List<MessageRecord> newActive = new ArrayList<>();
                        newActive.add(summaryRecord);
                        newActive.addAll(partition2.toKeep());

                        String historyBlock = buildHistoryBlock(newActive);
                        return next.next(context).map(prompt -> prependHistory(prompt, historyBlock));
                    });
        }

        String historyBlock = buildHistoryBlock(activeMessages);
        return next.next(context).map(prompt -> prependHistory(prompt, historyBlock));
    }

    // ---------------------------------------------------------------------------
    // Summary detection
    // ---------------------------------------------------------------------------

    private Optional<MessageRecord> findLatestSummary(List<MessageRecord> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageRecord msg = messages.get(i);
            if (msg.metadata() != null && Boolean.TRUE.equals(msg.metadata().get("isSummary"))) {
                return Optional.of(msg);
            }
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------------------
    // Compression (mirrors Python _create_summary / _acreate_summary)
    // ---------------------------------------------------------------------------

    private Mono<MessageRecord> compressHistory(String threadId, String runId, List<MessageRecord> messages, DeerFlowProperties.Summarization config) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summarize the following conversation history for a research agent. ")
          .append("Your summary MUST be compact and inform the agent of the previous achievements, findings, and context.\n")
          .append("CRITICAL: You MUST preserve all source IDs (e.g., source-xxxx), URLs, and citation anchors so the agent can still reference them correctly in the future.\n\n")
          .append("History to summarize:\n");
        for (MessageRecord msg : messages) {
            sb.append(msg.role()).append(": ").append(msg.content()).append("\n\n");
        }

        // Use lightweight summary model if configured (Python summary_model)
        String modelName = config.getSummaryModelName();
        ModelPrompt prompt = new ModelPrompt(sb.toString(), "Provide the conversation summary now.", modelName);

        return modelClient.generate(prompt)
                .map(response -> {
                    String summaryText = response != null ? response.content() : "[Empty Summary]";
                    Map<String, Object> metadata = Map.of(
                            "isSummary", true,
                            "name", "summary",  // Python hidden format: HumanMessage(name="summary")
                            "timestamp", Instant.now().toString(),
                            "event", "CONTEXT_COMPRESSED",
                            "summarizedMessageCount", messages.size()
                    );
                    return messageStore.add(threadId, runId, MessageRole.SYSTEM,
                            "Previous conversation summary (including source references):\n" + summaryText,
                            metadata);
                })
                .onErrorResume(ex -> {
                    log.warn("Summary model call failed, creating degraded summary. Error: {}", ex.getMessage());
                    String degradedSummary = createDegradedSummary(messages);
                    Map<String, Object> metadata = Map.of(
                            "isSummary", true,
                            "name", "summary",
                            "timestamp", Instant.now().toString(),
                            "event", "CONTEXT_COMPRESSED_DEGRADED",
                            "error", ex.getMessage()
                    );
                    return Mono.just(messageStore.add(threadId, runId, MessageRole.SYSTEM, degradedSummary, metadata));
                });
    }

    private String createDegradedSummary(List<MessageRecord> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("Previous conversation summary (degraded due to model error):\n");
        for (MessageRecord msg : messages) {
            String content = msg.content() == null ? "" : msg.content();
            if (content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            sb.append("- ").append(msg.role()).append(": ").append(content).append("\n");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------
    // History block assembly
    // ---------------------------------------------------------------------------

    private String buildHistoryBlock(List<MessageRecord> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("<conversation_history>\n");
        for (MessageRecord msg : messages) {
            sb.append(msg.role()).append(": ").append(msg.content()).append("\n\n");
        }
        sb.append("</conversation_history>\n");
        return sb.toString();
    }

    private ModelPrompt prependHistory(ModelPrompt prompt, String historyBlock) {
        String baseUser = prompt.userPrompt();
        String updatedUser = (baseUser == null || baseUser.isBlank())
                ? historyBlock.trim()
                : historyBlock + "\n" + baseUser;
        return new ModelPrompt(prompt.systemPrompt(), updatedUser, prompt.modelName());
    }

    // ---------------------------------------------------------------------------
    // Skill rescue (simplified Python _partition_with_skill_rescue)
    // ---------------------------------------------------------------------------

    private PartitionResult partitionWithSkillRescue(List<MessageRecord> toSummarize, List<MessageRecord> toKeep, DeerFlowProperties.Summarization config) {
        if (toSummarize.isEmpty()) {
            return new PartitionResult(toSummarize, toKeep);
        }

        String skillsRoot = properties.getSkillsRoot();
        if (skillsRoot == null || skillsRoot.isBlank()) {
            return new PartitionResult(toSummarize, toKeep);
        }

        Set<String> skillReadToolNames = Set.of(config.getSkillFileReadToolNames().split(","));
        String normalizedSkillsRoot = skillsRoot.replace("\\", "/");

        List<MessageRecord> rescued = new ArrayList<>();
        List<MessageRecord> remaining = new ArrayList<>();

        for (MessageRecord msg : toSummarize) {
            if (isSkillToolResult(msg, skillReadToolNames, normalizedSkillsRoot)) {
                rescued.add(msg);
            } else {
                remaining.add(msg);
            }
        }

        if (!rescued.isEmpty()) {
            log.info("Skill rescue: preserving {} skill-loading messages from summarization", rescued.size());
        }

        List<MessageRecord> newToKeep = new ArrayList<>(toKeep);
        newToKeep.addAll(0, rescued);
        return new PartitionResult(remaining, newToKeep);
    }

    private boolean isSkillToolResult(MessageRecord msg, Set<String> readToolNames, String skillsRoot) {
        if (msg.role() != MessageRole.TOOL) {
            return false;
        }
        String content = msg.content() == null ? "" : msg.content();
        String metadataTool = msg.metadata() != null ? String.valueOf(msg.metadata().get("tool")) : "";
        // Check if the tool result is from a file-read tool and the content references the skills directory
        boolean isReadTool = readToolNames.contains(metadataTool.trim());
        boolean referencesSkills = content.contains(skillsRoot) || content.contains("SKILL.md");
        return isReadTool && referencesSkills;
    }

    // ---------------------------------------------------------------------------
    // Dynamic context preservation (simplified Python _preserve_dynamic_context_reminders)
    // ---------------------------------------------------------------------------

    private PartitionResult preserveDynamicContextReminders(List<MessageRecord> toSummarize, List<MessageRecord> toKeep) {
        List<MessageRecord> rescued = new ArrayList<>();
        List<MessageRecord> remaining = new ArrayList<>();

        for (MessageRecord msg : toSummarize) {
            if (isDynamicContextReminder(msg)) {
                rescued.add(msg);
            } else {
                remaining.add(msg);
            }
        }

        if (!rescued.isEmpty()) {
            log.info("Dynamic context preservation: rescuing {} reminder messages from summarization", rescued.size());
        }

        List<MessageRecord> newToKeep = new ArrayList<>(toKeep);
        newToKeep.addAll(0, rescued);
        return new PartitionResult(remaining, newToKeep);
    }

    private boolean isDynamicContextReminder(MessageRecord msg) {
        if (msg.metadata() != null && Boolean.TRUE.equals(msg.metadata().get("dynamicContextReminder"))) {
            return true;
        }
        // Heuristic: system/context messages containing current date/time patterns
        String content = msg.content() == null ? "" : msg.content();
        return content.contains("[Dynamic context]") || content.contains("Current date/time:");
    }

    // ---------------------------------------------------------------------------
    // Partition result record
    // ---------------------------------------------------------------------------

    private record PartitionResult(List<MessageRecord> toSummarize, List<MessageRecord> toKeep) {}
}
