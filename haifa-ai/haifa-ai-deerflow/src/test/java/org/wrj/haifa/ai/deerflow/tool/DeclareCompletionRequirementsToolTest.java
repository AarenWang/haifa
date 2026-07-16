package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeclareCompletionRequirementsToolTest {

    private final DeclareCompletionRequirementsTool tool = new DeclareCompletionRequirementsTool();

    @Test
    void returnsValidatedRequirementsAsTrustedMetadata() {
        ToolResult result = tool.execute(new ToolRequest("""
                {"requirements":[
                  {"type":"LOCAL_OBSERVATION","subject":"battery status"},
                  {"type":"ARTIFACT_DELIVERY","subject":"chart"}
                ]}
                """, Path.of(".")));

        assertThat(result.status()).isEqualTo(ToolResult.Status.SUCCESS);
        assertThat((List<Map<String, Object>>) result.metadata().get("declaredCompletionRequirements"))
                .extracting(requirement -> requirement.get("type"))
                .containsExactly("LOCAL_OBSERVATION", "ARTIFACT_DELIVERY");
    }

    @Test
    void rejectsUnknownRequirementType() {
        ToolResult result = tool.execute(new ToolRequest(
                "{\"requirements\":[{\"type\":\"MADE_UP\"}]}", Path.of(".")));

        assertThat(result.status()).isEqualTo(ToolResult.Status.FAILED);
        assertThat(result.metadata()).doesNotContainKey("declaredCompletionRequirements");
    }
}
