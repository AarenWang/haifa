package org.wrj.haifa.ai.deerflow.middleware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.research.ResearchEvidenceCompressor;
import org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver;

/**
 * Tool output budget middleware inspired by Python deer-flow ToolOutputBudgetMiddleware.
 * <p>
 * Enforces a per-result budget on tool outputs. When a tool output exceeds the configured
 * threshold, it is either:
 * <ul>
 *   <li>Compressed by LLM ({@link ResearchEvidenceCompressor}) for web_fetch content</li>
 *   <li>Externalized to disk and replaced with a compact preview (head + tail + file reference)</li>
 *   <li>Fallback to head+tail truncation when disk persistence is unavailable</li>
 * </ul>
 *
 * <p>Key differences from Python deer-flow:
 * <ul>
 *   <li>Java version has no LangGraph AgentState / Runtime; it operates as a utility called by
 *       {@link org.wrj.haifa.ai.deerflow.agent.loop.AgentLoopObserver}.</li>
 *   <li>Java version uses LLM compression (ResearchEvidenceCompressor) for web_fetch before
 *       falling back to truncation, whereas Python version relies purely on externalization
 *       + truncation.</li>
 *   <li>Java version has no Sandbox concept; externalization writes to the host filesystem
 *       under {@code outputsRoot}.</li>
 *   <li>No tiktoken available; character counts are used as a proxy for token budgets.</li>
 * </ul>
 */
@Component
public class ToolOutputBudgetMiddleware {

    private static final Logger log = LoggerFactory.getLogger(ToolOutputBudgetMiddleware.class);


    private final DeerFlowProperties properties;
    private final ResearchEvidenceCompressor evidenceCompressor;

    public ToolOutputBudgetMiddleware(DeerFlowProperties properties, ResearchEvidenceCompressor evidenceCompressor) {
        this.properties = properties;
        this.evidenceCompressor = evidenceCompressor;
    }

    /**
     * Apply budget to a single tool output. Called by observer after tool execution.
     *
     * @param toolName   the tool name
     * @param output     the raw tool output
     * @param threadId   current thread id
     * @param runId      current run id
     * @param url        optional url (for web_fetch)
     * @param sourceId   optional source id (for web_fetch)
     * @param title      optional title (for web_fetch)
     * @return the processed output (may be compressed, externalized, or truncated)
     */
    public String processToolOutput(String toolName, String output, String threadId, String runId, String url, String sourceId, String title) {
        long startedAt = System.nanoTime();
        log.info("layer=middleware phase=enter middleware=ToolOutputBudgetMiddleware method=processToolOutput runId={} threadId={} toolName={} outputChars={}",
                runId, threadId, toolName, output == null ? 0 : output.length());
        try {
            return processToolOutputInternal(toolName, output, threadId, runId, url, sourceId, title);
        } finally {
            log.info("layer=middleware phase=exit middleware=ToolOutputBudgetMiddleware method=processToolOutput durationMs={} runId={} threadId={} toolName={}",
                    (System.nanoTime() - startedAt) / 1_000_000, runId, threadId, toolName);
        }
    }

