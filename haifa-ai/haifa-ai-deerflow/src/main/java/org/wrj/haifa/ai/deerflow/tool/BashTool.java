package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.sandbox.CommandPolicy;
import org.wrj.haifa.ai.deerflow.sandbox.LocalRestrictedSandboxRunner;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxRequest;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxResult;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxRunner;

@Component
public class BashTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DeerFlowProperties properties;
    private final SandboxRunner sandboxRunner;
    private final CommandPolicy commandPolicy;

    @Autowired
    public BashTool(DeerFlowProperties properties, SandboxRunner sandboxRunner, CommandPolicy commandPolicy) {
        this.properties = properties;
        this.sandboxRunner = sandboxRunner;
        this.commandPolicy = commandPolicy;
    }

    public BashTool(DeerFlowProperties properties) {
        this(properties, new LocalRestrictedSandboxRunner(properties), new CommandPolicy(properties));
    }

    @Override
    public String name() {
        return "bash";
    }

    @Override
    public String description() {
        return "Run a shell command inside the configured sandbox. Arguments: {\"command\": \"command to run\"}";
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
        if (properties.getSandbox() == null || !properties.getSandbox().isEnabled()) {
            return ToolResult.of(name(), "Tool bash sandbox is disabled by security configuration.",
                    Map.of("denied", true, "reason", "sandbox disabled"));
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

            CommandPolicy.Decision decision = commandPolicy.evaluate(command, request.workspaceRoot());
            if (!decision.allowed()) {
                return ToolResult.of(name(), "Command denied: " + decision.reason(),
                        Map.of("denied", true, "reason", decision.reason()));
            }

            SandboxResult result = sandboxRunner.run(new SandboxRequest(
                    command,
                    request.workspaceRoot(),
                    null,
                    Duration.ofMillis(properties.getSandbox().getTimeoutMs()),
                    Map.of(),
                    properties.getSandbox().isNetworkEnabled(),
                    request.runId()
            ));
            return ToolResult.of(name(), render(result), metadata(result));
        } catch (Exception e) {
            return ToolResult.of(name(), "Error executing command: " + e.getMessage());
        }
    }

    private static String render(SandboxResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("Sandbox ID: ").append(result.sandboxId()).append("\n");
        builder.append("Exit code: ").append(result.exitCode()).append("\n");
        builder.append("Timed out: ").append(result.timedOut()).append("\n");
        builder.append("Duration ms: ").append(result.durationMs()).append("\n");
        builder.append("Output truncated: ").append(result.outputTruncated()).append("\n");
        builder.append("Stdout:\n").append(result.stdout().isBlank() ? "(empty)" : result.stdout()).append("\n");
        builder.append("Stderr:\n").append(result.stderr().isBlank() ? "(empty)" : result.stderr());
        return builder.toString();
    }

    private static Map<String, Object> metadata(SandboxResult result) {
        Map<String, Object> metadata = new HashMap<>(result.metadata());
        metadata.put("sandboxId", result.sandboxId());
        metadata.put("exitCode", result.exitCode());
        metadata.put("stdout", result.stdout());
        metadata.put("stderr", result.stderr());
        metadata.put("durationMs", result.durationMs());
        metadata.put("timedOut", result.timedOut());
        metadata.put("outputTruncated", result.outputTruncated());
        return metadata;
    }
}
