package org.wrj.haifa.ai.deerflow.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactRecord;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

class ArtifactControllerTest {

    @Test
    void downloadsRegisteredArtifact(@TempDir Path tempDir) throws Exception {
        Path outputs = tempDir.resolve("outputs");
        Files.createDirectories(outputs);
        Path report = outputs.resolve("report.md");
        Files.writeString(report, "# Report");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(outputs.toString());
        ArtifactService service = new ArtifactService(properties);
        ArtifactRecord record = service.register("thread-1", "run-1", report, "text/markdown");
        ArtifactController controller = new ArtifactController(service);

        assertThat(controller.list("thread-1", "run-1").block()).hasSize(1);
        ArtifactResponse detail = controller.get(record.artifactId()).block();
        assertThat(detail.preview()).contains("# Report");

        ResponseEntity<Resource> response = controller.download(record.artifactId()).block();
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("report.md");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().exists()).isTrue();
    }
}
