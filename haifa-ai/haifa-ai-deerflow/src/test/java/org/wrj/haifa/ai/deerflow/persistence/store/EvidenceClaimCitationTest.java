package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.claim.Claim;
import org.wrj.haifa.ai.deerflow.claim.ClaimStore;
import org.wrj.haifa.ai.deerflow.claim.Citation;
import org.wrj.haifa.ai.deerflow.claim.CitationStore;
import org.wrj.haifa.ai.deerflow.evidence.EvidenceItem;
import org.wrj.haifa.ai.deerflow.evidence.EvidenceItemStore;

@SpringBootTest
@ActiveProfiles("test")
class EvidenceClaimCitationTest {

    @Autowired
    @Qualifier("EvidenceItemStoreV2")
    private EvidenceItemStore evidenceItemStore;

    @Autowired
    private ClaimStore claimStore;

    @Autowired
    private CitationStore citationStore;

    @Test
    void testEvidenceClaimCitationTraceability() {
        String runId = "run-trace-1";
        String threadId = "thread-trace-1";
        String sourceId = "src-trace-1";
        String workItemId = "wi-trace-1";

        EvidenceItem ev = evidenceItemStore.create(sourceId, runId, threadId, workItemId, "Summary of quote", "Raw supporting quote", "p1", 0.95);
        assertThat(ev.getEvidenceId()).isNotNull();

        Claim claim = claimStore.create(runId, threadId, null, "Agent assertion text", List.of(ev.getEvidenceId()), 0.9, "supported");
        assertThat(claim.getClaimId()).isNotNull();
        assertThat(claim.getSupportEvidenceIds()).contains(ev.getEvidenceId());

        Citation citation = citationStore.create(claim.getClaimId(), ev.getEvidenceId(), sourceId, "p1", "valid");
        assertThat(citation.getCitationId()).isNotNull();

        List<Citation> citations = citationStore.findByClaimId(claim.getClaimId());
        assertThat(citations).hasSize(1);
        assertThat(citations.get(0).getSourceId()).isEqualTo(sourceId);
    }
}
