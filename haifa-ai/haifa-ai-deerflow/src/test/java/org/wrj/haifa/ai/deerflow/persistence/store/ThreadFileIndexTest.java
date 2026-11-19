package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.threadfile.ThreadFile;
import org.wrj.haifa.ai.deerflow.threadfile.ThreadFileStore;

@SpringBootTest
@ActiveProfiles("test")
class ThreadFileIndexTest {

    @Autowired
    private ThreadFileStore threadFileStore;

    @Test
    void testThreadFileIndexing() {
        String threadId = "thread-file-1";
        String runId = "run-file-1";
        String path = "/workspace/report.md";

        ThreadFile tf = threadFileStore.register(
                threadId,
                runId,
                "art-file-1",
                path,
                "/workspace/report-edit.md",
                "report",
                "generated",
                1,
                List.of("wi-file-1"),
                List.of("ev-file-1")
        );
        assertThat(tf.getFileId()).isNotNull();
        assertThat(tf.getArtifactId()).isEqualTo("art-file-1");
        assertThat(tf.getPath()).isEqualTo(path);
        assertThat(tf.getEditablePath()).isEqualTo("/workspace/report-edit.md");
        assertThat(tf.getGeneratedFromWorkItemIds()).contains("wi-file-1");
        assertThat(tf.getGeneratedFromEvidenceIds()).contains("ev-file-1");

        List<ThreadFile> list = threadFileStore.findByThreadId(threadId);
        assertThat(list).hasSize(1);

        list = threadFileStore.findByRunId(runId);
        assertThat(list).hasSize(1);
    }
}
