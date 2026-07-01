package org.wrj.haifa.ai.deerflow.artifact;

public record ReportWriteResult(
        ArtifactRecord artifact,
        String markdown,
        String summary,
        String limitations
) {
}
