package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.upload.UploadRecord;
import org.wrj.haifa.ai.deerflow.upload.UploadStorageService;

@Component
public class ReadUploadedFileTool implements ParallelSafeAgentTool {

    private static final int MAX_CHARS = 12_000;
    private static final Pattern FILE_ID_PATTERN = Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");

    private final UploadStorageService uploadStorageService;

    public ReadUploadedFileTool(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @Override
    public String name() {
        return "read_uploaded_file";
    }

    @Override
    public String description() {
        return "Reads the content of an uploaded file by its file ID.";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "file_id": {
                      "type": "string",
                      "description": "Uploaded file ID. If omitted, the first file attached to the current request is read."
                    }
                  },
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public boolean supports(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String text = userMessage.toLowerCase();
        return (text.contains("read") || text.contains("open") || text.contains("查看") || text.contains("读取"))
                && (text.contains("upload") || text.contains("file") || text.contains("附件") || text.contains("文件"));
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        List<String> fileIds = request.uploadedFileIds();
        if (fileIds == null || fileIds.isEmpty()) {
            return ToolResult.of(name(), "No uploaded files are associated with this request.");
        }

        Optional<String> targetId = extractFileId(request.userMessage());
        if (targetId.isEmpty()) {
            // If no explicit file ID found, try to read the first uploaded file
            targetId = fileIds.stream().findFirst();
        }

        if (targetId.isEmpty()) {
            return ToolResult.of(name(), "No uploaded file ID was found in the request.");
        }

        String fileId = targetId.get();
        if (!fileIds.contains(fileId)) {
            return ToolResult.of(name(), "File ID not found in the current request's uploaded files: " + fileId);
        }

        UploadRecord record = uploadStorageService.findByFileIdAndThreadId(fileId, request.threadId());
        if (record == null) {
            return ToolResult.of(name(), "File not found: " + fileId);
        }

        try {
            String content = uploadStorageService.readContent(fileId, request.threadId());
            if (content.length() > MAX_CHARS) {
                content = content.substring(0, MAX_CHARS) + "\n...[truncated]";
            }
            return ToolResult.of(name(), "File: " + record.getOriginalFilename() + "\n\n" + content);
        } catch (Exception e) {
            return ToolResult.of(name(), "Failed to read uploaded file: " + e.getMessage());
        }
    }

    private static Optional<String> extractFileId(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = FILE_ID_PATTERN.matcher(userMessage);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }
}
