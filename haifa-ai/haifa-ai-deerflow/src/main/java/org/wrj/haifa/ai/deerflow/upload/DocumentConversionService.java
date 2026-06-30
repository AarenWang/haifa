package org.wrj.haifa.ai.deerflow.upload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.persistence.store.UploadStore;

@Service
public class DocumentConversionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentConversionService.class);

    private final UploadStorageService uploadStorageService;
    private final DeerFlowProperties properties;
    private final UploadStore uploadStore;

    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "json", "csv", "log", "xml", "yml", "yaml",
            "properties");

    public DocumentConversionService(UploadStorageService uploadStorageService, DeerFlowProperties properties,
            UploadStore uploadStore) {
        this.uploadStorageService = uploadStorageService;
        this.properties = properties;
        this.uploadStore = uploadStore;
    }

    public UploadRecord convert(String fileId) {
        long startTime = System.currentTimeMillis();
        UploadRecord record = uploadStorageService.find(fileId);
        if (record == null) {
            throw new IllegalArgumentException("File not found: " + fileId);
        }

        String extension = record.getExtension().toLowerCase();
        if (!TEXT_EXTENSIONS.contains(extension)) {
            record.setConversionStatus(ConversionStatus.UNSUPPORTED);
            record.setConverted(true);
            record.setError("Unsupported file type for conversion: " + extension);
            log.warn("Unsupported conversion for fileId={}, extension={}", fileId, extension);
            return uploadStore.save(record);
        }

        try {
            Path path = Path.of(record.getStoredPath());
            String content = Files.readString(path, StandardCharsets.UTF_8);
            int maxChars = properties.getMaxConvertedChars();
            String convertedContent;
            if (content.length() > maxChars) {
                convertedContent = content.substring(0, maxChars) + "\n...[truncated]";
            } else {
                convertedContent = content;
            }
            record.setConvertedContent(convertedContent);
            String preview = convertedContent.length() > 200 ? convertedContent.substring(0, 200) + "..."
                    : convertedContent;
            record.setContentPreview(preview);
            record.setConversionStatus(ConversionStatus.COMPLETED);
            record.setConverted(true);
            log.info("Converted fileId={}, originalSize={}, convertedSize={}, durationMs={}", fileId, content.length(),
                    convertedContent.length(), System.currentTimeMillis() - startTime);
            return uploadStore.save(record);
        } catch (IOException e) {
            record.setConversionStatus(ConversionStatus.FAILED);
            record.setConverted(true);
            record.setError("Failed to read file for conversion: " + e.getMessage());
            log.error("Conversion failed for fileId={}", fileId, e);
            return uploadStore.save(record);
        }
    }
}
