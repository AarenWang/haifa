package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.completion.CompletionRequirement;
import org.wrj.haifa.ai.deerflow.completion.CompletionRequirements;

@Component
public class DeclareCompletionRequirementsTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "declare_completion_requirements";
    }

    @Override
    public String description() {
        return "Declare structured completion requirements before performing work that depends on real observations, "
                + "derived data, delivered artifacts, web sources, file changes, command execution, or user-provided data. "
                + "This Tool adds constraints only and never creates evidence. Arguments: {\"requirements\":[{\"type\":"
                + "\"LOCAL_OBSERVATION|DERIVED_DATA|ARTIFACT_DELIVERY|WEB_CITATION|FILE_MUTATION|COMMAND_EXECUTION|"
                + "USER_PROVIDED_DATA\",\"subject\":\"short description\"}]}";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "requirements": {
                      "type": "array",
                      "minItems": 1,
                      "items": {
                        "type": "object",
                        "properties": {
                          "type": {
                            "type": "string",
                            "enum": ["LOCAL_OBSERVATION", "DERIVED_DATA", "ARTIFACT_DELIVERY", "WEB_CITATION", "FILE_MUTATION", "COMMAND_EXECUTION", "USER_PROVIDED_DATA"]
                          },
                          "subject": {"type": "string"}
                        },
                        "required": ["type"],
                        "additionalProperties": false
                      }
                    }
                  },
                  "required": ["requirements"],
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public boolean supports(String userMessage) {
        return false;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        try {
            JsonNode root = MAPPER.readTree(request.userMessage());
            JsonNode requirementsNode = root == null ? null : root.get("requirements");
            if (requirementsNode == null || !requirementsNode.isArray() || requirementsNode.isEmpty()) {
                return ToolResult.failed(name(), "Error: non-empty requirements array is required");
            }
            List<Object> values = MAPPER.convertValue(requirementsNode, new TypeReference<>() { });
            List<CompletionRequirement> requirements = CompletionRequirements.fromRequestMetadata(
                    Map.of(CompletionRequirements.METADATA_KEY, values));
            if (requirements.isEmpty() || requirements.size() != values.size()) {
                return ToolResult.failed(name(), "Error: every completion requirement type must be valid");
            }
            List<Map<String, Object>> declared = requirements.stream().map(CompletionRequirement::toMap).toList();
            return ToolResult.success(name(), "Declared " + declared.size() + " completion requirement(s).",
                    Map.of("declaredCompletionRequirements", declared));
        } catch (Exception ex) {
            return ToolResult.failed(name(), "Error parsing completion requirements: " + ex.getMessage());
        }
    }
}
