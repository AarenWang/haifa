package org.wrj.haifa.ai.deerflow.thread;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.persistence.entity.ThreadEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.JsonMapper;
import org.wrj.haifa.ai.deerflow.persistence.mapper.ThreadMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.ThreadRepository;
import org.wrj.haifa.ai.deerflow.thread.ThreadRecord;
import org.wrj.haifa.ai.deerflow.thread.ThreadStatus;

@Component
public class ThreadManager {

    private static final int TITLE_MAX_LENGTH = 80;

    private final ThreadRepository threadRepository;
    private final ThreadMapper threadMapper;
    private final JsonMapper jsonMapper;

    public ThreadManager(ThreadRepository threadRepository, ThreadMapper threadMapper, JsonMapper jsonMapper) {
        this.threadRepository = threadRepository;
        this.threadMapper = threadMapper;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public ThreadRecord create(String title, Map<String, Object> metadata) {
        return upsert(UUID.randomUUID().toString(), title, metadata);
    }

    @Transactional
    public ThreadRecord upsert(String threadId, String title, Map<String, Object> metadata) {
        String resolvedThreadId = StringUtils.hasText(threadId) ? threadId.trim() : UUID.randomUUID().toString();
        Optional<ThreadEntity> existing = threadRepository.findByThreadId(resolvedThreadId);
        Instant now = Instant.now();
        if (existing.isEmpty()) {
            ThreadEntity entity = new ThreadEntity();
            entity.setThreadId(resolvedThreadId);
            entity.setTitle(normalizeTitle(title));
            entity.setStatus(ThreadStatus.ACTIVE);
            entity.setMetadataJson(metadataToJson(metadata));
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            threadRepository.save(entity);
            return threadMapper.toRecord(entity);
        }
        ThreadEntity entity = existing.get();
        entity.setUpdatedAt(now);
        if (StringUtils.hasText(title) && isUntitled(entity.getTitle())) {
            entity.setTitle(normalizeTitle(title));
        }
        if (metadata != null && !metadata.isEmpty()) {
            entity.setMetadataJson(metadataToJson(metadata));
        }
        threadRepository.save(entity);
        return threadMapper.toRecord(entity);
    }

    @Transactional(readOnly = true)
    public Optional<ThreadRecord> find(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return Optional.empty();
        }
        return threadRepository.findByThreadId(threadId.trim())
                .map(threadMapper::toRecord);
    }

    @Transactional(readOnly = true)
    public List<ThreadRecord> list() {
        return threadRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(threadMapper::toRecord)
                .toList();
    }

    @Transactional
    public Optional<ThreadRecord> update(String threadId, String title, ThreadStatus status, Map<String, Object> metadata) {
        if (!StringUtils.hasText(threadId)) {
            return Optional.empty();
        }
        return threadRepository.findByThreadId(threadId.trim()).map(entity -> {
            if (StringUtils.hasText(title)) {
                entity.setTitle(normalizeTitle(title));
            }
            if (status != null) {
                entity.setStatus(status);
            }
            if (metadata != null) {
                entity.setMetadataJson(metadataToJson(metadata));
            }
            entity.setUpdatedAt(Instant.now());
            threadRepository.save(entity);
            return threadMapper.toRecord(entity);
        });
    }

    @Transactional
    public Optional<ThreadRecord> touch(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return Optional.empty();
        }
        return threadRepository.findByThreadId(threadId.trim()).map(entity -> {
            entity.setUpdatedAt(Instant.now());
            threadRepository.save(entity);
            return threadMapper.toRecord(entity);
        });
    }

    @Transactional(readOnly = true)
    public int count() {
        return (int) threadRepository.count();
    }

    public static String titleFromMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "New thread";
        }
        return normalizeTitle(message);
    }

    private static String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "New thread";
        }
        String compact = title.trim().replaceAll("\\s+", " ");
        if (compact.length() <= TITLE_MAX_LENGTH) {
            return compact;
        }
        return compact.substring(0, TITLE_MAX_LENGTH - 1) + "...";
    }

    private static boolean isUntitled(String title) {
        return !StringUtils.hasText(title) || "New thread".equals(title);
    }

    private String metadataToJson(Map<String, Object> metadata) {
        String result = jsonMapper.toJson(metadata);
        return result != null ? result : "{}";
    }
}
