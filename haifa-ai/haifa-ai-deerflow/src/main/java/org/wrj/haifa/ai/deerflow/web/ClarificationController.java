package org.wrj.haifa.ai.deerflow.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationAnswer;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStore;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow/clarifications")
public class ClarificationController {

    private final ClarificationStore clarificationStore;

    public ClarificationController(ClarificationStore clarificationStore) {
        this.clarificationStore = clarificationStore;
    }

    @GetMapping("/pending")
    public Mono<ClarificationRecord> getPending(@RequestParam String threadId) {
        return Mono.justOrEmpty(clarificationStore.findPending(threadId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending clarification for thread")));
    }

    @PostMapping("/{clarificationId}/answer")
    public Mono<ClarificationRecord> answer(@PathVariable String clarificationId, @RequestBody AnswerRequest request) {
        if (request == null || request.answer() == null || request.answer().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer is required"));
        }
        try {
            return Mono.just(clarificationStore.answer(clarificationId, request.answer(), request.answers()));
        } catch (IllegalArgumentException ex) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()));
        }
    }

    public record AnswerRequest(String answer, java.util.List<ClarificationAnswer> answers) {}
}
