package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.claim.Citation;
import org.wrj.haifa.ai.deerflow.claim.CitationStore;

@Component
public class SubmitCitationTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private CitationStore citationStore;

    @Override
    public String name() {
        return "submit_citation";
    }

    @Override
    public String description() {
        return "Submit a citation linking a claim to evidence and source. Arguments: {\"claimId\": \"...\", \"evidenceId\": \"...\", \"sourceId\": \"...\", \"locator\": \"...\", \"status\": \"valid\"}";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "claimId": { "type": "string" },
                    "evidenceId": { "type": "string" },
                    "sourceId": { "type": "string" },
                    "locator": { "type": "string" },
                    "status": { "type": "string" }
                  },
                  "required": ["claimId", "evidenceId", "sourceId"],
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("submit_citation");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String jsonInput = request.userMessage();
        if (jsonInput == null || jsonInput.isBlank()) {
            return ToolResult.of(name(), "Error: arguments JSON required");
        }

        try {
            JsonNode node = MAPPER.readTree(jsonInput);
            String claimId = node.get("claimId").asText();
            String evidenceId = node.get("evidenceId").asText();
            String sourceId = node.get("sourceId").asText();
            String locator = node.has("locator") ? node.get("locator").asText() : "";
            String status = node.has("status") ? node.get("status").asText() : "valid";

            Citation citation = citationStore.create(claimId, evidenceId, sourceId, locator, status);
            return ToolResult.of(name(), "Citation submitted successfully. Assigned citationId: " + citation.getCitationId(),
                    java.util.Map.of("citationId", citation.getCitationId()));
        } catch (Exception e) {
            return ToolResult.of(name(), "Error executing submit_citation: " + e.getMessage());
        }
    }
}
