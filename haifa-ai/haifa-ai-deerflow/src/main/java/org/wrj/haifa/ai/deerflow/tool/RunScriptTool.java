package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.sandbox.CommandPolicy;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxBackend;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxRequest;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxResult;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxRunner;

@Component
public class RunScriptTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DeerFlowProperties properties;
    private final SandboxRunner sandboxRunner;
    private final CommandPolicy commandPolicy;
    private final UserDataPathResolver pathResolver;

    @Autowired
    public RunScriptTool(DeerFlowProperties properties, SandboxRunner sandboxRunner, CommandPolicy commandPolicy) {
        this.properties = properties;
        this.sandboxRunner = sandboxRunner;
        this.commandPolicy = commandPolicy;
        this.pathResolver = new UserDataPathResolver(properties);
    }

    @Override
    public String name() {
        return "run_script";
    }

    @Override
    public String description() {
        return "Generate and execute short scripts for local observation, lightweight tasks, file inspection, environment checks, and output verification. Scripts run under /mnt/user-data/workspace; final deliverables should be written under /mnt/user-data/outputs. "
                + "Arguments: {\"language\": \"python|powershell|node|bash\", \"code\": \"script code\", \"args\": [\"optional arguments\"], \"purpose\": \"short reason\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String lower = userMessage.toLowerCase();
        return lower.contains("script") || lower.contains("run") || lower.contains("execute")
                || lower.contains("cpu") || lower.contains("memory") || lower.contains("system")
                || lower.contains("computer") || lower.contains("environment")
                || lower.contains("\u4f7f\u7528\u7387") || lower.contains("\u7535\u8111");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (!properties.isRunScriptEnabled()) {
            return ToolResult.of(name(), "Tool run_script is disabled by security configuration.");
        }
        if (properties.getSandbox() == null || !properties.getSandbox().isEnabled()) {
            return ToolResult.of(name(), "Tool run_script sandbox is disabled by security configuration.",
                    Map.of("denied", true, "reason", "sandbox disabled"));
        }
        SandboxBackend backend = SandboxBackend.from(properties.getSandbox().getBackend());
        if (backend == SandboxBackend.LOCAL && !properties.getSandbox().isRunScriptLocalUnsafeAllowed()) {
            return ToolResult.of(name(), "Execution denied: run_script requires docker sandbox backend for strong isolation. To override this for local unsafe development, set haifa.ai.deerflow.sandbox.run-script-local-unsafe-allowed=true.",
                    Map.of("denied", true, "reason", "local backend execution not allowed"));
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

            String language = node.has("language") ? node.get("language").asText() : null;
            String code = node.has("code") ? node.get("code").asText() : null;
            String purpose = node.has("purpose") ? node.get("purpose").asText() : "";

            java.util.List<String> args = new java.util.ArrayList<>();
            if (node.has("args") && node.get("args").isArray()) {
                for (JsonNode argNode : node.get("args")) {
                    args.add(argNode.asText());
                }
            }

            if (language == null || language.isBlank()) {
                return ToolResult.of(name(), "Error: language is required");
            }
            if (code == null || code.isBlank()) {
                return ToolResult.of(name(), "Error: code is required");
            }
            if (code.length() > 65536) {
                return ToolResult.of(name(), "Error: code exceeds maximum size of 64 KB");
            }
            CommandPolicy.Decision bodyDecision = commandPolicy.evaluateScriptBody(code);
            if (!bodyDecision.allowed()) {
                return ToolResult.of(name(), "Script execution denied: " + bodyDecision.reason(),
                        Map.of("denied", true, "reason", bodyDecision.reason()));
            }
            if (args != null) {
                for (String arg : args) {
                    if (arg != null && arg.length() > 2048) {
                        return ToolResult.of(name(), "Error: argument exceeds maximum size of 2 KB");
                    }
                }
            }

            String allowedLangs = properties.getSandbox().getAllowedScriptLanguages();
            java.util.List<String> allowedList = java.util.Arrays.stream(allowedLangs.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            if (!allowedList.contains(language.toLowerCase())) {
                return ToolResult.of(name(), "Error: language '" + language + "' is not allowed by security configuration. Allowed: " + allowedLangs,
                        Map.of("denied", true, "reason", "language not allowed"));
            }

            String ext;
            try {
                ext = getExtensionFor(language);
            } catch (IllegalArgumentException ex) {
                return ToolResult.of(name(), ex.getMessage());
            }

            String runId = request.runId() == null || request.runId().isBlank() ? "adhoc" : request.runId();
            String scriptFolderId = UUID.randomUUID().toString();
            Path workspaceRootPath = pathResolver.workspaceRoot();

            Path baseDir = workspaceRootPath.resolve(Path.of(
                    properties.getSandbox().getWorkdirSubdir(),
                    runId,
                    properties.getSandbox().getScriptWorkdirSubdir(),
                    scriptFolderId
            ));
            Path normalizedDir = baseDir.toAbsolutePath().normalize();
            if (!normalizedDir.startsWith(workspaceRootPath)) {
                return ToolResult.of(name(), "Error: destination directory is outside allowed workspace root");
            }

            Path scriptFile = normalizedDir.resolve("script" + ext).normalize();
            if (!scriptFile.startsWith(workspaceRootPath)) {
                return ToolResult.of(name(), "Error: script file path is outside allowed workspace root");
            }

            try {
                Files.createDirectories(normalizedDir);
                Files.writeString(scriptFile, code, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                return ToolResult.of(name(), "Error writing script file: " + ex.getMessage());
            }

            java.util.List<String> cmdArgs;
            try {
                cmdArgs = commandArgsFor(language, "script" + ext, args);
            } catch (IllegalArgumentException ex) {
                return ToolResult.of(name(), ex.getMessage());
            }
            String commandToRun = String.join(" ", cmdArgs);
            CommandPolicy.Decision policyDecision = commandPolicy.evaluate(commandToRun, pathResolver.workspaceRoot());
            if (!policyDecision.allowed()) {
                return ToolResult.of(name(), "Command denied by policy: " + policyDecision.reason(),
                        Map.of("denied", true, "reason", policyDecision.reason()));
            }

            String relativeScriptPath = workspaceRootPath.relativize(scriptFile.toAbsolutePath()).toString().replace('\\', '/');

            SandboxResult result = sandboxRunner.run(new SandboxRequest(
                    commandToRun,
                    cmdArgs,
                    pathResolver.workspaceRoot(),
                    normalizedDir,
                    Duration.ofMillis(properties.getSandbox().getTimeoutMs()),
                    Map.of(
                            "DEERFLOW_UPLOADS_DIR", pathResolver.uploadsRoot().toString(),
                            "DEERFLOW_WORKSPACE_DIR", pathResolver.workspaceRoot().toString(),
                            "DEERFLOW_OUTPUTS_DIR", pathResolver.outputsRoot().toString()),
                    properties.getSandbox().isNetworkEnabled(),
                    request.runId()
            ));

            return ToolResult.of(name(), render(result, language), metadata(result, language, purpose, relativeScriptPath));
        } catch (Exception e) {
            return ToolResult.of(name(), "Error executing script: " + e.getMessage());
        }
    }

    private String getExtensionFor(String language) {
        return switch (language.toLowerCase()) {
            case "python", "python3" -> ".py";
            case "powershell" -> ".ps1";
            case "node" -> ".js";
            case "bash" -> ".sh";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private java.util.List<String> commandArgsFor(String language, String scriptFileName, java.util.List<String> args) {
        java.util.List<String> cmd = new java.util.ArrayList<>();
        switch (language.toLowerCase()) {
            case "python" -> cmd.add("python");
            case "python3" -> cmd.add("python3");
            case "powershell" -> {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    cmd.add("powershell");
                } else {
                    cmd.add("pwsh");
                }
                cmd.add("-NoProfile");
                cmd.add("-ExecutionPolicy");
                cmd.add("Bypass");
                cmd.add("-File");
            }
            case "node" -> cmd.add("node");
            case "bash" -> cmd.add("bash");
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        }
        cmd.add(scriptFileName);
        if (args != null) {
            cmd.addAll(args);
        }
        return cmd;
    }

    private static String render(SandboxResult result, String language) {
        StringBuilder builder = new StringBuilder();
        builder.append("Script language: ").append(language).append("\n");
        builder.append("Sandbox ID: ").append(result.sandboxId()).append("\n");
        builder.append("Exit code: ").append(result.exitCode()).append("\n");
        builder.append("Timed out: ").append(result.timedOut()).append("\n");
        builder.append("Duration ms: ").append(result.durationMs()).append("\n");
        builder.append("Output truncated: ").append(result.outputTruncated()).append("\n");
        builder.append("Stdout:\n").append(result.stdout().isBlank() ? "(empty)" : result.stdout()).append("\n");
        builder.append("Stderr:\n").append(result.stderr().isBlank() ? "(empty)" : result.stderr());
        return builder.toString();
    }

    private static Map<String, Object> metadata(SandboxResult result, String language, String purpose, String scriptPath) {
        Map<String, Object> metadata = new HashMap<>(result.metadata());
        metadata.put("toolName", "run_script");
        metadata.put("language", language);
        metadata.put("purpose", purpose);
        metadata.put("sandboxId", result.sandboxId());
        metadata.put("exitCode", result.exitCode());
        metadata.put("stdout", result.stdout());
        metadata.put("stderr", result.stderr());
        metadata.put("durationMs", result.durationMs());
        metadata.put("timedOut", result.timedOut());
        metadata.put("outputTruncated", result.outputTruncated());
        metadata.put("scriptPath", scriptPath);
        return metadata;
    }
}