    private String processToolOutputInternal(String toolName, String output, String threadId, String runId,
            String url, String sourceId, String title) {
        if (output == null) {
            return "";
        }

        DeerFlowProperties.ToolOutputBudget config = properties.getToolOutputBudget();
        if (!config.isEnabled()) {
            return output;
        }

        // 1. Skip exempt tools (mirror Python exempt_tools)
        if (isExempt(toolName, config)) {
            return output;
        }

        // 2. Fast path: already within fallback budget
        int fallbackMax = config.getFallbackMaxChars() > 0 ? config.getFallbackMaxChars() : 8_000;
        int externalizeMin = config.getExternalizeMinChars() > 0 ? config.getExternalizeMinChars() : 10_000;
        if (output.length() <= Math.min(fallbackMax, externalizeMin)) {
            return output;
        }

        log.info("layer=middleware Tool output budget exceeded for tool: {}. length={}, externalizeMin={}, fallbackMax={}",
                toolName, output.length(), externalizeMin, fallbackMax);

        // 3. For web_fetch, try LLM compression first (Java existing path, retained)
        String processed = output;
        if ("web_fetch".equalsIgnoreCase(toolName) && evidenceCompressor != null) {
            try {
                processed = evidenceCompressor.compressFetchContent(title, url, sourceId, output);
                log.info("layer=middleware LLM compressed web_fetch output from {} to {} chars", output.length(), processed.length());
            } catch (Exception e) {
                log.warn("layer=middleware LLM compression failed for web_fetch, falling back to truncation. Error: {}", e.getMessage());
            }
        }

        // 4. If still over externalize threshold, try disk externalization (Python _externalize)
        if (config.isExternalizeEnabled() && processed.length() > externalizeMin) {
            String virtualPath = externalizeToDisk(processed, toolName, threadId, runId, config);
            if (virtualPath != null) {
                String preview = buildPreview(processed, toolName, virtualPath, config.getPreviewHeadChars(), config.getPreviewTailChars());
                log.info("layer=middleware Externalized {} output ({} chars) to {}. Preview length={}",
                        toolName, processed.length(), virtualPath, preview.length());
                return preview;
            }
            log.warn("layer=middleware Disk externalization failed for {}. Falling back to truncation.", toolName);
        }

        // 5. Fallback head+tail truncation (Python _build_fallback)
        if (processed.length() > fallbackMax) {
            String truncated = buildFallback(processed, toolName, fallbackMax, config.getFallbackHeadChars(), config.getFallbackTailChars());
            log.warn("layer=middleware Fallback-truncated {} output: {} chars -> {} chars", toolName, processed.length(), truncated.length());
            return truncated;
        }
        return processed;
    }

    /**
     * Check whether a history entry has already been budget-processed.
     * Mirrors Python _needs_budget pre-scan to avoid repeated processing.
     */
    public boolean needsBudget(String historyEntry, String toolName) {
        if (historyEntry == null || toolName == null) {
            return false;
        }
        // Already contains externalized file reference or truncation marker
        return !historyEntry.contains("[Full " + toolName + " output saved to")
                && !historyEntry.contains("chars omitted from " + toolName + " output");
    }

    // ---------------------------------------------------------------------------
    // Exemption
    // ---------------------------------------------------------------------------

    private boolean isExempt(String toolName, DeerFlowProperties.ToolOutputBudget config) {
        if (toolName == null) {
            return false;
        }
        String exempt = config.getExemptTools();
        if (exempt == null || exempt.isBlank()) {
            return false;
        }
        Set<String> exemptSet = Set.of(exempt.split(","));
        return exemptSet.contains(toolName.trim());
    }

    // ---------------------------------------------------------------------------
    // Disk externalization (Python _externalize equivalent)
    // ---------------------------------------------------------------------------

    private String externalizeToDisk(String content, String toolName, String threadId, String runId,
                                     DeerFlowProperties.ToolOutputBudget config) {
        String outputsRoot = properties.getOutputsRoot();
        if (outputsRoot == null || outputsRoot.isBlank()) {
            return null;
        }

        String storageSubdir = config.getStorageSubdir();
        if (storageSubdir == null || storageSubdir.isBlank()) {
            storageSubdir = "tool-outputs";
        }
        // Sanitize subdir to prevent path traversal
        if (storageSubdir.contains("..") || Paths.get(storageSubdir).isAbsolute()) {
            return null;
        }

        Path storageDir = Paths.get(outputsRoot, storageSubdir);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            log.warn("layer=middleware Failed to create externalization directory: {}", storageDir, e);
            return null;
        }

        String filename = buildExternalizedFilename(toolName);
        Path filepath = storageDir.resolve(filename);
        try {
            // Validate that resolved path stays under storageDir
            if (!filepath.toAbsolutePath().normalize().startsWith(storageDir.toAbsolutePath().normalize())) {
                log.warn("layer=middleware Externalization path escapes storage directory: {}", filepath);
                return null;
            }
            Files.writeString(filepath, content);
        } catch (IOException e) {
            log.warn("layer=middleware Failed to write externalized tool output to {}", filepath, e);
            return null;
        }

