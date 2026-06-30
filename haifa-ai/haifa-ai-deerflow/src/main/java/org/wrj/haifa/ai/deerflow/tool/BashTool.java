package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class BashTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DeerFlowProperties properties;

    public BashTool(DeerFlowProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "bash";
    }

    @Override
    public String description() {
        return "Run a shell command inside the workspace. Arguments: {\"command\": \"command to run\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("bash");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (!properties.isBashEnabled()) {
            return ToolResult.of(name(), "Tool bash is disabled by security configuration.");
        }
        try {
            String jsonInput = request.userMessage();
            if (jsonInput == null || jsonInput.isBlank()) {
                return ToolResult.of(name(), "Error: arguments JSON required");
            }
            JsonNode node;
            try {
                node = MAPPER.readTree(jsonInput);
            } catch (Exception jsonEx) {
                return ToolResult.of(name(), "Error parsing tool arguments as JSON: " + jsonEx.getMessage());
            }
            String command = node.has("command") ? node.get("command").asText() : null;
            if (command == null || command.isBlank()) {
                return ToolResult.of(name(), "Error: command is required");
            }

            List<String> cmd = new ArrayList<>();
            // Determine OS to execute command properly
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                cmd.add("cmd.exe");
                cmd.add("/c");
                cmd.add(command);
            } else {
                cmd.add("/bin/bash");
                cmd.add("-c");
                cmd.add(command);
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(request.workspaceRoot().toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.of(name(), output.toString() + "\n\nError: command timed out after 30 seconds.");
            }

            int exitCode = process.exitValue();
            return ToolResult.of(name(), "Exit code: " + exitCode + "\nOutput:\n" + output.toString());
        } catch (Exception e) {
            return ToolResult.of(name(), "Error executing command: " + e.getMessage());
        }
    }
}
