package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.upload.UploadRecord;
import org.wrj.haifa.ai.deerflow.upload.UploadStorageService;

@Component
public class ListUploadedFilesTool implements AgentTool {

    private final UploadStorageService uploadStorageService;

    public ListUploadedFilesTool(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @Override
    public String name() {
        return "list_uploaded_files";
    }

    @Override
    public String description() {
        return "Lists uploaded files for the current thread.";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {},
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
        return text.contains("uploaded file") || text.contains("uploaded files") || text.contains("list upload")
                || text.contains("my files") || text.contains("attached files") || text.contains("上传文件")
                || text.contains("附件");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        List<String> fileIds = request.uploadedFileIds();
        if (fileIds == null || fileIds.isEmpty()) {
            return ToolResult.of(name(), "No uploaded files are associated with this request.");
        }

        List<String> lines = fileIds.stream()
                .map(fileId -> uploadStorageService.findByFileIdAndThreadId(fileId, request.threadId()))
                .filter(r -> r != null)
                .map(r -> "- " + r.getOriginalFilename() + " (id: " + r.getFileId() + ", size: " + r.getSize()
                        + " bytes, status: " + r.getConversionStatus() + ")")
                .collect(Collectors.toList());

        if (lines.isEmpty()) {
            return ToolResult.of(name(), "No uploaded files found for the given IDs.");
        }

        return ToolResult.of(name(), String.join("\n", lines));
    }
}
