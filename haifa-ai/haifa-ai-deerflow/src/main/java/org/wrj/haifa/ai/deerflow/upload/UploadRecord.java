package org.wrj.haifa.ai.deerflow.upload;

import java.time.Instant;

public class UploadRecord {

    private String fileId;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private long size;
    private String extension;
    private String threadId;
    private Instant createdAt;
    private boolean converted;
    private ConversionStatus conversionStatus;
    private String error;
    private String contentPreview;
    private String storedPath;
    private String convertedContent;

    public UploadRecord() {
        this.createdAt = Instant.now();
        this.converted = false;
        this.conversionStatus = ConversionStatus.PENDING;
    }

    public UploadRecord(String fileId, String originalFilename, String storedFilename, String contentType,
                        long size, String extension, String threadId, String storedPath) {
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.size = size;
        this.extension = extension;
        this.threadId = threadId;
        this.storedPath = storedPath;
        this.createdAt = Instant.now();
        this.converted = false;
        this.conversionStatus = ConversionStatus.PENDING;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isConverted() {
        return converted;
    }

    public void setConverted(boolean converted) {
        this.converted = converted;
    }

    public ConversionStatus getConversionStatus() {
        return conversionStatus;
    }

    public void setConversionStatus(ConversionStatus conversionStatus) {
        this.conversionStatus = conversionStatus;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;
    }

    public String getConvertedContent() {
        return convertedContent;
    }

    public void setConvertedContent(String convertedContent) {
        this.convertedContent = convertedContent;
    }
}