        // Return virtual path that the model can reference (read_file tool can resolve)
        return UserDataPathResolver.VIRTUAL_OUTPUTS_ROOT + "/" + storageSubdir + "/" + filename;
    }

    private String buildExternalizedFilename(String toolName) {
        String safeName = sanitizeToolName(toolName);
        String ext = extMap(toolName);
        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return safeName + "-" + shortId + "." + ext;
    }

    private String sanitizeToolName(String name) {
        if (name == null) {
            return "unknown";
        }
        String base = Paths.get(name).getFileName().toString();
        String safe = base.replace("..", "").replace("/", "_").replace("\\", "_");
        return safe.isBlank() ? "unknown" : safe;
    }

    private String extMap(String toolName) {
        return switch (toolName) {
            case "bash", "bash_tool", "web_fetch" -> "log";
            default -> "txt";
        };
    }

    // ---------------------------------------------------------------------------
    // Preview builder (Python _build_preview equivalent)
    // ---------------------------------------------------------------------------

    private String buildPreview(String content, String toolName, String virtualPath, int headChars, int tailChars) {
        int total = content.length();
        int headEnd = snapToLineBoundary(content, Math.min(headChars, total));
        int tailStart = Math.max(headEnd, total - tailChars);
        int tailStartSnapped = snapToLineBoundary(content, tailStart);
        if (tailStartSnapped > headEnd) {
            tailStart = tailStartSnapped;
        }

        String head = content.substring(0, headEnd);
        String tail = tailStart < total ? content.substring(tailStart) : "";
        int omitted = total - head.length() - tail.length();

        String ref = "\n\n[Full " + toolName + " output saved to " + virtualPath
                + " (" + total + " chars, ~" + (total / 4) + " tokens)."
                + " Use read_file with start_line and end_line to access specific sections."
                + " " + omitted + " chars omitted from this preview.]\n\n";

        StringBuilder sb = new StringBuilder();
        sb.append(head).append(ref);
        if (!tail.isEmpty()) {
            sb.append(tail);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------
    // Fallback truncation builder (Python _build_fallback equivalent)
    // ---------------------------------------------------------------------------

    private String buildFallback(String content, String toolName, int maxChars, int headChars, int tailChars) {
        int total = content.length();
        if (maxChars <= 0 || total <= maxChars) {
            return content;
        }

        String markerTemplate = "\n\n[... {n} chars omitted from {tn} output. Persistent storage unavailable. Consider narrowing the query or using more specific parameters.]\n\n";
        String marker = markerTemplate.replace("{n}", String.valueOf(total)).replace("{tn}", toolName);
        int markerOverhead = marker.length();

        if (markerOverhead >= maxChars) {
            return content.substring(0, maxChars);
        }

        int budget = maxChars - markerOverhead;
        int effectiveHead = Math.min(headChars, budget);
        int effectiveTail = Math.min(tailChars, Math.max(0, budget - effectiveHead));

        int headEnd = snapToLineBoundary(content, Math.min(effectiveHead, total));
        int tailStart = Math.max(headEnd, total - effectiveTail);
        int tailStartSnapped = snapToLineBoundary(content, tailStart);
        if (tailStartSnapped > headEnd) {
            tailStart = tailStartSnapped;
        }

        String head = content.substring(0, headEnd);
        String tail = tailStart < total ? content.substring(tailStart) : "";
        int omitted = total - head.length() - tail.length();

        String actualMarker = markerTemplate.replace("{n}", String.valueOf(omitted)).replace("{tn}", toolName);
        StringBuilder sb = new StringBuilder();
        sb.append(head).append(actualMarker);
        if (!tail.isEmpty()) {
            sb.append(tail);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------
    // Text helpers
    // ---------------------------------------------------------------------------

    private int snapToLineBoundary(String text, int pos) {
        if (pos <= 0 || pos >= text.length()) {
            return pos;
        }
        int half = pos / 2;
        int nl = text.lastIndexOf('\n', pos - 1);
        if (nl >= half) {
            return nl + 1;
        }
        return pos;
    }
}
