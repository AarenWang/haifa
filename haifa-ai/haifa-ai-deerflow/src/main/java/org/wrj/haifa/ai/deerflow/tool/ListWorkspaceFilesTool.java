package org.wrj.haifa.ai.deerflow.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class ListWorkspaceFilesTool implements AgentTool {

    private static final int MAX_FILES = 80;

    @Override
    public String name() {
        return "list_workspace_files";
    }

    @Override
    public String description() {
        return "Lists files directly under the configured workspace root.";
    }

    @Override
    public boolean supports(String userMessage) {
        String text = userMessage == null ? "" : userMessage.toLowerCase();
        return text.contains("list") || text.contains("files") || text.contains("workspace") || text.contains("dir")
                || text.contains("目录") || text.contains("文件") || text.contains("列出");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        Path root = request.workspaceRoot().toAbsolutePath().normalize();
        try (Stream<Path> paths = Files.list(root)) {
            String content = paths
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .limit(MAX_FILES)
                    .map(path -> {
                        String suffix = Files.isDirectory(path) ? "/" : "";
                        return root.relativize(path).toString().replace('\\', '/') + suffix;
                    })
                    .collect(Collectors.joining("\n"));
            return ToolResult.of(name(), content.isBlank() ? "(workspace is empty)" : content);
        }
        catch (IOException ex) {
            return ToolResult.of(name(), "Failed to list workspace: " + ex.getMessage());
        }
    }
}
