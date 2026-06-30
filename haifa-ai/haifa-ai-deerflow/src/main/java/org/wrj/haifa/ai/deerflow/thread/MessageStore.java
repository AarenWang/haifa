package org.wrj.haifa.ai.deerflow.thread;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.persistence.entity.MessageEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.JsonMapper;
import org.wrj.haifa.ai.deerflow.persistence.mapper.MessageMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.MessageRepository;

@Component
public class MessageStore {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final JsonMapper jsonMapper;
    private final AtomicInteger sequenceCounter;

    public MessageStore(MessageRepository messageRepository, MessageMapper messageMapper, JsonMapper jsonMapper) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.jsonMapper = jsonMapper;
        Integer maxSeq = messageRepository.findTopByOrderBySequenceNoDesc()
                .map(MessageEntity::getSequenceNo)
                .orElse(0);
        this.sequenceCounter = new AtomicInteger(maxSeq);
    }

    @Transactional
    public MessageRecord add(String threadId, String runId, MessageRole role, String content, Map<String, Object> metadata) {
        if (!StringUtils.hasText(threadId)) {
            throw new IllegalArgumentException("threadId is required");
        }
        String messageId = UUID.randomUUID().toString();
        MessageEntity entity = new MessageEntity();
        entity.setMessageId(messageId);
        entity.setThreadId(threadId.trim());
        entity.setRunId(runId);
        entity.setRole(role);
        entity.setContent(content == null ? "" : content);
        entity.setMetadataJson(metadataToJson(metadata));
        entity.setCreatedAt(Instant.now());
        entity.setSequenceNo(sequenceCounter.incrementAndGet());
        messageRepository.save(entity);
        return messageMapper.toRecord(entity);
    }

    @Transactional(readOnly = true)
    public List<MessageRecord> listByThread(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return List.of();
        }
        return messageRepository.findByThreadIdOrderBySequenceNoAsc(threadId.trim()).stream()
                .map(messageMapper::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageRecord> listByRun(String runId) {
        if (!StringUtils.hasText(runId)) {
            return List.of();
        }
        return messageRepository.findByRunIdOrderBySequenceNoAsc(runId.trim()).stream()
                .map(messageMapper::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public int count() {
        return (int) messageRepository.count();
    }

    private String metadataToJson(Map<String, Object> metadata) {
        String result = jsonMapper.toJson(metadata);
        return result != null ? result : "{}";
    }
}
