package org.wrj.haifa.ai.deerflow.web;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRuntime;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow/runs")
public class RunController {

    private final AgentRuntime agentRuntime;
    private final RunManager runManager;

    public RunController(AgentRuntime agentRuntime, RunManager runManager) {
        this.agentRuntime = agentRuntime;
        this.runManager = runManager;
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentEvent>> stream(@Valid @RequestBody RunCreateRequest request) {
        return this.agentRuntime.stream(new AgentRequest(request.threadId(), request.message(), request.model(), request.uploadedFileIds()))
                .map(event -> ServerSentEvent.<AgentEvent>builder(event)
                        .id(event.runId() + ":" + event.eventId())
                        .event(event.type().name().toLowerCase())
                        .build());
    }

    @GetMapping("/{runId}")
    public Mono<RunResponse> get(@PathVariable String runId) {
        return Mono.justOrEmpty(this.runManager.find(runId))
                .map(RunController::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found")));
    }

    private static RunResponse toResponse(RunRecord record) {
        return new RunResponse(record.runId(), record.threadId(), record.modelName(), record.status(), record.error(),
                record.createdAt(), record.updatedAt());
    }
}
