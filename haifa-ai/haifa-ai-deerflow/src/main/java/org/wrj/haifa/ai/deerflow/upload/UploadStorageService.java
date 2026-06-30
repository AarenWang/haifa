package org.wrj.haifa.ai.deerflow.upload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Service
public class UploadStorageService {

    private static final Logger log = LoggerFactory.getLogger(UploadStorageService.class);

    private final DeerFlowProperties properties;
    private final ConcurrentHashMap<String, UploadRecord> metadataStore = new ConcurrentHashMap<>();
    private final Set<String> allowedExtensions;

    public UploadStorageService(DeerFlowProperties properties) {
        this.properties = properties;
        this.allowedExtensions = Arrays.stream(properties.getAllowedUploadExtensions().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public UploadRecord store(FilePart filePart, String threadId) {
        long startTime = System.currentTimeMillis();
        String originalFilename = filePart.filename();

        if (!StringUtils.hasText(threadId)) {
            throw new IllegalArgumentException("threadId is required for uploads");
        }

        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("Filename must not be empty");
        }

        String extension = getExtension(originalFilename);
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File extension not allowed: " + extension);
        }

        // Pre-check size from Content-Length header if available
        long contentLength = filePart.headers().getContentLength();
        if (contentLength > 0 && contentLength > properties.getMaxUploadBytes()) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed: " + properties.getMaxUploadBytes() + " bytes");
        }

        DataBuffer joined;
        try {
            joined = DataBufferUtils.join(filePart.content(), maxUploadBytesAsInt()).block();
        } catch (DataBufferLimitException e) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed: " + properties.getMaxUploadBytes() + " bytes", e);
        }
        if (joined == null) {
            throw new IllegalArgumentException("Failed to read file content");
        }
        byte[] content;
        try {
            content = new byte[joined.readableByteCount()];
            joined.read(content);
        } finally {
            DataBufferUtils.release(joined);
        }

        if (content.length > properties.getMaxUploadBytes()) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed: " + properties.getMaxUploadBytes() + " bytes");
        }

        String sanitizedFilename = sanitizeFilename(originalFilename);
        String fileId = UUID.randomUUID().toString();
        String storedFilename = fileId + "_" + sanitizedFilename;

        try {
            Path uploadsDir = Path.of(properties.getUploadsRoot()).toAbsolutePath().normalize();
            Files.createDirectories(uploadsDir);
            Path targetPath = uploadsDir.resolve(storedFilename);
            // Prevent path traversal: ensure resolved path is still under uploadsDir
            if (!targetPath.startsWith(uploadsDir)) {
                throw new IllegalArgumentException("Invalid filename: path traversal detected");
            }
            Files.write(targetPath, content);

            UploadRecord record = new UploadRecord(
                    fileId,
                    originalFilename,
                    storedFilename,
                    filePart.headers().getContentType() != null ? filePart.headers().getContentType().toString()
                            : "application/octet-stream",
                    content.length,
                    extension,
                    threadId,
                    targetPath.toString());

            metadataStore.put(fileId, record);
            log.info("Stored upload fileId={}, originalFilename={}, size={}, threadId={}, durationMs={}",
                    fileId, originalFilename, content.length, threadId, System.currentTimeMillis() - startTime);
            return record;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    public UploadRecord find(String fileId) {
        return metadataStore.get(fileId);
    }

    public UploadRecord findByFileIdAndThreadId(String fileId, String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return null;
        }
        UploadRecord record = metadataStore.get(fileId);
        if (record == null) {
            return null;
        }
        if (!threadId.equals(record.getThreadId())) {
            return null; // File does not belong to this thread
        }
        return record;
    }

    public List<UploadRecord> list(String threadId) {
        if (!StringUtils.hasText(threadId)) {
            return List.of(); // Return empty list instead of all files when no thread is specified
        }
        return metadataStore.values().stream()
                .filter(r -> threadId.equals(r.getThreadId()))
                .collect(Collectors.toList());
    }

    public String readContent(String fileId, String threadId) {
        UploadRecord record = findByFileIdAndThreadId(fileId, threadId);
        if (record == null) {
            throw new IllegalArgumentException("File not found: " + fileId);
        }
        if (record.getConvertedContent() != null) {
            return record.getConvertedContent();
        }
        try {
            Path path = Path.of(record.getStoredPath());
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content: " + fileId, e);
        }
    }

    public void delete(String fileId, String threadId) {
        UploadRecord record = findByFileIdAndThreadId(fileId, threadId);
        if (record == null) {
            throw new IllegalArgumentException("File not found: " + fileId);
        }
        metadataStore.remove(fileId);
        try {
            Path path = Path.of(record.getStoredPath());
            Files.deleteIfExists(path);
            log.info("Deleted upload fileId={}, originalFilename={}", fileId, record.getOriginalFilename());
        } catch (IOException e) {
            log.warn("Failed to delete file from disk: fileId={}, path={}", fileId, record.getStoredPath(), e);
        }
    }

    public int count() {
        return metadataStore.size();
    }

    private static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private static String sanitizeFilename(String filename) {
        String name = Path.of(filename).getFileName().toString();
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private int maxUploadBytesAsInt() {
        return (int) Math.min(properties.getMaxUploadBytes(), Integer.MAX_VALUE);
    }
}
