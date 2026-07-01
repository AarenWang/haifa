package org.wrj.haifa.ai.deerflow.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryCandidateStore;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryFactStore;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class MemoryReflectionService {

    private static final Logger log = LoggerFactory.getLogger(MemoryReflectionService.class);

    private static final Pattern UPLOAD_SENTENCE_RE = Pattern.compile(
            "[^.!?]*\\b(" +
            "upload(ed|ing)?(\\s+\\w+){0,3}\\s+(file|files?|document|documents?|attachment|attachments?)" +
            "|file\\s+upload" +
            "|/mnt/user-data/uploads/" +
            "|<uploaded_files>" +
            "|session-scoped|temporary search" +
            ")[^.!?]*[.!?]?\\s*",
            Pattern.CASE_INSENSITIVE
    );

    private final MessageStore messageStore;
    private final MemoryFactStore factStore;
    private final MemoryCandidateStore candidateStore;
    private final AgentModelClient modelClient;
    private final ObjectMapper objectMapper;
    private final RunManager runManager;

    public MemoryReflectionService(
            MessageStore messageStore,
            MemoryFactStore factStore,
            MemoryCandidateStore candidateStore,
            AgentModelClient modelClient,
            ObjectMapper objectMapper,
            RunManager runManager) {
        this.messageStore = messageStore;
        this.factStore = factStore;
        this.candidateStore = candidateStore;
        this.modelClient = modelClient;
        this.objectMapper = objectMapper;
        this.runManager = runManager;
    }

    public void reflectAsync(String threadId, String runId) {
        Mono.fromRunnable(() -> reflect(threadId, runId))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    public void reflect(String threadId, String runId) {
        try {
            log.info("Starting memory reflection for threadId={}, runId={}", threadId, runId);
            RunRecord run = runManager.find(runId).orElse(null);
            RunMode mode = RunMode.CHAT;
            String modelName = null;
            if (run != null) {
                if ("research".equalsIgnoreCase(run.mode())) {
                    mode = RunMode.RESEARCH;
                }
                modelName = run.modelName();
            }

            String userId = "default-user";
            if (run != null && run.metadata() != null) {
                Object metaUserId = run.metadata().get("userId");
                if (metaUserId instanceof String s && !s.isBlank()) {
                    userId = s;
                }
            }

            List<MessageRecord> messages = messageStore.listByRun(runId);
            if (messages.isEmpty()) {
                log.info("No messages found for runId={}, skipping reflection", runId);
                return;
            }

            final String finalUserId = userId;
            List<MemoryFactRecord> activeFacts = factStore.findByUserIdAndStatus(finalUserId, "active");

            String systemPrompt = buildSystemPrompt(mode);
            String userPrompt = buildUserPrompt(activeFacts, messages);

            ModelPrompt prompt = new ModelPrompt(systemPrompt, userPrompt, modelName);

            modelClient.generate(prompt)
                    .map(response -> response.content())
                    .doOnNext(content -> processReflectionOutput(content, finalUserId, threadId, runId))
                    .doOnError(ex -> log.error("Failed to generate memory reflection for runId={}", runId, ex))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error running memory reflection for runId={}", runId, e);
        }
    }

    private String buildSystemPrompt(RunMode mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI memory extraction agent. Your job is to analyze the conversation messages and extract long-term memory candidates for the user.\n\n");
        sb.append("Category definitions:\n");
        sb.append("- preference: User preferred styles, language, settings.\n");
        sb.append("- correction: User corrections (e.g., 'don't do X, do Y').\n");
        sb.append("- reinforcement: Reinforced guidelines/facts.\n");
        sb.append("- goal: User long-term objectives.\n");
        sb.append("- project_context: User repository/workspace information.\n");
        sb.append("- identity: Information about the user's role or background.\n");
        sb.append("- constraint: Strict constraints user wants you to respect.\n\n");
        sb.append("Extraction Rules:\n");
        sb.append("1. Extract memory facts ONLY if the user explicitly indicates they want to remember them or states a persistent rule/preference.\n");
        sb.append("2. Set confidence high (e.g. >= 0.8) for explicit user statements, and lower (< 0.5) for implicit preferences.\n");
        sb.append("3. Action type must be ADD (new memory), UPDATE (modify existing memory - specify targetFactId), or ARCHIVE (existing memory no longer valid).\n");

        if (mode == RunMode.RESEARCH) {
            sb.append("4. CRITICAL: This was a RESEARCH run. Do NOT extract temporary search facts, sources, or research findings as user memories. ONLY extract if the user explicitly commanded a preference or rule during the conversation.\n");
        } else {
            sb.append("4. Do NOT extract conversation-specific details (like current time or brief questions) as long-term memories.\n");
        }

        sb.append("5. Output MUST be a valid JSON array. Do not write any markdown code block wrapper or text outside the JSON block.\n\n");
        sb.append("Output JSON format:\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"category\": \"preference\" | \"correction\" | \"reinforcement\" | \"goal\" | \"project_context\" | \"identity\" | \"constraint\",\n");
        sb.append("    \"content\": \"Description of the memory fact\",\n");
        sb.append("    \"action\": \"ADD\" | \"UPDATE\" | \"ARCHIVE\",\n");
        sb.append("    \"targetFactId\": \"id of existing fact, or null\",\n");
        sb.append("    \"sourceError\": \"optional description of the prior mistake or wrong approach being corrected, or null\",\n");
        sb.append("    \"confidence\": 0.0 - 1.0\n");
        sb.append("  }\n");
        sb.append("]\n");

        return sb.toString();
    }

    private String buildUserPrompt(List<MemoryFactRecord> activeFacts, List<MessageRecord> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("Existing memories:\n");
        if (activeFacts.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (MemoryFactRecord fact : activeFacts) {
                sb.append("- [id: ").append(fact.id()).append("] [category: ").append(fact.category())
                        .append("] ").append(fact.content()).append("\n");
            }
        }
        sb.append("\nRun Conversation Messages:\n");
        for (MessageRecord msg : messages) {
            sb.append("[").append(msg.role()).append("]: ").append(msg.content()).append("\n");
        }
        sb.append("\nExtract any memory candidate updates now:");
        return sb.toString();
    }

    private void processReflectionOutput(String content, String userId, String threadId, String runId) {
        if (content == null || content.isBlank()) {
            return;
        }
        try {
            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                if (firstNewline != -1) {
                    cleaned = cleaned.substring(firstNewline + 1);
                } else {
                    cleaned = cleaned.substring(3);
                }
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }

            List<CandidateExtraction> extractions = objectMapper.readValue(
                    cleaned,
                    new TypeReference<List<CandidateExtraction>>() {}
            );

            log.info("Extracted {} memory candidate(s) for runId={}", extractions.size(), runId);

            for (CandidateExtraction ext : extractions) {
                if (ext.content == null || ext.content.isBlank()) {
                    continue;
                }
                
                // Filter out transient upload/session/temporary mentions
                String cleanedContent = UPLOAD_SENTENCE_RE.matcher(ext.content).replaceAll("").trim();
                cleanedContent = cleanedContent.replaceAll("  +", " ");
                if (cleanedContent.isBlank()) {
                    log.info("Skipping candidate with transient/upload only content: '{}'", ext.content);
                    continue;
                }

                MemoryCandidateRecord candidate = new MemoryCandidateRecord(
                        null, // Generated by store
                        userId,
                        null,
                        ext.category != null ? ext.category : "preference",
                        cleanedContent,
                        "reflection",
                        threadId,
                        runId,
                        ext.confidence != null ? ext.confidence : 0.5,
                        "pending",
                        ext.action != null ? ext.action.toUpperCase() : "ADD",
                        ext.targetFactId,
                        ext.sourceError,
                        Instant.now(),
                        Instant.now()
                );

                candidateStore.save(candidate);
            }

        } catch (Exception e) {
            log.error("Failed to parse and save reflection candidates. Output content was: {}", content, e);
        }
    }

    public static class CandidateExtraction {
        public String category;
        public String content;
        public String action;
        public String targetFactId;
        public String sourceError;
        public Double confidence;
    }
}
