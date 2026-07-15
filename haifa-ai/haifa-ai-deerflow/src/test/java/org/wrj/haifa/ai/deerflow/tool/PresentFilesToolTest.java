package org.wrj.haifa.ai.deerflow.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactRecord;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

class PresentFilesToolTest {

    @Test
    void registersAndPresentsExistingOutputPaths(@TempDir Path tempDir) throws Exception {
        Path outputs = tempDir.resolve("outputs");
        Files.createDirectories(outputs);
        Path report = outputs.resolve("report.md");
        Files.writeString(report, "# Report");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(outputs.toString());
        ArtifactService service = new ArtifactService(properties);
        UserDataPathResolver resolver = new UserDataPathResolver(properties);
        PresentFilesTool tool = new PresentFilesTool(service, resolver);

        ToolResult ok = tool.execute(new ToolRequest(
                "{\"filepaths\":[\"/mnt/user-data/outputs/report.md\"]}",
                tempDir,
                java.util.List.of(),
                "thread-1", "run-1"));
        ToolResult bad = tool.execute(new ToolRequest(
                "{\"filepaths\":[\"/mnt/user-data/outputs/missing.md\"]}",
                tempDir,
                java.util.List.of(),
                "thread-1", "run-1"));

        ArtifactRecord record = service.list("thread-1", "run-1").get(0);
        assertThat(ok.status()).isEqualTo(ToolResult.Status.SUCCESS);
        assertThat(ok.content()).contains(record.artifactId(), "/api/deerflow/artifacts/");
        assertThat(ok.metadata()).containsEntry("artifactDeliverySucceeded", true);
        assertThat(bad.status()).isEqualTo(ToolResult.Status.FAILED);
        assertThat(bad.content()).contains("unable to present file");
    }
}
