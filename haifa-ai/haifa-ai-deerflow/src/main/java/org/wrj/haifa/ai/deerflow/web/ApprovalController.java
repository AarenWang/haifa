package org.wrj.haifa.ai.deerflow.web;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.wrj.haifa.ai.deerflow.approval.ApprovalDecisionRequest;
import org.wrj.haifa.ai.deerflow.approval.ApprovalDecisionType;
import org.wrj.haifa.ai.deerflow.approval.ApprovalRequestRecord;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStore;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow/approvals")
public class ApprovalController {

    private final ApprovalStore approvalStore;
    private final DeerFlowProperties properties;

    public ApprovalController(ApprovalStore approvalStore, DeerFlowProperties properties) {
        this.approvalStore = approvalStore;
        this.properties = properties;
    }

    @GetMapping("/pending")
    public Mono<ApprovalRequestRecord> getPending(@RequestParam String threadId) {
        return Mono.justOrEmpty(approvalStore.findPendingByThreadId(threadId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending approval for thread")));
    }

    @GetMapping("/run/{runId}")
    public Mono<List<ApprovalRequestRecord>> getByRun(@PathVariable String runId) {
        return Mono.just(approvalStore.findByRunId(runId));
    }

    @GetMapping("/{approvalId}")
    public Mono<ApprovalRequestRecord> get(@PathVariable String approvalId) {
        return Mono.justOrEmpty(approvalStore.find(approvalId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval record not found")));
    }

    @GetMapping("/config")
    public Mono<Map<String, Boolean>> getConfig() {
        boolean allowAlways = properties.getApproval() != null && properties.getApproval().isAllowAlwaysApproval();
        return Mono.just(Map.of("allowAlwaysApproval", allowAlways));
    }

    @PostMapping("/{approvalId}/decision")
    public Mono<ApprovalRequestRecord> decide(
            @PathVariable String approvalId,
            @RequestBody ApprovalDecisionRequest request,
            org.springframework.web.server.ServerWebExchange exchange) {
        if (request == null || request.decision() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision type is required"));
        }
        if (request.decision() == ApprovalDecisionType.APPROVE_ALWAYS) {
            if (properties.getApproval() == null || !properties.getApproval().isAllowAlwaysApproval()) {
                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "APPROVE_ALWAYS decision type is disabled by configuration."));
            }
        }
        String resolvedUser = UserIdResolver.resolve(exchange);
        try {
            return Mono.just(approvalStore.decide(approvalId, request, resolvedUser));
        } catch (IllegalArgumentException ex) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()));
        } catch (IllegalStateException ex) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()));
        }
    }
}
