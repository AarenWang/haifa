package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final List<AgentTool> tools;

    public ToolRegistry(List<AgentTool> tools) {
        this.tools = List.copyOf(tools);
    }

    public List<AgentTool> plan(String userMessage, int maxTools) {
        return this.tools.stream()
                .filter(tool -> tool.supports(userMessage))
                .limit(Math.max(0, maxTools))
                .toList();
    }

    public List<AgentTool> tools() {
        return this.tools;
    }
}
