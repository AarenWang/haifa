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

    @Test
    void rejectsTextMasqueradingAsPdf(@TempDir Path tempDir) throws Exception {
        Path outputs = tempDir.resolve("outputs");
        Files.createDirectories(outputs);
        Path fakePdf = outputs.resolve("report.pdf");
        Files.writeString(fakePdf, "PDF generation is unavailable");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(outputs.toString());
        ArtifactService service = new ArtifactService(properties);

        assertThatThrownBy(() -> service.register("thread-1", "run-1", fakePdf, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match", "report.pdf");
    }

    @Test
    void acceptsPdfWithValidSignature(@TempDir Path tempDir) throws Exception {
        Path outputs = tempDir.resolve("outputs");
        Files.createDirectories(outputs);
        Path pdf = outputs.resolve("report.pdf");
        Files.write(pdf, "%PDF-1.4\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(outputs.toString());
        ArtifactService service = new ArtifactService(properties);

        ArtifactRecord record = service.register("thread-1", "run-1", pdf, null);
        assertThat(record.mimeType()).isEqualTo("application/pdf");
    }
}
