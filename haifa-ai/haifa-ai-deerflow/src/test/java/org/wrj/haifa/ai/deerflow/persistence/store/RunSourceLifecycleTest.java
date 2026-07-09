package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.source.Source;
import org.wrj.haifa.ai.deerflow.source.SourceStore;

@SpringBootTest
@ActiveProfiles("test")
class RunSourceLifecycleTest {

    @Autowired
    private SourceStore sourceStore;

    @Test
    void testSourceLifecycle() {
        String runId = "run-src-1";
        String threadId = "thread-src-1";
        String url = "https://example.com/item";

        Source source = sourceStore.discover(runId, threadId, url, "Example Title", "example.com");
        assertThat(source.getSourceId()).isNotNull();
        assertThat(source.getLifecycleStatus()).isEqualTo("discovered");

        sourceStore.updateFetched(source.getSourceId(), url, "Updated Title", "web_page", "high", "hash123", "{}");

        List<Source> list = sourceStore.findByRunId(runId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getLifecycleStatus()).isEqualTo("fetched");
        assertThat(list.get(0).getContentHash()).isEqualTo("hash123");
        assertThat(list.get(0).getFetchedAt()).isNotNull();

        sourceStore.updateStatus(source.getSourceId(), "accepted", null);
        Source accepted = sourceStore.findByUrlAndThreadId(url, threadId).orElse(null);
        assertThat(accepted).isNotNull();
        assertThat(accepted.getLifecycleStatus()).isEqualTo("accepted");
    }
}
