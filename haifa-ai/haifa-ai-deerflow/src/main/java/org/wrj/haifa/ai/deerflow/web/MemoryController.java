package org.wrj.haifa.ai.deerflow.web;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.wrj.haifa.ai.deerflow.memory.AgentPersonaRecord;
import org.wrj.haifa.ai.deerflow.memory.MemoryCandidateRecord;
import org.wrj.haifa.ai.deerflow.memory.MemoryFactRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentPersonaStore;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryCandidateStore;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryFactStore;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow")
public class MemoryController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MemoryController.class);

    private final AgentPersonaStore personaStore;
    private final MemoryFactStore factStore;
    private final MemoryCandidateStore candidateStore;

    public MemoryController(
            AgentPersonaStore personaStore,
            MemoryFactStore factStore,
            MemoryCandidateStore candidateStore) {
        this.personaStore = personaStore;
        this.factStore = factStore;
        this.candidateStore = candidateStore;
    }

    // --- PERSONA ENDPOINTS ---

    @GetMapping("/persona")
    public Mono<List<AgentPersonaRecord>> getPersonas(ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        return Mono.just(personaStore.findByUserId(userId));
    }

    @PutMapping("/persona")
    public Mono<AgentPersonaRecord> savePersona(@RequestBody AgentPersonaRecord request, ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        
        if (request.name() == null || request.name().trim().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Persona name cannot be empty"));
        }
        if (request.soul() == null || request.soul().trim().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Persona soul rules cannot be empty"));
        }
        if (request.soul().length() > 10000) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Persona soul rules content too long (max 10000 characters)"));
        }

        if (request.enabled()) {
            List<AgentPersonaRecord> existingPersonas = personaStore.findByUserId(userId);
            boolean hasOtherEnabled = existingPersonas.stream()
                    .anyMatch(p -> p.enabled() && !p.id().equals(request.id()) &&
                                   (p.agentId() == null ? request.agentId() == null : p.agentId().equals(request.agentId())));
            if (hasOtherEnabled) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only one enabled Persona is allowed per context"));
            }
        }

        // If updating existing, verify ownership
        if (request.id() != null && !request.id().isBlank()) {
            var existingOpt = personaStore.findById(request.id());
            if (existingOpt.isPresent() && !existingOpt.get().userId().equals(userId)) {
                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
            }
        }

        AgentPersonaRecord toSave = new AgentPersonaRecord(
                request.id(),
                userId,
                request.agentId(),
                request.name(),
                request.description(),
                request.soul(),
                request.enabled(),
                request.createdAt(),
                request.updatedAt()
        );
        return Mono.just(personaStore.save(toSave));
    }

    // --- FACT ENDPOINTS ---

    @GetMapping("/memory/facts")
    public Mono<List<MemoryFactRecord>> getMemoryFacts(@RequestParam(required = false) String status, ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        if (status != null && !status.isBlank()) {
            return Mono.just(factStore.findByUserIdAndStatus(userId, status));
        }
        return Mono.just(factStore.findByUserId(userId));
    }

    @PostMapping("/memory/facts")
    public Mono<MemoryFactRecord> createMemoryFact(@RequestBody MemoryFactRecord request, ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        MemoryFactRecord toSave = new MemoryFactRecord(
                null,
                userId,
                request.agentId(),
                request.category() != null ? request.category() : "preference",
                request.content(),
                request.source() != null ? request.source() : "manual",
                request.sourceThreadId(),
                request.sourceRunId(),
                request.confidence() != null ? request.confidence() : 1.0,
                "active",
                request.sourceError(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        return Mono.just(factStore.save(toSave));
    }

    @PatchMapping("/memory/facts/{id}")
    public Mono<MemoryFactRecord> updateMemoryFact(@PathVariable String id, @RequestBody MemoryFactRecord request, ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        return Mono.justOrEmpty(factStore.findById(id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Fact not found")))
                .flatMap(existing -> {
                    if (!existing.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    MemoryFactRecord updated = new MemoryFactRecord(
                            existing.id(),
                            existing.userId(),
                            request.agentId() != null ? request.agentId() : existing.agentId(),
                            request.category() != null ? request.category() : existing.category(),
                            request.content() != null ? request.content() : existing.content(),
                            request.source() != null ? request.source() : existing.source(),
                            request.sourceThreadId() != null ? request.sourceThreadId() : existing.sourceThreadId(),
                            request.sourceRunId() != null ? request.sourceRunId() : existing.sourceRunId(),
                            request.confidence() != null ? request.confidence() : existing.confidence(),
                            request.status() != null ? request.status() : existing.status(),
                            request.sourceError() != null ? request.sourceError() : existing.sourceError(),
                            existing.createdAt(),
                            Instant.now(),
                            request.lastUsedAt() != null ? request.lastUsedAt() : existing.lastUsedAt()
                    );
                    return Mono.just(factStore.save(updated));
                });
    }

    @DeleteMapping("/memory/facts/{id}")
    public Mono<Void> deleteMemoryFact(@PathVariable String id, ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        return Mono.justOrEmpty(factStore.findById(id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Fact not found")))
                .flatMap(existing -> {
                    if (!existing.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    MemoryFactRecord archived = new MemoryFactRecord(
                            existing.id(),
                            existing.userId(),
                            existing.agentId(),
                            existing.category(),
                            existing.content(),
                            existing.source(),
                            existing.sourceThreadId(),
                            existing.sourceRunId(),
                            existing.confidence(),
                            "archived",
                            existing.sourceError(),
                            existing.createdAt(),
                            Instant.now(),
                            existing.lastUsedAt()
                    );
                    factStore.save(archived);
                    return Mono.empty();
                });
    }

    // --- CANDIDATE ENDPOINTS ---

    @GetMapping("/memory/candidates")
    public Mono<List<MemoryCandidateRecord>> getCandidates(@RequestParam(required = false, defaultValue = "pending") String status, ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        return Mono.just(candidateStore.findByUserIdAndStatus(userId, status));
    }

    @PostMapping("/memory/candidates/{id}/approve")
    public Mono<MemoryFactRecord> approveCandidate(@PathVariable String id, ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        return Mono.justOrEmpty(candidateStore.findById(id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found")))
                .flatMap(candidate -> {
                    if (!candidate.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    
                    // Update candidate status to approved
                    MemoryCandidateRecord approvedCandidate = new MemoryCandidateRecord(
                            candidate.id(),
                            candidate.userId(),
                            candidate.agentId(),
                            candidate.category(),
                            candidate.content(),
                            candidate.source(),
                            candidate.sourceThreadId(),
                            candidate.sourceRunId(),
                            candidate.confidence(),
                            "approved",
                            candidate.action(),
                            candidate.targetFactId(),
                            candidate.sourceError(),
                            candidate.createdAt(),
                            Instant.now()
                    );
                    candidateStore.save(approvedCandidate);

                    String action = candidate.action() != null ? candidate.action().toUpperCase() : "ADD";
                    if ("ARCHIVE".equals(action)) {
                        if (candidate.targetFactId() == null || candidate.targetFactId().isBlank()) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing targetFactId for ARCHIVE action"));
                        }
                        return Mono.justOrEmpty(factStore.findById(candidate.targetFactId()))
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Target memory fact not found")))
                                .flatMap(existing -> {
                                    if (!existing.userId().equals(userId)) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                                    }
                                    MemoryFactRecord archived = new MemoryFactRecord(
                                            existing.id(),
                                            existing.userId(),
                                            existing.agentId(),
                                            existing.category(),
                                            existing.content(),
                                            existing.source(),
                                            existing.sourceThreadId(),
                                            existing.sourceRunId(),
                                            existing.confidence(),
                                            "archived",
                                            existing.sourceError(),
                                            existing.createdAt(),
                                            Instant.now(),
                                            existing.lastUsedAt()
                                    );
                                    return Mono.just(factStore.save(archived));
                                });
                    } else if ("UPDATE".equals(action)) {
                        if (candidate.targetFactId() == null || candidate.targetFactId().isBlank()) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing targetFactId for UPDATE action"));
                        }
                        return Mono.justOrEmpty(factStore.findById(candidate.targetFactId()))
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Target memory fact not found")))
                                .flatMap(existing -> {
                                    if (!existing.userId().equals(userId)) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                                    }
                                    MemoryFactRecord updated = new MemoryFactRecord(
                                            existing.id(),
                                            existing.userId(),
                                            existing.agentId(),
                                            candidate.category(),
                                            candidate.content(),
                                            existing.source(),
                                            candidate.sourceThreadId(),
                                            candidate.sourceRunId(),
                                            candidate.confidence(),
                                            "active",
                                            candidate.sourceError() != null ? candidate.sourceError() : existing.sourceError(),
                                            existing.createdAt(),
                                            Instant.now(),
                                            existing.lastUsedAt()
                                    );
                                    return Mono.just(factStore.save(updated));
                                });
                    } else {
                        // ADD
                        // Deduplication: check if active fact with same content already exists
                        List<MemoryFactRecord> activeFacts = factStore.findByUserIdAndStatus(userId, "active");
                        String normalizedCandidateContent = candidate.content().trim().toLowerCase();
                        MemoryFactRecord existingMatch = activeFacts.stream()
                                .filter(fact -> fact.content() != null && fact.content().trim().toLowerCase().equals(normalizedCandidateContent))
                                .findFirst()
                                .orElse(null);
                        if (existingMatch != null) {
                            log.info("Deduplicated candidate: active fact with same content already exists (factId={})", existingMatch.id());
                            return Mono.just(existingMatch);
                        }

                        MemoryFactRecord newFact = new MemoryFactRecord(
                                null,
                                candidate.userId(),
                                candidate.agentId(),
                                candidate.category(),
                                candidate.content(),
                                candidate.source(),
                                candidate.sourceThreadId(),
                                candidate.sourceRunId(),
                                candidate.confidence(),
                                "active",
                                candidate.sourceError(),
                                Instant.now(),
                                Instant.now(),
                                Instant.now()
                        );
                        return Mono.just(factStore.save(newFact));
                    }
                });
    }

    @PostMapping("/memory/candidates/{id}/reject")
    public Mono<MemoryCandidateRecord> rejectCandidate(@PathVariable String id, ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        return Mono.justOrEmpty(candidateStore.findById(id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found")))
                .flatMap(candidate -> {
                    if (!candidate.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    MemoryCandidateRecord rejectedCandidate = new MemoryCandidateRecord(
                            candidate.id(),
                            candidate.userId(),
                            candidate.agentId(),
                            candidate.category(),
                            candidate.content(),
                            candidate.source(),
                            candidate.sourceThreadId(),
                            candidate.sourceRunId(),
                            candidate.confidence(),
                            "rejected",
                            candidate.action(),
                            candidate.targetFactId(),
                            candidate.sourceError(),
                            candidate.createdAt(),
                            Instant.now()
                    );
                    return Mono.just(candidateStore.save(rejectedCandidate));
                });
    }
}
