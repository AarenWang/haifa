package org.wrj.haifa.ai.deerflow.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;
import org.wrj.haifa.ai.deerflow.thread.ThreadRecord;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import java.util.List;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow/threads")
public class ThreadController {

    private final ThreadManager threadManager;
    private final RunManager runManager;
    private final MessageStore messageStore;
    private final AgentModelClient modelClient;
    private final DeerFlowProperties properties;

    public ThreadController(ThreadManager threadManager, RunManager runManager, MessageStore messageStore, AgentModelClient modelClient, DeerFlowProperties properties) {
        this.threadManager = threadManager;
        this.runManager = runManager;
        this.messageStore = messageStore;
        this.modelClient = modelClient;
        this.properties = properties;
    }

    @PostMapping
    public Mono<ThreadRecord> create(@RequestBody(required = false) ThreadCreateRequest request) {
        if (request == null) {
            return Mono.just(this.threadManager.create(null, null));
        }
        return Mono.just(this.threadManager.upsert(request.threadId(), request.title(), request.metadata()));
    }

    @GetMapping
    public Mono<ThreadListResponse> list() {
        return Mono.just(new ThreadListResponse(this.threadManager.list()));
    }

    @GetMapping("/{threadId}")
    public Mono<ThreadRecord> get(@PathVariable String threadId) {
        return Mono.justOrEmpty(this.threadManager.find(threadId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found")));
    }

    @PatchMapping("/{threadId}")
    public Mono<ThreadRecord> update(@PathVariable String threadId, @RequestBody ThreadUpdateRequest request) {
        return Mono.justOrEmpty(this.threadManager.update(threadId, request.title(), request.status(), request.metadata()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found")));
    }

    @GetMapping("/{threadId}/runs")
    public Mono<RunListResponse> runs(@PathVariable String threadId) {
        return Mono.justOrEmpty(this.threadManager.find(threadId))
                .map(ignored -> new RunListResponse(this.runManager.listByThread(threadId).stream()
                        .map(ThreadController::toResponse)
                        .toList()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found")));
    }

    @GetMapping("/{threadId}/messages")
    public Mono<MessageListResponse> messages(@PathVariable String threadId) {
        return Mono.justOrEmpty(this.threadManager.find(threadId))
                .map(ignored -> new MessageListResponse(this.messageStore.listByThread(threadId)))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found")));
    }

    @PostMapping("/{threadId}/recommend-questions")
    public Mono<List<String>> recommendQuestions(@PathVariable String threadId) {
        return Mono.justOrEmpty(this.threadManager.find(threadId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found")))
                .flatMap(ignored -> {
                    List<org.wrj.haifa.ai.deerflow.thread.MessageRecord> records = this.messageStore.listByThread(threadId);
                    if (records.isEmpty()) {
                        return Mono.just(List.of());
                    }

                    // Keep only the last 5 messages and truncate each message content to 1000 chars to control context window size
                    int limit = 5;
                    List<org.wrj.haifa.ai.deerflow.thread.MessageRecord> recentRecords = records.size() > limit
                            ? records.subList(records.size() - limit, records.size())
                            : records;

                    List<ModelMessage> modelMessages = recentRecords.stream()
                            .map(r -> {
                                String content = r.content() != null ? r.content() : "";
                                if (content.length() > 1000) {
                                    content = content.substring(0, 1000) + "... [truncated]";
                                }
                                return new ModelMessage(
                                        ModelMessage.Role.valueOf(r.role().name()),
                                        content
                                );
                            })
                            .toList();

                    StringBuilder conversationBuilder = new StringBuilder();
                    for (ModelMessage m : modelMessages) {
                        String role = m.role().name().toLowerCase();
                        String content = m.content() != null ? m.content().trim() : "";
                        if ("user".equals(role)) {
                            conversationBuilder.append("User: ").append(content).append("\n");
                        } else if ("assistant".equals(role)) {
                            conversationBuilder.append("Assistant: ").append(content).append("\n");
                        } else {
                            conversationBuilder.append(m.role().name()).append(": ").append(content).append("\n");
                        }
                    }
                    String conversation = conversationBuilder.toString().trim();

                    String systemPrompt = "You are generating follow-up questions to help the user continue the conversation.\n" +
                            "Based on the conversation below, produce EXACTLY 3 short questions the user might ask next.\n" +
                            "Requirements:\n" +
                            "- Questions must be relevant to the preceding conversation.\n" +
                            "- Questions must be written in the same language as the user.\n" +
                            "- Keep each question concise (ideally <= 20 words / <= 40 Chinese characters).\n" +
                            "- Do NOT include numbering, markdown, or any extra text.\n" +
                            "- Output MUST be a JSON array of strings only.";
                    String userPrompt = "Conversation Context:\n" + conversation + "\n\nGenerate 3 follow-up questions";

                    String modelName = this.properties.getModel();
                    return this.modelClient.generate(new ModelPrompt(systemPrompt, userPrompt, modelName))
                            .map(response -> {
                                String content = response.content() != null ? response.content() : "";
                                List<String> parsed = parseJsonStringList(content);
                                if (parsed == null || parsed.isEmpty()) {
                                    return List.of("Tell me more about this", "What are the key points?", "What should we do next?");
                                }
                                return parsed;
                            })
                            .onErrorReturn(List.of("Tell me more about this", "What are the key points?", "What should we do next?"));
                });
    }

    private static String stripThinkBlocks(String text) {
        if (text == null) return "";
        // Match <think>...</think> block spanning multiple lines
        text = text.replaceAll("(?is)<think\\b[^>]*>.*?</think\\s*>", "");
        // Match dangling, unclosed <think> and discard everything after it
        int openThinkIdx = text.toLowerCase().indexOf("<think");
        if (openThinkIdx >= 0) {
            text = text.substring(0, openThinkIdx);
        }
        return text.trim();
    }

    private static String stripMarkdownCodeFence(String text) {
        if (text == null) return "";
        text = text.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        String[] lines = text.split("\\r?\\n");
        if (lines.length >= 3 && lines[0].startsWith("```") && lines[lines.length - 1].startsWith("```")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < lines.length - 1; i++) {
                sb.append(lines[i]).append("\n");
            }
            return sb.toString().trim();
        }
        return text;
    }

    private static List<String> parseJsonStringList(String text) {
        String candidate = stripThinkBlocks(text);
        candidate = stripMarkdownCodeFence(candidate);
        int start = candidate.indexOf("[");
        int end = candidate.lastIndexOf("]");
        if (start == -1 || end == -1 || end <= start) {
            return null;
        }
        candidate = candidate.substring(start, end + 1);
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> rawList = mapper.readValue(candidate, new TypeReference<List<String>>() {});
            java.util.ArrayList<String> cleaned = new java.util.ArrayList<>();
            for (String s : rawList) {
                if (s != null) {
                    String cleanStr = s.replace("\n", " ").trim();
                    if (!cleanStr.isEmpty()) {
                        cleaned.add(cleanStr);
                    }
                }
            }
            return cleaned;
        } catch (Exception e) {
            return null;
        }
    }

    private static RunResponse toResponse(RunRecord record) {
        return new RunResponse(record.runId(), record.threadId(), record.modelName(), record.status(), record.error(),
                record.mode(), record.createdAt(), record.updatedAt());
    }
}
