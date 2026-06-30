package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.persistence.entity.UploadEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.UploadMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.UploadRepository;
import org.wrj.haifa.ai.deerflow.upload.UploadRecord;

@Component
public class UploadStore {

    private final UploadRepository uploadRepository;
    private final UploadMapper uploadMapper;

    public UploadStore(UploadRepository uploadRepository, UploadMapper uploadMapper) {
        this.uploadRepository = uploadRepository;
        this.uploadMapper = uploadMapper;
    }

    @Transactional
    public UploadRecord save(UploadRecord record) {
        UploadEntity entity = uploadRepository.findByFileId(record.getFileId())
                .orElseGet(UploadEntity::new);
        uploadMapper.updateEntity(entity, record);
        Instant now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt() : now);
        }
        entity.setUpdatedAt(now);
        uploadRepository.save(entity);
        return uploadMapper.toRecord(entity);
    }

    @Transactional(readOnly = true)
    public UploadRecord find(String fileId) {
        return uploadRepository.findByFileId(fileId)
                .map(uploadMapper::toRecord)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public UploadRecord findByFileIdAndThreadId(String fileId, String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return null;
        }
        return uploadRepository.findByFileIdAndThreadId(fileId, threadId)
                .map(uploadMapper::toRecord)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UploadRecord> list(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return List.of();
        }
        return uploadRepository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
                .map(uploadMapper::toRecord)
                .toList();
    }

    @Transactional
    public void delete(String fileId, String threadId) {
        uploadRepository.findByFileIdAndThreadId(fileId, threadId).ifPresent(uploadRepository::delete);
    }

    @Transactional(readOnly = true)
    public int count() {
        return (int) uploadRepository.count();
    }
}
