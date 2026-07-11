package org.wrj.haifa.ai.deerflow.artifact;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class ArtifactService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final DeerFlowProperties properties;
    private final Map<String, ArtifactRecord> artifactsById = new ConcurrentHashMap<>();

    public ArtifactService(DeerFlowProperties properties) {
        this.properties = properties;
        loadArtifacts();
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
            String resolvedMimeType = StringUtils.hasText(mimeType) ? mimeType : detectMimeType(resolved);
            validateBinarySignature(resolved, resolvedMimeType);
            ArtifactRecord record = new ArtifactRecord(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    resolved.toString(),
                    resolved.getFileName().toString(),
                    resolvedMimeType,
                    Files.size(resolved),
                    Instant.now()
            );
            artifactsById.put(record.artifactId(), record);
            saveArtifacts();
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
        validateBinarySignature(path, record.mimeType());
        return path;
    }

    public ArtifactPreview readTextPreview(String artifactId, int maxChars) {
        ArtifactRecord record = find(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));
        if (!isSourceViewable(record)) {
            return new ArtifactPreview("", false);
        }
        try {
            String content = Files.readString(resolveForDownload(artifactId), StandardCharsets.UTF_8);
            if (maxChars > 0 && content.length() > maxChars) {
                return new ArtifactPreview(content.substring(0, maxChars), true);
            }
            return new ArtifactPreview(content, false);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read artifact: " + artifactId, ex);
        }
    }

    public String readMarkdownPreview(String artifactId, int maxChars) {
        ArtifactRecord record = find(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));
        if (!isMarkdown(record)) {
            return "";
        }
        ArtifactPreview preview = readTextPreview(artifactId, maxChars);
        if (preview.truncated()) {
            return preview.content() + "\n\n...[truncated]";
        }
        return preview.content();
    }

    public boolean isSourceViewable(ArtifactRecord record) {
        String mimeType = normalizeMime(record.mimeType());
        String extension = extension(record.filename());
        return mimeType.startsWith("text/")
                || mimeType.equals("application/json")
                || mimeType.equals("application/xml")
                || mimeType.equals("application/javascript")
                || mimeType.equals("image/svg+xml")
                || switch (extension) {
                    case "md", "markdown", "txt", "log", "json", "csv", "tsv", "html", "htm",
                            "svg", "xml", "css", "js", "jsx", "ts", "tsx", "yaml", "yml" -> true;
                    default -> false;
                };
    }

    public boolean isRenderable(ArtifactRecord record) {
        String mimeType = normalizeMime(record.mimeType());
        String extension = extension(record.filename());
        return isMarkdown(record)
                || mimeType.equals("text/html")
                || mimeType.equals("image/svg+xml")
                || mimeType.equals("image/png")
                || mimeType.equals("image/jpeg")
                || mimeType.equals("image/gif")
                || mimeType.equals("image/webp")
                || mimeType.equals("application/pdf")
                || switch (extension) {
                    case "html", "htm", "svg", "png", "jpg", "jpeg", "gif", "webp", "pdf" -> true;
                    default -> false;
                };
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

    private static boolean isMarkdown(ArtifactRecord record) {
        String mimeType = normalizeMime(record.mimeType());
        String extension = extension(record.filename());
        return mimeType.equals("text/markdown") || extension.equals("md") || extension.equals("markdown");
    }

    private static String detectMimeType(Path path) {
        String fallback = detectMimeTypeByExtension(path.getFileName().toString());
        if (!fallback.equals("application/octet-stream")) {
            return fallback;
        }
        try {
            String detected = Files.probeContentType(path);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ignored) {
            // fall through
        }
        return fallback;
    }

    private static String detectMimeTypeByExtension(String filename) {
        return switch (extension(filename)) {
            case "html", "htm" -> "text/html";
            case "svg" -> "image/svg+xml";
            case "md", "markdown" -> "text/markdown";
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            case "tsv" -> "text/tab-separated-values";
            case "txt", "log" -> "text/plain";
            case "xml" -> "application/xml";
            case "css" -> "text/css";
            case "js", "mjs" -> "application/javascript";
            case "ts", "tsx" -> "text/typescript";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private static String extension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0 || dot == lower.length() - 1) {
            return "";
        }
        return lower.substring(dot + 1);
    }

    private static String normalizeMime(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return "application/octet-stream";
        }
        int semicolon = mimeType.indexOf(';');
        String normalized = semicolon >= 0 ? mimeType.substring(0, semicolon) : mimeType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateBinarySignature(Path path, String mimeType) {
        String normalizedMime = normalizeMime(mimeType);
        String fileExtension = extension(path.getFileName().toString());
        String signatureType = switch (fileExtension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> normalizedMime;
        };
        try {
            byte[] header = readHeader(path, 12);
            boolean valid = switch (signatureType) {
                case "application/pdf" -> startsWith(header, "%PDF-".getBytes(StandardCharsets.US_ASCII));
                case "image/png" -> startsWith(header,
                        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
                case "image/jpeg" -> header.length >= 3
                        && header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff;
                case "image/gif" -> startsWith(header, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                        || startsWith(header, "GIF89a".getBytes(StandardCharsets.US_ASCII));
                case "image/webp" -> header.length >= 12
                        && startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> true;
            };
            if (!valid) {
                throw new IllegalArgumentException("Artifact content does not match its declared binary format: "
                        + path.getFileName());
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to validate artifact content: " + path, ex);
        }
    }

    private static byte[] readHeader(Path path, int maxBytes) throws IOException {
        try (java.io.InputStream input = Files.newInputStream(path)) {
            return input.readNBytes(maxBytes);
        }
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private synchronized void loadArtifacts() {
        if (isTestContext()) {
            return;
        }
        Path path = getArtifactsJsonPath();
        if (Files.exists(path)) {
            try {
                String content = Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
                Map<String, ArtifactRecord> loaded = MAPPER.readValue(content, new TypeReference<Map<String, ArtifactRecord>>() {});
                if (loaded != null) {
                    artifactsById.putAll(loaded);
                }
            } catch (Exception e) {
                // Ignore or log
            }
        }
    }

    private synchronized void saveArtifacts() {
        if (isTestContext()) {
            return;
        }
        Path path = getArtifactsJsonPath();
        try {
            Files.createDirectories(path.getParent());
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(artifactsById);
            Files.writeString(path, json, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Ignore or log
        }
    }

    private static boolean isTestContext() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.") || element.getClassName().startsWith("org.testng.")) {
                return true;
            }
        }
        return false;
    }

    private Path getArtifactsJsonPath() {
        String rootStr = properties.getUserDataRoot();
        if (rootStr != null && rootStr.contains("${user.dir}")) {
            rootStr = rootStr.replace("${user.dir}", System.getProperty("user.dir"));
        }
        return Path.of(rootStr != null ? rootStr : "data/user-data").resolve("artifacts.json");
    }
}
