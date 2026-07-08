package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDataPathResolverTest {

    @Test
    void resolvesReadableVirtualRoots(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        UserDataPathResolver resolver = new UserDataPathResolver(properties);

        assertThat(resolver.resolveReadable("/mnt/user-data/uploads/input.csv", tmp.resolve("workspace")))
                .isEqualTo(tmp.resolve("uploads/input.csv").toAbsolutePath().normalize());
        assertThat(resolver.resolveReadable("/mnt/user-data/workspace/note.md", tmp.resolve("workspace")))
                .isEqualTo(tmp.resolve("workspace/note.md").toAbsolutePath().normalize());
        assertThat(resolver.resolveReadable("/mnt/user-data/outputs/report.md", tmp.resolve("workspace")))
                .isEqualTo(tmp.resolve("outputs/report.md").toAbsolutePath().normalize());
    }

    @Test
    void writableRootsExcludeUploads(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        UserDataPathResolver resolver = new UserDataPathResolver(properties);

        assertThat(resolver.resolveWritable("draft.md", tmp.resolve("workspace")))
                .isEqualTo(tmp.resolve("workspace/draft.md").toAbsolutePath().normalize());
        assertThat(resolver.resolveWritable("outputs/report.md", tmp.resolve("workspace")))
                .isEqualTo(tmp.resolve("outputs/report.md").toAbsolutePath().normalize());
        assertThatThrownBy(() -> resolver.resolveWritable("/mnt/user-data/uploads/new.txt", tmp.resolve("workspace")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uploads directory is written only by the frontend upload service");
    }

    @Test
    void mapsPhysicalOutputsToVirtualPath(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        UserDataPathResolver resolver = new UserDataPathResolver(properties);

        assertThat(resolver.toVirtualPath(tmp.resolve("outputs/reports/final.md")))
                .isEqualTo("/mnt/user-data/outputs/reports/final.md");
    }

    @Test
    void rewritesAndMasksVirtualPaths(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        UserDataPathResolver resolver = new UserDataPathResolver(properties);

        String text = "Read from /mnt/user-data/uploads/input.csv and write to /mnt/user-data/workspace/result.csv and /mnt/user-data/outputs/report.md";
        String localUploads = tmp.resolve("uploads").toAbsolutePath().normalize().toString().replace('\\', '/');
        String localWorkspace = tmp.resolve("workspace").toAbsolutePath().normalize().toString().replace('\\', '/');
        String localOutputs = tmp.resolve("outputs").toAbsolutePath().normalize().toString().replace('\\', '/');

        String rewritten = resolver.rewriteContainerPathsToLocal(text);
        assertThat(rewritten).contains(localUploads + "/input.csv");
        assertThat(rewritten).contains(localWorkspace + "/result.csv");
        assertThat(rewritten).contains(localOutputs + "/report.md");

        String masked = resolver.maskLocalPathsToContainer(rewritten);
        assertThat(masked).isEqualTo(text);
    }

    private static DeerFlowProperties properties(Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(tmp.resolve("workspace").toString());
        properties.setUploadsRoot(tmp.resolve("uploads").toString());
        properties.setOutputsRoot(tmp.resolve("outputs").toString());
        return properties;
    }
}