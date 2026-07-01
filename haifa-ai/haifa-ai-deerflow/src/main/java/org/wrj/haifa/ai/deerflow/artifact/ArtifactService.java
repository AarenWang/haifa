package org.wrj.haifa.ai.deerflow.artifact;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Service
public class ArtifactService {

    private final DeerFlowProperties properties;
    private final Map<String, ArtifactRecord> artifactsById = new ConcurrentHashMap<>();

    public ArtifactService(DeerFlowProperties properties) {
        this.properties = properties;
    }

    public ArtifactRecord register(String threadId, String runId, Path file, String mimeType) {
        if (!StringUtils.hasText(threadId)) {
            throw new IllegalArgumentException("threadId is required");
        }
        if (!StringUtils.hasText(runId)) {
            throw new IllegalArgumentException("runId is required");
        }
        Path resolved = validateRegisteredPath(file);
        try {
            ArtifactRecord record = new ArtifactRecord(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    resolved.toString(),
                    resolved.getFileName().toString(),
                    StringUtils.hasText(mimeType) ? mimeType : detectMimeType(resolved),
                    Files.size(resolved),
                    Instant.now()
            );
            artifactsById.put(record.artifactId(), record);
            return record;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to register artifact: " + file, ex);
        }
    }

    public List<ArtifactRecord> list(String threadId, String runId) {
        return artifactsById.values().stream()
                .filter(record -> !StringUtils.hasText(threadId) || threadId.equals(record.threadId()))
                .filter(record -> !StringUtils.hasText(runId) || runId.equals(record.runId()))
                .sorted(Comparator.comparing(ArtifactRecord::createdAt).reversed())
                .toList();
    }

    public Optional<ArtifactRecord> find(String artifactId) {
        return Optional.ofNullable(artifactsById.get(artifactId));
    }

    public Optional<ArtifactRecord> findVisible(String threadId, String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        ArtifactRecord byId = artifactsById.get(token);
        if (byId != null && matchesThread(threadId, byId)) {
            return Optional.of(byId);
        }
        String safeToken = Path.of(token).getFileName().toString();
        return artifactsById.values().stream()
                .filter(record -> matchesThread(threadId, record))
                .filter(record -> record.filename().equals(token) || record.filename().equals(safeToken))
                .findFirst();
    }

    public Path resolveForDownload(String artifactId) {
        ArtifactRecord record = find(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));
        Path path = Path.of(record.path()).toAbsolutePath().normalize();
        if (!path.startsWith(outputsRoot()) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Artifact path is no longer valid");
        }
        return path;
    }

    public String readMarkdownPreview(String artifactId, int maxChars) {
        ArtifactRecord record = find(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));
        if (!record.mimeType().equals("text/markdown") && !record.filename().endsWith(".md")) {
            return "";
        }
        try {
            String content = Files.readString(resolveForDownload(artifactId), StandardCharsets.UTF_8);
            if (maxChars > 0 && content.length() > maxChars) {
                return content.substring(0, maxChars) + "\n\n...[truncated]";
            }
            return content;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read artifact: " + artifactId, ex);
        }
    }

    public Path outputsRoot() {
        return Path.of(properties.getOutputsRoot()).toAbsolutePath().normalize();
    }

    private Path validateRegisteredPath(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("Artifact path is required");
        }
        Path root = outputsRoot();
        Path resolved = file.toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Only files under outputsRoot can be registered as artifacts");
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("Artifact file does not exist: " + file);
        }
        return resolved;
    }

    private static boolean matchesThread(String threadId, ArtifactRecord record) {
        return !StringUtils.hasText(threadId) || threadId.equals(record.threadId());
    }

    private static String detectMimeType(Path path) {
        try {
            String detected = Files.probeContentType(path);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ignored) {
            // fall through
        }
        return path.getFileName().toString().endsWith(".md") ? "text/markdown" : "application/octet-stream";
    }
}
