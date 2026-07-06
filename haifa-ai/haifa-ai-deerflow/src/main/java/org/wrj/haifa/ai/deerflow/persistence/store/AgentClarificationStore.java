package org.wrj.haifa.ai.deerflow.persistence.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.persistence.entity.ClarificationEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.ClarificationRepository;

@Component
public class AgentClarificationStore implements ClarificationStore {

    private static final Logger log = LoggerFactory.getLogger(AgentClarificationStore.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<List<ClarificationQuestion>> QUESTION_LIST = new TypeReference<>() {};
    private static final TypeReference<List<ClarificationAnswer>> ANSWER_LIST = new TypeReference<>() {};

    private final ClarificationRepository clarificationRepository;
    private final ObjectMapper objectMapper;

    public AgentClarificationStore(ClarificationRepository clarificationRepository, ObjectMapper objectMapper) {
        this.clarificationRepository = clarificationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ClarificationRecord create(String threadId, String runId, String question, String type, String context,
                                      List<String> options, List<ClarificationQuestion> questions) {
        cancelPendingByThread(threadId);

        Instant now = Instant.now();
        ClarificationEntity entity = new ClarificationEntity();
        entity.setClarificationId(UUID.randomUUID().toString());
        entity.setThreadId(threadId);
        entity.setRunId(runId);
        entity.setQuestion(question);
        entity.setClarificationType(type);
        entity.setContext(context);
        entity.setStatus(ClarificationStatus.PENDING);
        entity.setCreatedAt(now);
        entity.setOptionsJson(toJson(options == null ? List.of() : List.copyOf(options)));
        entity.setQuestionsJson(toJson(questions == null ? List.of() : List.copyOf(questions)));
        entity.setAnswersJson(toJson(List.of()));
        clarificationRepository.save(entity);
        return toRecord(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClarificationRecord> findPending(String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return Optional.empty();
        }
        return clarificationRepository
                .findFirstByThreadIdAndStatusOrderByCreatedAtDesc(threadId, ClarificationStatus.PENDING)
                .map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClarificationRecord> findPendingByRunId(String runId) {
        if (runId == null || runId.isBlank()) {
            return Optional.empty();
        }
        return clarificationRepository
                .findFirstByRunIdAndStatusOrderByCreatedAtDesc(runId, ClarificationStatus.PENDING)
                .map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClarificationRecord> findByRunId(String runId) {
        if (runId == null || runId.isBlank()) {
            return Optional.empty();
        }
        return clarificationRepository.findFirstByRunIdOrderByCreatedAtDesc(runId).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClarificationRecord> find(String clarificationId) {
        if (clarificationId == null || clarificationId.isBlank()) {
            return Optional.empty();
        }
        return clarificationRepository.findByClarificationId(clarificationId).map(this::toRecord);
    }

    @Override
    @Transactional
    public ClarificationRecord answer(String clarificationId, String answer, List<ClarificationAnswer> answers) {
        ClarificationEntity entity = clarificationRepository.findByClarificationId(clarificationId)
                .orElseThrow(() -> new IllegalArgumentException("Clarification not found: " + clarificationId));
        if (entity.getStatus() != ClarificationStatus.PENDING) {
            throw new IllegalStateException("Clarification " + clarificationId + " is not in PENDING status: "
                    + entity.getStatus());
        }
        entity.setStatus(ClarificationStatus.ANSWERED);
        entity.setAnswer(answer);
        entity.setAnsweredAt(Instant.now());
        entity.setAnswersJson(toJson(answers == null ? List.of() : List.copyOf(answers)));
        clarificationRepository.save(entity);
        return toRecord(entity);
    }

    @Override
    @Transactional
    public void cancel(String clarificationId) {
        if (clarificationId == null || clarificationId.isBlank()) {
            return;
        }
        clarificationRepository.findByClarificationId(clarificationId).ifPresent(entity -> {
            if (entity.getStatus() == ClarificationStatus.PENDING) {
                entity.setStatus(ClarificationStatus.CANCELLED);
                entity.setAnsweredAt(Instant.now());
                clarificationRepository.save(entity);
            }
        });
    }

    @Transactional
    public void clearAll() {
        clarificationRepository.deleteAll();
    }

    private void cancelPendingByThread(String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return;
        }
        List<ClarificationEntity> pending = clarificationRepository
                .findByThreadIdAndStatus(threadId, ClarificationStatus.PENDING);
        Instant now = Instant.now();
        for (ClarificationEntity entity : pending) {
            entity.setStatus(ClarificationStatus.CANCELLED);
            entity.setAnsweredAt(now);
            clarificationRepository.save(entity);
        }
    }

    private ClarificationRecord toRecord(ClarificationEntity entity) {
        return new ClarificationRecord(
                entity.getClarificationId(),
                entity.getThreadId(),
                entity.getRunId(),
                entity.getQuestion(),
                entity.getClarificationType(),
                entity.getContext(),
                entity.getStatus(),
                entity.getAnswer(),
                entity.getCreatedAt(),
                entity.getAnsweredAt(),
                fromJson(entity.getOptionsJson(), STRING_LIST),
                fromJson(entity.getQuestionsJson(), QUESTION_LIST),
                fromJson(entity.getAnswersJson(), ANSWER_LIST)
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Failed to serialize clarification JSON: {}", ex.getMessage());
            return "[]";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            if (json == null || json.isBlank()) {
                return objectMapper.readValue("[]", typeReference);
            }
            return objectMapper.readValue(json, typeReference);
        } catch (Exception ex) {
            log.warn("Failed to deserialize clarification JSON: {}", ex.getMessage());
            try {
                return objectMapper.readValue("[]", typeReference);
            } catch (Exception fallbackEx) {
                throw new IllegalStateException("Failed to deserialize empty clarification JSON fallback", fallbackEx);
            }
        }
    }
}
