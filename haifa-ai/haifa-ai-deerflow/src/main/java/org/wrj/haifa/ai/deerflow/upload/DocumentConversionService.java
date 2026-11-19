package org.wrj.haifa.ai.deerflow.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
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
    private static final Set<String> WORD_EXTENSIONS = Set.of("docx");

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
        if (!TEXT_EXTENSIONS.contains(extension) && !WORD_EXTENSIONS.contains(extension)) {
            record.setConversionStatus(ConversionStatus.UNSUPPORTED);
            record.setConverted(true);
            record.setError("Unsupported file type for conversion: " + extension);
            log.warn("Unsupported conversion for fileId={}, extension={}", fileId, extension);
            return uploadStore.save(record);
        }

        try {
            Path path = Path.of(record.getStoredPath());
            String content = extractContent(path, extension);
            String convertedContent = limitConvertedContent(content);
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

    private String extractContent(Path path, String extension) throws IOException {
        if (TEXT_EXTENSIONS.contains(extension)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        if ("docx".equals(extension)) {
            return extractDocxContent(path);
        }
        throw new IOException("Unsupported file type for conversion: " + extension);
    }

    private static String extractDocxContent(Path path) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = Files.newInputStream(path);
                XWPFDocument document = new XWPFDocument(inputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(content, paragraph.getText());
            }
            for (XWPFTable table : document.getTables()) {
                appendTable(content, table);
            }
        }
        return content.toString().trim();
    }

    private static void appendTable(StringBuilder content, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            StringBuilder rowText = new StringBuilder();
            for (XWPFTableCell cell : row.getTableCells()) {
                if (!rowText.isEmpty()) {
                    rowText.append('\t');
                }
                rowText.append(cell.getText());
            }
            appendLine(content, rowText.toString());
        }
    }

    private static void appendLine(StringBuilder content, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!content.isEmpty()) {
            content.append(System.lineSeparator());
        }
        content.append(text.trim());
    }

    private String limitConvertedContent(String content) {
        int maxChars = properties.getMaxConvertedChars();
        if (content.length() > maxChars) {
            return content.substring(0, maxChars) + "\n...[truncated]";
        }
        return content;
    }
}
