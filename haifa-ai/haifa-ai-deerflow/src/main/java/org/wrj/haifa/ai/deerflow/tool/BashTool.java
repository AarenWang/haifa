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
        return "Run a shell command inside the configured sandbox. Arguments: {\"description\": \"optional summary\", \"command\": \"command to run\"}. Use /mnt/skills and /mnt/user-data virtual paths.";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("bash");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (!properties.isBashEnabled()) {
            return ToolResult.denied(name(), "Tool bash is disabled by security configuration.",
                    Map.of("denied", true, "reason", "bash disabled"));
        }
        if (properties.getSandbox() == null || !properties.getSandbox().isEnabled()) {
            return ToolResult.denied(name(), "Tool bash sandbox is disabled by security configuration.",
                    Map.of("denied", true, "reason", "sandbox disabled"));
        }
        try {
            String jsonInput = request.userMessage();
            if (jsonInput == null || jsonInput.isBlank()) {
                return ToolResult.failed(name(), "Error: arguments JSON required");
            }
            JsonNode node;
            try {
                node = MAPPER.readTree(jsonInput);
            } catch (Exception jsonEx) {
                return ToolResult.failed(name(), "Error parsing tool arguments as JSON: " + jsonEx.getMessage());
            }
            String command = node.has("command") ? node.get("command").asText() : null;
            if (command == null || command.isBlank()) {
                return ToolResult.failed(name(), "Error: command is required");
            }
            command = normalizeLineContinuations(command);

            CommandPolicy.Decision decision = commandPolicy.evaluate(command, request.workspaceRoot());
            if (!decision.allowed()) {
                return ToolResult.denied(name(), "Command denied: " + decision.reason(),
                        Map.of("denied", true, "reason", decision.reason()));
            }

            SandboxResult result = sandboxRunner.run(new SandboxRequest(
                    command,
                    request.workspaceRoot(),
                    null,
                    Duration.ofMillis(properties.getSandbox().getTimeoutMs()),
                    configuredEnvironment(),
                    properties.getSandbox().isNetworkEnabled(),
                    request.runId()
            ));
            Map<String, Object> metadata = metadata(result);
            if (result.timedOut()) {
                return ToolResult.timedOut(name(), render(result), metadata);
            }
            if (result.exitCode() != 0) {
                return ToolResult.failed(name(), render(result), metadata);
            }
            return ToolResult.success(name(), render(result), metadata);
        } catch (Exception e) {
            return ToolResult.failed(name(), "Error executing command: " + e.getMessage(),
                    Map.of("errorType", e.getClass().getSimpleName()));
        }
    }

    private Map<String, String> configuredEnvironment() {
        Map<String, String> configured = properties.getSandbox().getEnvironment();
        if (configured == null || configured.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        configured.forEach((key, value) -> {
            if (key != null && key.matches("[A-Za-z_][A-Za-z0-9_]*") && value != null && !value.isBlank()) {
                result.put(key, value);
            }
        });
        return Map.copyOf(result);
    }

    private static String normalizeLineContinuations(String command) {
        return command.replaceAll("\\\\\\r?\\n\\s*", " ").trim();
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
