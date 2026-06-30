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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow/threads")
public class ThreadController {

    private final ThreadManager threadManager;
    private final RunManager runManager;
    private final MessageStore messageStore;

    public ThreadController(ThreadManager threadManager, RunManager runManager, MessageStore messageStore) {
        this.threadManager = threadManager;
        this.runManager = runManager;
        this.messageStore = messageStore;
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

    private static RunResponse toResponse(RunRecord record) {
        return new RunResponse(record.runId(), record.threadId(), record.modelName(), record.status(), record.error(),
                record.mode(), record.createdAt(), record.updatedAt());
    }
}
