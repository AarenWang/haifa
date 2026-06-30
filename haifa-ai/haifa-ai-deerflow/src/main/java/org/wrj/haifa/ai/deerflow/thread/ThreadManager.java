package org.wrj.haifa.ai.deerflow.thread;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ThreadManager {

    private static final int TITLE_MAX_LENGTH = 80;

    private final Map<String, ThreadRecord> threads = new ConcurrentHashMap<>();

    public ThreadRecord create(String title, Map<String, Object> metadata) {
        return upsert(UUID.randomUUID().toString(), title, metadata);
    }

    public ThreadRecord upsert(String threadId, String title, Map<String, Object> metadata) {
        String resolvedThreadId = StringUtils.hasText(threadId) ? threadId.trim() : UUID.randomUUID().toString();
        return this.threads.compute(resolvedThreadId, (ignored, existing) -> {
            Instant now = Instant.now();
            if (existing == null) {
                return new ThreadRecord(resolvedThreadId, normalizeTitle(title), ThreadStatus.ACTIVE,
                        metadata == null ? Map.of() : Map.copyOf(metadata), now, now);
            }
            ThreadRecord updated = existing.touched();
            if (StringUtils.hasText(title) && isUntitled(existing.title())) {
                updated = updated.withTitle(normalizeTitle(title));
            }
            if (metadata != null && !metadata.isEmpty()) {
                updated = updated.withMetadata(metadata);
            }
            return updated;
        });
    }

    public Optional<ThreadRecord> find(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.threads.get(threadId.trim()));
    }

    public List<ThreadRecord> list() {
        return this.threads.values().stream()
                .sorted(Comparator.comparing(ThreadRecord::updatedAt).reversed())
                .toList();
    }

    public Optional<ThreadRecord> update(String threadId, String title, ThreadStatus status, Map<String, Object> metadata) {
        if (!StringUtils.hasText(threadId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.threads.computeIfPresent(threadId.trim(), (ignored, existing) -> {
            ThreadRecord updated = existing;
            if (StringUtils.hasText(title)) {
                updated = updated.withTitle(normalizeTitle(title));
            }
            if (status != null) {
                updated = updated.withStatus(status);
            }
            if (metadata != null) {
                updated = updated.withMetadata(metadata);
            }
            return updated.touched();
        }));
    }

    public Optional<ThreadRecord> touch(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.threads.computeIfPresent(threadId.trim(), (ignored, existing) -> existing.touched()));
    }

    public int count() {
        return this.threads.size();
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
}
