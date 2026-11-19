package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.claim.Claim;
import org.wrj.haifa.ai.deerflow.claim.ClaimStore;

@Component
public class SubmitClaimTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private ClaimStore claimStore;

    @Override
    public String name() {
        return "submit_claim";
    }

    @Override
    public String description() {
        return "Submit a research assertion or claim with supporting evidence IDs. Arguments: {\"text\": \"...\", \"evidenceIds\": [\"id1\", \"id2\"], \"confidence\": 0.9, \"status\": \"supported\", \"artifactId\": \"...\"}";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "text": { "type": "string" },
                    "evidenceIds": {
                      "type": "array",
                      "items": { "type": "string" }
                    },
                    "confidence": { "type": "number" },
                    "status": { "type": "string" },
                    "artifactId": { "type": "string" }
                  },
                  "required": ["text", "evidenceIds"],
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("submit_claim");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String jsonInput = request.userMessage();
        if (jsonInput == null || jsonInput.isBlank()) {
            return ToolResult.of(name(), "Error: arguments JSON required");
        }

        try {
            JsonNode node = MAPPER.readTree(jsonInput);
            String text = node.get("text").asText();
            List<String> evidenceIds = MAPPER.readValue(node.get("evidenceIds").toString(), new TypeReference<List<String>>() {});
            Double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 1.0;
            String status = node.has("status") ? node.get("status").asText() : "supported";
            String artifactId = node.has("artifactId") ? node.get("artifactId").asText() : null;

            Claim claim = claimStore.create(request.runId(), request.threadId(), artifactId, text, evidenceIds, confidence, status);
            return ToolResult.of(name(), "Claim submitted successfully. Assigned claimId: " + claim.getClaimId(),
                    java.util.Map.of("claimId", claim.getClaimId()));
        } catch (Exception e) {
            return ToolResult.of(name(), "Error executing submit_claim: " + e.getMessage());
        }
    }
}
