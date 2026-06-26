package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;

public record ToolRequest(String userMessage, Path workspaceRoot) {
}
