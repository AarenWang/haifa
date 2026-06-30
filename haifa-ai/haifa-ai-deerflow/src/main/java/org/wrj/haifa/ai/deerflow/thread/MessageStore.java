package org.wrj.haifa.ai.deerflow.thread;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MessageStore {

    private final List<MessageRecord> messages = Collections.synchronizedList(new ArrayList<>());

    public MessageRecord add(String threadId, String runId, MessageRole role, String content, Map<String, Object> metadata) {
        if (!StringUtils.hasText(threadId)) {
            throw new IllegalArgumentException("threadId is required");
        }
        String messageId = UUID.randomUUID().toString();
        MessageRecord record = new MessageRecord(messageId, threadId.trim(), runId, role,
                content == null ? "" : content, metadata == null ? Map.of() : Map.copyOf(metadata), Instant.now());
        this.messages.add(record);
        return record;
    }

    public List<MessageRecord> listByThread(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return List.of();
        }
        synchronized (this.messages) {
            return this.messages.stream()
                    .filter(record -> threadId.trim().equals(record.threadId()))
                    .toList();
        }
    }

    public List<MessageRecord> listByRun(String runId) {
        if (!StringUtils.hasText(runId)) {
            return List.of();
        }
        synchronized (this.messages) {
            return this.messages.stream()
                    .filter(record -> runId.trim().equals(record.runId()))
                    .toList();
        }
    }

    public int count() {
        return this.messages.size();
    }
}
