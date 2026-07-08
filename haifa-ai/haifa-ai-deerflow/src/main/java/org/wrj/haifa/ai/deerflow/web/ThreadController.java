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
import java.util.List;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow/threads")
public class ThreadController {

    private final ThreadManager threadManager;
    private final RunManager runManager;
    private final MessageStore messageStore;
    private final AgentModelClient modelClient;

    public ThreadController(ThreadManager threadManager, RunManager runManager, MessageStore messageStore, AgentModelClient modelClient) {
        this.threadManager = threadManager;
        this.runManager = runManager;
        this.messageStore = messageStore;
        this.modelClient = modelClient;
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

                    // Convert thread messages to ModelMessage format
                    List<ModelMessage> modelMessages = records.stream()
                            .map(r -> new ModelMessage(
                                    ModelMessage.Role.valueOf(r.role().name()),
                                    r.content()
                            ))
                            .toList();

                    String systemPrompt = "You are a helpful assistant. Based on the conversation history, generate exactly 3 relevant, interesting, and diverse follow-up questions that the user might want to ask next. Each question must be short and concise (under 25 words). The language of the generated questions MUST exactly match the language of the user's messages in the conversation history (e.g. if the user asked in Chinese, the questions must be in Chinese). Return the questions as a JSON array of strings, e.g. [\"question 1\", \"question 2\", \"question 3\"]. Respond ONLY with the raw JSON array, without any markdown formatting or code blocks.";
                    String userPrompt = ModelPrompt.renderMessages(modelMessages);

                    return this.modelClient.generate(new ModelPrompt(systemPrompt, userPrompt, null))
                            .map(response -> {
                                String content = response.content().trim();
                                if (content.startsWith("```")) {
                                    content = content.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "");
                                }
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    return mapper.readValue(content, new TypeReference<List<String>>() {});
                                } catch (Exception e) {
                                    return List.of("Tell me more about this", "What are the key points?", "What should we do next?");
                                }
                            })
                            .onErrorReturn(List.of("Tell me more about this", "What are the key points?", "What should we do next?"));
                });
    }

    private static RunResponse toResponse(RunRecord record) {
        return new RunResponse(record.runId(), record.threadId(), record.modelName(), record.status(), record.error(),
                record.mode(), record.createdAt(), record.updatedAt());
    }
}
