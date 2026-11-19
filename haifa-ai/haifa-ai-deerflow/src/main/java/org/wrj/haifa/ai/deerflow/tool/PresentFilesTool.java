package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactRecord;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;

@Component
public class PresentFilesTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ArtifactService artifactService;
    private final UserDataPathResolver pathResolver;

    @Autowired
    public PresentFilesTool(ArtifactService artifactService, UserDataPathResolver pathResolver) {
        this.artifactService = artifactService;
        this.pathResolver = pathResolver;
    }

    @Override
    public String name() {
        return "present_files";
    }

    @Override
    public String description() {
        return "Register and present completed output files to the user. Arguments: {\"filepaths\": [\"/mnt/user-data/outputs/file.png\"]}. The legacy files field also accepts artifact IDs or filenames. Files must be non-empty regular files under outputs.";
    }

    @Override
    public java.util.List<org.wrj.haifa.ai.deerflow.completion.ToolCompletionContract> completionContracts() {
        return java.util.List.of(new org.wrj.haifa.ai.deerflow.completion.ToolCompletionContract(
                org.wrj.haifa.ai.deerflow.completion.CompletionRequirementType.ARTIFACT_DELIVERY,
                org.wrj.haifa.ai.deerflow.completion.EvidenceType.ARTIFACT,
                "presented files"));
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("present_files");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
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
            JsonNode filesNode = node.has("filepaths") ? node.get("filepaths") : node.get("files");
            if (filesNode == null || !filesNode.isArray()) {
                return ToolResult.failed(name(), "Error: filepaths array is required");
            }
            if (!org.springframework.util.StringUtils.hasText(request.threadId())
                    || !org.springframework.util.StringUtils.hasText(request.runId())) {
                return ToolResult.failed(name(), "Error: threadId and runId are required to present files");
            }

            StringBuilder sb = new StringBuilder("Presented artifacts:\n");
            java.util.List<String> artifactIds = new java.util.ArrayList<>();
            for (JsonNode file : filesNode) {
                String requested = file.asText();
                java.util.Optional<ArtifactRecord> artifact = artifactService.findVisible(
                        request.threadId(), request.runId(), requested);
                if (artifact.isEmpty()) {
                    try {
                        java.nio.file.Path path = pathResolver.resolveReadable(requested, request.workspaceRoot());
                        if (!path.startsWith(pathResolver.outputsRoot())) {
                            return ToolResult.failed(name(), "Error: only files under outputs can be presented: " + requested);
                        }
                        artifact = java.util.Optional.of(artifactService.register(
                                request.threadId(), request.runId(), path, null));
                    } catch (IllegalArgumentException ex) {
                        return ToolResult.failed(name(), "Error: unable to present file " + requested + ": " + ex.getMessage());
                    }
                }
                if (artifact.isEmpty()) {
                    return ToolResult.failed(name(), "Error: file is not a registered artifact: " + requested);
                }
                ArtifactRecord record = artifact.get();
                artifactIds.add(record.artifactId());
                sb.append("- ")
                        .append(record.filename())
                        .append(" (artifactId=")
                        .append(record.artifactId())
                        .append(", download=/api/deerflow/artifacts/")
                        .append(record.artifactId())
                        .append("/download)\n");
            }
            if (artifactIds.isEmpty()) {
                return ToolResult.failed(name(), "Error: at least one file is required");
            }
            return ToolResult.success(name(), sb.toString().trim(), java.util.Map.of(
                    "artifacts", artifactIds,
                    "presentedArtifactIds", artifactIds,
                    "artifactDeliverySucceeded", true));
        } catch (Exception e) {
            return ToolResult.failed(name(), "Error: " + e.getMessage(),
                    java.util.Map.of("errorType", e.getClass().getSimpleName()));
        }
    }
}
