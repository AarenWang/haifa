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
    void presentsOnlyRegisteredArtifacts(@TempDir Path tempDir) throws Exception {
        Path outputs = tempDir.resolve("outputs");
        Files.createDirectories(outputs);
        Path report = outputs.resolve("report.md");
        Files.writeString(report, "# Report");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(outputs.toString());
        ArtifactService service = new ArtifactService(properties);
        ArtifactRecord record = service.register("thread-1", "run-1", report, "text/markdown");
        PresentFilesTool tool = new PresentFilesTool(service);

        ToolResult ok = tool.execute(new ToolRequest(
                "{\"files\":[\"" + record.artifactId() + "\"]}",
                tempDir,
                java.util.List.of(),
                "thread-1"));
        ToolResult bad = tool.execute(new ToolRequest(
                "{\"files\":[\"../secrets.md\"]}",
                tempDir,
                java.util.List.of(),
                "thread-1"));

        assertThat(ok.content()).contains(record.artifactId(), "/api/deerflow/artifacts/");
        assertThat(bad.content()).contains("not a registered artifact");
    }
}
