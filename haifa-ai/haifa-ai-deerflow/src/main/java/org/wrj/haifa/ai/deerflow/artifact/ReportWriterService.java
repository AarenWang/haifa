package org.wrj.haifa.ai.deerflow.artifact;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.plan.QualityGateResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;

@Service
public class ReportWriterService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final ArtifactService artifactService;

    public ReportWriterService(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    public ReportWriteResult writeReport(String threadId, String runId, ResearchPlan plan,
            List<ResearchSource> sources, List<EvidenceItem> evidenceItems,
            QualityGateResult qualityGate, String synthesisResult, ResearchOptions options) {
        String topic = plan == null || !StringUtils.hasText(plan.topic()) ? "research" : plan.topic();
        List<ResearchSource> safeSources = sources == null ? List.of() : sources;
        List<EvidenceItem> safeEvidence = evidenceItems == null ? List.of() : evidenceItems;
        String summary = executiveSummary(synthesisResult, safeEvidence, topic);
        String limitations = limitations(qualityGate);
        String markdown = buildMarkdown(topic, plan, safeSources, safeEvidence, qualityGate,
                synthesisResult, summary, limitations, options);
        Path path = writeMarkdownFile(topic, runId, markdown);
        ArtifactRecord artifact = artifactService.register(threadId, runId, path, "text/markdown");
        return new ReportWriteResult(artifact, markdown, summary, limitations);
    }

    private String buildMarkdown(String topic, ResearchPlan plan, List<ResearchSource> sources,
            List<EvidenceItem> evidenceItems, QualityGateResult qualityGate, String synthesisResult,
            String summary, String limitations, ResearchOptions options) {
        Map<String, ResearchSource> sourcesById = new LinkedHashMap<>();
        for (ResearchSource source : sources) {
            sourcesById.put(source.sourceId(), source);
        }
        Map<String, ResearchSource> citedSources = citedSources(evidenceItems, sourcesById);
        if (citedSources.isEmpty()) {
            sourcesById.values().stream()
                    .filter(ResearchSource::fetched)
                    .forEach(source -> citedSources.putIfAbsent(source.sourceId(), source));
        }

        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(escapeHeading(topic)).append("\n\n");
        builder.append("- **Research Date:** ").append(LocalDate.now(ZoneOffset.UTC)).append("\n");
        builder.append("- **Timestamp:** ").append(Instant.now()).append("\n");
        builder.append("- **Depth:** ").append(options == null ? "standard" : options.depth().name().toLowerCase(Locale.ROOT)).append("\n");
        builder.append("- **Time Window:** ").append(options == null ? "latest" : options.timeWindow().name().toLowerCase(Locale.ROOT)).append("\n\n");

        builder.append("## Executive Summary\n\n").append(summary).append("\n\n");
        builder.append("## Methodology\n\n");
        builder.append("- Generated a structured research plan before synthesis.\n");
        builder.append("- Collected and deduplicated sources under the active run.\n");
        builder.append("- Extracted evidence items and bound each cited claim back to a registered source.\n");
        builder.append("- Evaluated coverage with the research quality gate before artifact delivery.\n\n");

        builder.append("## Key Findings\n\n");
        if (evidenceItems.isEmpty()) {
            builder.append("- No structured evidence was extracted; see limitations.\n\n");
        } else {
            evidenceItems.stream().limit(8).forEach(evidence -> {
                ResearchSource source = sourcesById.get(evidence.sourceId());
                builder.append("- ").append(textOrFallback(evidence.claim(), evidence.quoteOrParaphrase()));
                if (source != null) {
                    builder.append(' ').append(inlineCitation(source));
                }
                builder.append('\n');
            });
            builder.append('\n');
        }

        builder.append("## Dimensions\n\n");
        if (plan == null || plan.dimensions().isEmpty()) {
            builder.append("- No structured dimensions were available.\n\n");
        } else {
            for (ResearchDimension dimension : plan.dimensions()) {
                builder.append("### ").append(escapeHeading(dimension.title())).append("\n\n");
                builder.append("- Status: `").append(dimension.status()).append("`\n");
                builder.append("- Sources: ").append(dimension.actualSourceCount()).append(" / ")
                        .append(dimension.expectedSourceCount()).append("\n");
                builder.append("- Evidence items: ").append(dimension.actualEvidenceCount()).append("\n");
                if (StringUtils.hasText(dimension.description())) {
                    builder.append("- Scope: ").append(dimension.description()).append("\n");
                }
                builder.append('\n');
            }
        }

        builder.append("## Evidence Table\n\n");
        builder.append("| Evidence | Dimension | Confidence | Source |\n");
        builder.append("| --- | --- | ---: | --- |\n");
        if (evidenceItems.isEmpty()) {
            builder.append("| No evidence extracted | general | 0.00 | n/a |\n");
        } else {
            for (EvidenceItem evidence : evidenceItems) {
                ResearchSource source = sourcesById.get(evidence.sourceId());
                builder.append("| ")
                        .append(tableCell(textOrFallback(evidence.claim(), evidence.quoteOrParaphrase())))
                        .append(" | ")
                        .append(tableCell(evidence.dimension()))
                        .append(" | ")
                        .append(String.format(Locale.ROOT, "%.2f", evidence.confidence()))
                        .append(" | ")
                        .append(source == null ? "n/a" : inlineCitation(source))
                        .append(" |\n");
            }
        }
        builder.append('\n');

        builder.append("## Limitations\n\n");
        builder.append(limitations).append("\n\n");

        builder.append("## Sources\n\n");
        if (citedSources.isEmpty()) {
            builder.append("- No registered source was cited in this report.\n");
        } else {
            citedSources.values().forEach(source -> builder.append("- [")
                    .append(StringUtils.hasText(source.title()) ? source.title() : source.domain())
                    .append("](").append(source.url()).append(")")
                    .append(" - ").append(source.domain())
                    .append('\n'));
        }

        if (StringUtils.hasText(synthesisResult)) {
            builder.append("\n## Synthesis Notes\n\n");
            builder.append(trimToParagraph(synthesisResult, 1_200)).append('\n');
        }
        return builder.toString().trim() + "\n";
    }

    private Path writeMarkdownFile(String topic, String runId, String markdown) {
        try {
            Path root = artifactService.outputsRoot();
            Files.createDirectories(root);
            String date = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMAT);
            Path target = root.resolve("research-" + slug(topic) + "-" + date + "-" + slug(runId) + ".md").normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("Report filename escaped outputsRoot");
            }
            Files.writeString(target, markdown, StandardCharsets.UTF_8);
            return target;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write research report", ex);
        }
    }

    private static Map<String, ResearchSource> citedSources(List<EvidenceItem> evidenceItems,
            Map<String, ResearchSource> sourcesById) {
        Map<String, ResearchSource> result = new LinkedHashMap<>();
        for (EvidenceItem evidence : evidenceItems) {
            ResearchSource source = sourcesById.get(evidence.sourceId());
            if (source != null) {
                result.putIfAbsent(source.sourceId(), source);
            }
        }
        return result;
    }

    private static String executiveSummary(String synthesisResult, List<EvidenceItem> evidenceItems, String topic) {
        if (StringUtils.hasText(synthesisResult)) {
            return trimToParagraph(stripSourcesSection(synthesisResult), 700);
        }
        if (!evidenceItems.isEmpty()) {
            return "This report summarizes the available research evidence for " + topic
                    + ". It is based on " + evidenceItems.size() + " extracted evidence items.";
        }
        return "This report records the research workflow for " + topic
                + ", but the run did not extract enough evidence for a confident synthesis.";
    }

    private static String limitations(QualityGateResult qualityGate) {
        if (qualityGate == null) {
            return "No quality gate result was available for this run.";
        }
        if (qualityGate.passed()) {
            return "The quality gate passed. Residual limitations may remain where source availability, recency, or opposing viewpoints are sparse.";
        }
        String gaps = qualityGate.gaps().isEmpty() ? "No specific gaps were captured." : String.join("; ", qualityGate.gaps());
        return "Quality gate did not pass. Gaps: " + gaps + ". Recommendation: " + qualityGate.recommendation();
    }

    private static String inlineCitation(ResearchSource source) {
        String title = StringUtils.hasText(source.title()) ? source.title() : source.domain();
        return "[citation:" + title.replace("]", "") + "](" + source.url() + ")";
    }

    private static String textOrFallback(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : (StringUtils.hasText(fallback) ? fallback : "Evidence item");
    }

    private static String trimToParagraph(String value, int maxChars) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replace("<final_answer>", "").replace("</final_answer>", "").trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars).trim() + "...";
    }

    private static String stripSourcesSection(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        int index = lower.indexOf("\nsources\n");
        if (index < 0) {
            index = lower.indexOf("\n## sources");
        }
        return index < 0 ? value : value.substring(0, index);
    }

    private static String tableCell(String value) {
        return Optional.ofNullable(value).orElse("").replace("|", "\\|").replace("\n", " ");
    }

    private static String escapeHeading(String value) {
        return Optional.ofNullable(value).orElse("Research Report").replace("\n", " ").trim();
    }

    private static String slug(String value) {
        String normalized = Normalizer.normalize(value == null ? "research" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "research" : normalized.substring(0, Math.min(normalized.length(), 60));
    }
}
