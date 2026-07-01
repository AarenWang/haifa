package org.wrj.haifa.ai.deerflow.artifact;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.ResearchSourceType;
import org.wrj.haifa.ai.deerflow.research.plan.QualityGateResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;

class ReportWriterServiceTest {

    @Test
    void writesMarkdownReportAndRegistersMatchingArtifact(@TempDir Path tempDir) throws Exception {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(tempDir.resolve("outputs").toString());
        ArtifactService artifactService = new ArtifactService(properties);
        ReportWriterService writer = new ReportWriterService(artifactService);

        ResearchPlan plan = new ResearchPlan(
                "plan-1",
                "thread-1",
                "run-1",
                "AI chip market",
                List.of("What is changing?"),
                List.of(new ResearchDimension("d1", "Data", "Market size", ResearchTaskStatus.COMPLETED,
                        List.of("ai chip market data"), 1, 1, 1, List.of("e1"))),
                List.of("ai chip market data"),
                "authoritative",
                "report",
                "COMPLETED",
                null,
                null
        );
        ResearchSource source = new ResearchSource(
                "s1", "thread-1", "run-1", "Market Data", "https://example.com/market",
                "https://example.com/market", "example.com", null, Instant.now(),
                ResearchSourceType.WEB_PAGE, 0.9, "snippet", "hash");
        EvidenceItem evidence = new EvidenceItem(
                "e1", "thread-1", "run-1", "s1", "quote", "Market revenue grew", "data", 0.9, Instant.now());

        ReportWriteResult result = writer.writeReport(
                "thread-1",
                "run-1",
                plan,
                List.of(source),
                List.of(evidence),
                QualityGateResult.passed(95.0, 3, 5),
                "Revenue grew because demand increased [citation:Market Data](https://example.com/market)",
                ResearchOptions.standard()
        );

        assertThat(Files.exists(Path.of(result.artifact().path()))).isTrue();
        assertThat(result.artifact().size()).isEqualTo(Files.size(Path.of(result.artifact().path())));
        assertThat(result.artifact().threadId()).isEqualTo("thread-1");
        assertThat(result.artifact().runId()).isEqualTo("run-1");
        assertThat(result.markdown()).contains("## Executive Summary", "## Evidence Table", "## Sources");
        assertThat(result.markdown()).contains("[citation:Market Data](https://example.com/market)");
        assertThat(result.markdown()).contains("- [Market Data](https://example.com/market)");
        assertThat(artifactService.list("thread-1", "run-1")).hasSize(1);
    }

    @Test
    void sourcesSectionOnlyContainsSourcesUsedByInlineEvidenceCitations(@TempDir Path tempDir) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(tempDir.resolve("outputs").toString());
        ReportWriterService writer = new ReportWriterService(new ArtifactService(properties));

        ResearchSource cited = new ResearchSource(
                "s1", "thread-1", "run-1", "Cited", "https://example.com/cited",
                "https://example.com/cited", "example.com", null, Instant.now(),
                ResearchSourceType.WEB_PAGE, 0.9, "", "hash1");
        ResearchSource unused = new ResearchSource(
                "s2", "thread-1", "run-1", "Unused", "https://example.com/unused",
                "https://example.com/unused", "example.com", null, Instant.now(),
                ResearchSourceType.WEB_PAGE, 0.9, "", "hash2");
        EvidenceItem evidence = new EvidenceItem(
                "e1", "thread-1", "run-1", "s1", "quote", "Cited claim", "facts", 0.9, Instant.now());

        ReportWriteResult result = writer.writeReport(
                "thread-1",
                "run-1",
                null,
                List.of(cited, unused),
                List.of(evidence),
                QualityGateResult.passed(90, 3, 5),
                "Synthesis",
                ResearchOptions.standard()
        );

        assertThat(result.markdown()).contains("[citation:Cited](https://example.com/cited)");
        assertThat(result.markdown()).contains("- [Cited](https://example.com/cited)");
        assertThat(result.markdown()).doesNotContain("https://example.com/unused");
    }

    @Test
    void repeatedTopicRunsWriteDistinctReportFiles(@TempDir Path tempDir) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(tempDir.resolve("outputs").toString());
        ArtifactService artifactService = new ArtifactService(properties);
        ReportWriterService writer = new ReportWriterService(artifactService);

        ReportWriteResult first = writer.writeReport(
                "thread-1",
                "run-1",
                null,
                List.of(),
                List.of(),
                null,
                "First synthesis",
                ResearchOptions.standard()
        );
        ReportWriteResult second = writer.writeReport(
                "thread-1",
                "run-2",
                null,
                List.of(),
                List.of(),
                null,
                "Second synthesis",
                ResearchOptions.standard()
        );

        assertThat(first.artifact().path()).isNotEqualTo(second.artifact().path());
        assertThat(Files.exists(Path.of(first.artifact().path()))).isTrue();
        assertThat(Files.exists(Path.of(second.artifact().path()))).isTrue();
        assertThat(artifactService.list("thread-1", null)).hasSize(2);
    }
}
