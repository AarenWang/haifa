package org.wrj.haifa.ai.deerflow.tool.execution;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolActionExecutionEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.ToolActionExecutionRepository;

/** Durable idempotency boundary for side-effecting tool actions. */
@Service
public class ToolExecutionIdempotencyService {

    private final ToolActionExecutionRepository repository;

    public ToolExecutionIdempotencyService(ToolActionExecutionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public synchronized Reservation reserve(String runId, String toolCallId, String toolName,
                                             String arguments, boolean highRisk) {
        String normalizedName = normalizeName(toolName);
        String argsHash = sha256(normalizeArguments(arguments));
        String key = sha256(runId + "\u0000" + toolCallId + "\u0000" + normalizedName + "\u0000" + argsHash);
        var existing = repository.findById(key);
        if (existing.isEmpty()) {
            ToolActionExecutionEntity entity = new ToolActionExecutionEntity();
            Instant now = Instant.now();
            entity.setIdempotencyKey(key);
            entity.setRunId(runId);
            entity.setToolCallId(toolCallId);
            entity.setNormalizedToolName(normalizedName);
            entity.setArgsHash(argsHash);
            entity.setStatus(ToolExecutionStatus.RESERVED);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            repository.saveAndFlush(entity);
            return new Reservation(key, Decision.EXECUTE, "", "");
        }

        ToolActionExecutionEntity entity = existing.get();
        if (entity.getStatus() == ToolExecutionStatus.SUCCEEDED) {
            return new Reservation(key, Decision.REPLAY_SUCCEEDED, nullToEmpty(entity.getResult()), "");
        }
        if (entity.getStatus() == ToolExecutionStatus.RESERVED
                || entity.getStatus() == ToolExecutionStatus.RUNNING
                || entity.getStatus() == ToolExecutionStatus.UNKNOWN_OUTCOME) {
            entity.setStatus(ToolExecutionStatus.UNKNOWN_OUTCOME);
            entity.setUpdatedAt(Instant.now());
            repository.saveAndFlush(entity);
            if (highRisk) {
                return new Reservation(key, Decision.BLOCK_UNKNOWN_OUTCOME, "",
                        "Previous high-risk tool attempt has an unknown outcome; automatic retry is blocked");
            }
        } else if (highRisk) {
            return new Reservation(key, Decision.BLOCK_RETRY_POLICY, "",
                    "Previous high-risk tool attempt did not succeed; explicit retry approval is required");
        }
        entity.setStatus(ToolExecutionStatus.RESERVED);
        entity.setError(null);
        entity.setUpdatedAt(Instant.now());
        repository.saveAndFlush(entity);
        return new Reservation(key, Decision.EXECUTE, "", "");
    }

    @Transactional
    public void markRunning(String key) { update(key, ToolExecutionStatus.RUNNING, null, null); }
    @Transactional
    public void markSucceeded(String key, String result) { update(key, ToolExecutionStatus.SUCCEEDED, result, null); }
    @Transactional
    public void markFailed(String key, String error) { update(key, ToolExecutionStatus.FAILED, null, error); }
    @Transactional
    public void markCancelled(String key, String error) { update(key, ToolExecutionStatus.CANCELLED, null, error); }
    @Transactional
    public void markUnknownOutcome(String key, String error) {
        update(key, ToolExecutionStatus.UNKNOWN_OUTCOME, null, error);
    }

    private void update(String key, ToolExecutionStatus status, String result, String error) {
        repository.findById(key).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setResult(result);
            entity.setError(error);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        });
    }

    private static String normalizeName(String value) {
        return nullToEmpty(value).trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeArguments(String value) {
        return nullToEmpty(value).trim().replaceAll("\\s+", " ");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }

    public enum Decision { EXECUTE, REPLAY_SUCCEEDED, BLOCK_UNKNOWN_OUTCOME, BLOCK_RETRY_POLICY }
    public record Reservation(String idempotencyKey, Decision decision, String result, String reason) { }
}
