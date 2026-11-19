package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.evidence.EvidenceItem;
import org.wrj.haifa.ai.deerflow.evidence.EvidenceItemStore;

@Component
public class SubmitEvidenceTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    @Qualifier("EvidenceItemStoreV2")
    private EvidenceItemStore evidenceItemStore;

    @Override
    public String name() {
        return "submit_evidence";
    }

    @Override
    public String description() {
        return "Submit evidence extracted from fetched sources. Arguments: {\"sourceId\": \"...\", \"summary\": \"...\", \"claimSupportText\": \"...\", \"locator\": \"...\", \"confidence\": 0.95, \"workItemId\": \"...\"}";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "sourceId": { "type": "string" },
                    "summary": { "type": "string" },
                    "claimSupportText": { "type": "string" },
                    "locator": { "type": "string" },
                    "confidence": { "type": "number" },
                    "workItemId": { "type": "string" }
                  },
                  "required": ["sourceId", "summary", "claimSupportText"],
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("submit_evidence");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String jsonInput = request.userMessage();
        if (jsonInput == null || jsonInput.isBlank()) {
            return ToolResult.of(name(), "Error: arguments JSON required");
        }

        try {
            JsonNode node = MAPPER.readTree(jsonInput);
            String sourceId = node.get("sourceId").asText();
            String summary = node.get("summary").asText();
            String claimSupportText = node.get("claimSupportText").asText();
            String locator = node.has("locator") ? node.get("locator").asText() : "";
            Double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 1.0;
            String workItemId = node.has("workItemId") ? node.get("workItemId").asText() : null;

            EvidenceItem item = evidenceItemStore.create(sourceId, request.runId(), request.threadId(), workItemId, summary, claimSupportText, locator, confidence);
            return ToolResult.of(name(), "Evidence submitted successfully. Assigned evidenceId: " + item.getEvidenceId(),
                    java.util.Map.of("evidenceId", item.getEvidenceId(), "sourceId", sourceId));
        } catch (Exception e) {
            return ToolResult.of(name(), "Error executing submit_evidence: " + e.getMessage());
        }
    }
}
