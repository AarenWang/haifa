package org.wrj.haifa.ai.deerflow.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.sandbox.CommandPolicy;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxBackend;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.mcp.McpAgentTool;
import org.wrj.haifa.ai.deerflow.mcp.McpConnectionManager;
import org.wrj.haifa.ai.deerflow.mcp.McpRiskClassification;
import org.wrj.haifa.ai.deerflow.mcp.McpSemanticType;
import org.wrj.haifa.ai.deerflow.mcp.McpToolIdentity;

@Component
public class ApprovalPolicyService {

    private final DeerFlowProperties properties;
    private final ApprovalStore approvalStore;
    private final CommandPolicy commandPolicy;
    private final McpConnectionManager mcpConnectionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApprovalPolicyService(DeerFlowProperties properties, ApprovalStore approvalStore, CommandPolicy commandPolicy) {
        this(properties, approvalStore, commandPolicy, null);
    }

    @Autowired
    public ApprovalPolicyService(DeerFlowProperties properties, ApprovalStore approvalStore, CommandPolicy commandPolicy,
            McpConnectionManager mcpConnectionManager) {
        this.properties = properties;
        this.approvalStore = approvalStore;
        this.commandPolicy = commandPolicy;
        this.mcpConnectionManager = mcpConnectionManager;
    }

