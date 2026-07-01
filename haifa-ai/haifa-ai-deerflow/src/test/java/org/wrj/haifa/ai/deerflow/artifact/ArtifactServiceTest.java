package org.wrj.haifa.ai.deerflow.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

class ArtifactServiceTest {

    @Test
    void registersOnlyFilesUnderOutputsRoot(@TempDir Path tempDir) throws Exception {
        Path outputs = tempDir.resolve("outputs");
        Path outside = tempDir.resolve("outside.md");
        Files.createDirectories(outputs);
        Path report = outputs.resolve("report.md");
        Files.writeString(report, "# Report");
        Files.writeString(outside, "# Outside");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(outputs.toString());
        ArtifactService service = new ArtifactService(properties);

        ArtifactRecord record = service.register("thread-1", "run-1", report, "text/markdown");

        assertThat(record.filename()).isEqualTo("report.md");
        assertThat(service.resolveForDownload(record.artifactId())).isEqualTo(report.toAbsolutePath().normalize());
        assertThatThrownBy(() -> service.register("thread-1", "run-1", outside, "text/markdown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outputsRoot");
    }
}
