package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.entity.UploadEntity;
import org.wrj.haifa.ai.deerflow.upload.UploadRecord;

@Component
public class UploadMapper {

    private final JsonMapper jsonMapper;

    public UploadMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public UploadEntity toEntity(UploadRecord record) {
        UploadEntity entity = new UploadEntity();
        entity.setFileId(record.getFileId());
        entity.setThreadId(record.getThreadId());
        entity.setFileName(record.getOriginalFilename());
        entity.setStoredFileName(record.getStoredFilename());
        entity.setStoredPath(record.getStoredPath());
        entity.setMimeType(record.getContentType());
        entity.setFileSize(record.getSize());
        entity.setExtension(record.getExtension());
        entity.setStatus(record.getConversionStatus());
        entity.setError(record.getError());
        entity.setCreatedAt(record.getCreatedAt());
        entity.setUpdatedAt(record.getCreatedAt());
        entity.setMetadataJson(jsonMapper.toJson(java.util.Map.of("converted", record.isConverted(),
                "contentPreview", record.getContentPreview() != null ? record.getContentPreview() : "")));
        return entity;
    }

    public UploadRecord toRecord(UploadEntity entity) {
        UploadRecord record = new UploadRecord();
        record.setFileId(entity.getFileId());
        record.setOriginalFilename(entity.getFileName());
        record.setStoredFilename(entity.getStoredFileName());
        record.setContentType(entity.getMimeType());
        record.setSize(entity.getFileSize() != null ? entity.getFileSize() : 0);
        record.setExtension(entity.getExtension());
        record.setThreadId(entity.getThreadId());
        record.setCreatedAt(entity.getCreatedAt());
        record.setConversionStatus(entity.getStatus());
        record.setError(entity.getError());
        record.setStoredPath(entity.getStoredPath());
        java.util.Map<String, Object> meta = jsonMapper.fromJson(entity.getMetadataJson());
        if (meta.get("contentPreview") instanceof String preview) {
            record.setContentPreview(preview);
        }
        return record;
    }
}