    public ApprovalPolicyDecision evaluate(ModelToolCall toolCall, AgentTool tool, AgentRunConfig runConfig) {
        McpToolIdentity mcpIdentity = tool instanceof McpAgentTool mcpTool ? mcpTool.identity() : null;
        if (properties.getApproval() == null || !properties.getApproval().isEnabled()) {
            if (mcpIdentity != null && requiresMcpApproval(mcpIdentity)) {
                return new ApprovalPolicyDecision(ApprovalPolicyDecisionType.DENY, RiskLevel.BLOCKED,
                        "MCP tool requires approval but approval is disabled", null, null,
                        Map.of("connectionName", mcpIdentity.connectionName(), "originalToolName", mcpIdentity.originalToolName()));
            }
            return new ApprovalPolicyDecision(ApprovalPolicyDecisionType.ALLOW, RiskLevel.LOW, "Approval globally disabled", null, null, Map.of());
        }

        String toolName = toolCall.name();
        String arguments = toolCall.arguments() != null ? toolCall.arguments() : "";

        // 1. Hardline patterns check. These patterns are command-oriented, so
        // keep them away from low-risk interaction/search tool arguments such
        // as ask_clarification.delivery_format.
        if (properties.getApproval().isHardlinePatternsEnabled()) {
            if (shouldScanArgumentsForHardlinePatterns(toolName)) {
                String argsLower = arguments.toLowerCase().replace("\\", "/");
                String[] hardlineKeywords = {
                    "rm -rf /", "del /s /q", "format", "shutdown", "reboot", "sudo", "runas",
                    ".ssh/id_rsa", ".aws/credentials", ".env"
                };
                for (String kw : hardlineKeywords) {
                    if (argsLower.contains(kw)) {
                        return new ApprovalPolicyDecision(
                                ApprovalPolicyDecisionType.DENY,
                                RiskLevel.BLOCKED,
                                "Command or arguments hit hardline refuse pattern: " + kw,
                                null,
                                null,
                                Map.of()
                        );
                    }
                }
            }

            if ("run_script".equals(toolName)) {
                try {
                    JsonNode node = objectMapper.readTree(arguments);
                    String code = node.has("code") ? node.get("code").asText() : "";
                    CommandPolicy.Decision scriptDecision = commandPolicy.evaluateScriptBody(code);
                    if (!scriptDecision.allowed()) {
                        return new ApprovalPolicyDecision(
                                ApprovalPolicyDecisionType.DENY,
                                RiskLevel.BLOCKED,
                                "Script content denied by safety policy: " + scriptDecision.reason(),
                                null,
                                null,
                                Map.of()
                        );
                    }
                } catch (Exception e) {
                    // JSON parsing failed, let it be caught/handled by tool execution itself
                }
            }
        }

        // 2. Risk Key and Hash calculation
        String riskKey = mcpIdentity == null ? generateRiskKey(toolName, arguments)
                : mcpRiskKey(mcpIdentity, arguments);
        String argsHash = mcpIdentity == null ? hashArgs(toolName, arguments)
                : hashMcpArgs(mcpIdentity, arguments);

        // 3. Session approvals lookup
        if (properties.getApproval().isAllowSessionApproval()) {
            List<ApprovalRequestRecord> sessionApprovals = approvalStore.findByThreadId(runConfig.threadId());
            for (ApprovalRequestRecord record : sessionApprovals) {
                boolean isValidSessionApproval = (record.status() == ApprovalStatus.APPROVED || record.status() == ApprovalStatus.EXECUTED)
                        && record.decisionType() == ApprovalDecisionType.APPROVE_SESSION
                        && riskKey.equals(record.riskKey())
                        && argsHash.equals(record.argsHash());
                
                if (isValidSessionApproval) {
                    return new ApprovalPolicyDecision(ApprovalPolicyDecisionType.ALLOW, record.riskLevel(), "Session approved action matched", riskKey, record.preview(), Map.of());
                }
            }
        }

        // 4. Always approvals lookup (persistent/global user allowlist)
        if (properties.getApproval().isAllowAlwaysApproval()) {
            List<ApprovalRequestRecord> alwaysApprovals = approvalStore.findAlwaysApprovals();
            for (ApprovalRequestRecord record : alwaysApprovals) {
                boolean isValidAlwaysApproval = (record.status() == ApprovalStatus.APPROVED || record.status() == ApprovalStatus.EXECUTED)
                        && record.decisionType() == ApprovalDecisionType.APPROVE_ALWAYS
                        && riskKey.equals(record.riskKey())
                        && argsHash.equals(record.argsHash());
                
                if (isValidAlwaysApproval) {
                    return new ApprovalPolicyDecision(ApprovalPolicyDecisionType.ALLOW, record.riskLevel(), "Always approved action matched globally", riskKey, record.preview(), Map.of());
                }
            }
        }

        // 5. Default approval rules evaluation
        String preview = generatePreview(toolName, arguments);
        boolean isLocal = properties.getSandbox() != null
                && SandboxBackend.from(properties.getSandbox().getBackend()) != SandboxBackend.DOCKER;
        boolean isNetwork = properties.getSandbox() != null && properties.getSandbox().isNetworkEnabled();

        if (mcpIdentity != null && requiresMcpApproval(mcpIdentity)) {
            Map<String, Object> metadata = new java.util.LinkedHashMap<>();
            metadata.put("connectionName", mcpIdentity.connectionName());
            metadata.put("originalToolName", mcpIdentity.originalToolName());
            metadata.put("exposedName", mcpIdentity.exposedName());
            metadata.put("snapshotVersion", mcpIdentity.discoverySnapshotVersion());
            metadata.put("localRisk", mcpIdentity.localRiskClassification().name());
            metadata.put("semanticType", mcpIdentity.semanticType().name());
            metadata.put("capabilityOwner", mcpIdentity.capabilityOwner() == null ? "" : mcpIdentity.capabilityOwner().name());
            String destination = destinationHost(arguments);
            if (!destination.isBlank()) metadata.put("destinationHost", destination);
            return new ApprovalPolicyDecision(
                    ApprovalPolicyDecisionType.REQUIRE_APPROVAL,
                    mcpIdentity.localRiskClassification() == McpRiskClassification.DESTRUCTIVE ? RiskLevel.HIGH : RiskLevel.MEDIUM,
                    mcpIdentity.semanticType() == McpSemanticType.WEB_FETCH
                            ? "External fetch requires human approval"
                            : "MCP tool risk classification requires human approval",
                    riskKey,
                    redactPreview(arguments),
                    Map.copyOf(metadata));
        }

        if ("run_script".equals(toolName)) {
            if (isNetwork && properties.getApproval().isRequireForNetwork()) {
                return new ApprovalPolicyDecision(
                        ApprovalPolicyDecisionType.REQUIRE_APPROVAL,
                        RiskLevel.HIGH,
                        "Script execution with network enabled requires human approval",
                        riskKey,
                        preview,
                        Map.of()
                );
            }
            if (isLocal && properties.getApproval().isRequireForLocalScript()) {
                return new ApprovalPolicyDecision(
                        ApprovalPolicyDecisionType.REQUIRE_APPROVAL,
                        RiskLevel.HIGH,
                        "Local script execution requires human approval",
                        riskKey,
                        preview,
                        Map.of()
                );
            }
        }

        if (properties.getApproval().isRequireForFileWrite()) {
            if ("write_file".equals(toolName) || "patch".equals(toolName) || "str_replace".equals(toolName)) {
                return new ApprovalPolicyDecision(
                        ApprovalPolicyDecisionType.REQUIRE_APPROVAL,
                        RiskLevel.MEDIUM,
                        "File write/modification requires human approval",
                        riskKey,
                        preview,
                        Map.of()
                );
            }
        }

        if ("bash".equals(toolName)) {
            return new ApprovalPolicyDecision(
                    ApprovalPolicyDecisionType.REQUIRE_APPROVAL,
                    RiskLevel.HIGH,
                    "Shell command execution requires human approval",
                    riskKey,
                    preview,
                    Map.of()
            );
        }

        // default is allow
        return new ApprovalPolicyDecision(ApprovalPolicyDecisionType.ALLOW, RiskLevel.LOW, "Auto allowed read-only or low risk tool", riskKey, preview, Map.of());
    }

    public String generateRiskKey(String toolName, String arguments) {
        String riskKey = "tool=" + toolName;
        if ("run_script".equals(toolName)) {
            try {
                JsonNode node = objectMapper.readTree(arguments);
                String language = node.has("language") ? node.get("language").asText().toLowerCase() : "unknown";
                String backend = properties.getSandbox() != null ? properties.getSandbox().getBackend().toLowerCase() : "local";
                boolean network = properties.getSandbox() != null && properties.getSandbox().isNetworkEnabled();
                riskKey += ";language=" + language + ";backend=" + backend + ";network=" + network;
            } catch (Exception e) {
                // Ignore
            }
        }
        return riskKey;
    }

    public String hashArgs(String toolName, String argsJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = toolName + ":" + (argsJson == null ? "" : argsJson);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "sha256:" + hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isApprovalStillValid(ApprovalRequestRecord approval, ModelToolCall toolCall) {
        if (approval == null || toolCall == null) {
            return false;
        }
        String currentHash = mcpConnectionManager == null ? hashArgs(toolCall.name(), toolCall.arguments())
                : mcpConnectionManager.findIdentity(toolCall.name())
                        .map(identity -> hashMcpArgs(identity, toolCall.arguments()))
                        .orElseGet(() -> hashArgs(toolCall.name(), toolCall.arguments()));
        return approval.argsHash().equals(currentHash);
    }

    private static boolean requiresMcpApproval(McpToolIdentity identity) {
        return identity.semanticType() == McpSemanticType.WEB_FETCH
                || identity.localRiskClassification() == McpRiskClassification.UNKNOWN
                || identity.localRiskClassification() == McpRiskClassification.WRITE
                || identity.localRiskClassification() == McpRiskClassification.DESTRUCTIVE;
    }

    private String mcpRiskKey(McpToolIdentity identity, String arguments) {
        String owner = identity.capabilityOwner() == null ? "NONE" : identity.capabilityOwner().name();
        String destination = destinationHost(arguments);
        return "connection=" + identity.connectionName() + ";tool=" + identity.originalToolName()
                + ";owner=" + owner + ";snapshot=" + identity.discoverySnapshotVersion()
                + (destination.isBlank() ? "" : ";host=" + destination);
    }

    private String hashMcpArgs(McpToolIdentity identity, String arguments) {
        return hashArgs(mcpRiskKey(identity, arguments), arguments);
    }

    private String destinationHost(String arguments) {
        try {
            JsonNode node = objectMapper.readTree(arguments);
            JsonNode value = node.has("url") ? node.get("url") : node.get("uri");
            if (value == null || !value.isTextual()) return "";
            java.net.URI uri = java.net.URI.create(value.asText());
            return uri.getHost() == null ? "" : uri.getHost().toLowerCase(java.util.Locale.ROOT);
        }
        catch (Exception ex) {
            return "";
        }
    }

    private String redactPreview(String arguments) {
        try {
            JsonNode root = objectMapper.readTree(arguments);
            redactNode(root);
            String value = objectMapper.writeValueAsString(root);
            return value.length() <= 1000 ? value : value.substring(0, 1000) + "... (truncated)";
        }
        catch (Exception ex) {
            return "[unparseable arguments omitted]";
        }
    }

    private static void redactNode(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            java.util.List<String> names = new java.util.ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                if (name.toLowerCase(java.util.Locale.ROOT).matches(".*(token|secret|password|cookie|authorization|api.?key).*$")) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(name, "[REDACTED]");
                }
                else redactNode(node.get(name));
            }
        }
        else if (node.isArray()) node.forEach(ApprovalPolicyService::redactNode);
    }

    private String generatePreview(String toolName, String arguments) {
        if ("run_script".equals(toolName)) {
            try {
                JsonNode node = objectMapper.readTree(arguments);
                String code = node.has("code") ? node.get("code").asText() : "";
                if (code.length() > 1000) {
                    return code.substring(0, 1000) + "\n... (truncated)";
                }
                return code;
            } catch (Exception e) {
                return arguments;
            }
        }
        if (arguments.length() > 1000) {
            return arguments.substring(0, 1000) + "\n... (truncated)";
        }
        return arguments;
    }

    private boolean shouldScanArgumentsForHardlinePatterns(String toolName) {
        return Set.of("run_script", "bash", "write_file", "str_replace").contains(toolName);
    }
}
