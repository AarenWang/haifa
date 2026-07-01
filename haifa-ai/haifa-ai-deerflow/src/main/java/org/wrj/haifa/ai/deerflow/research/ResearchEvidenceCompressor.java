package org.wrj.haifa.ai.deerflow.research;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;

/**
 * Compresses research evidence (web fetch results) using an LLM.
 * <p>
 * When the LLM call fails (e.g., network timeout, model error), this class falls back to a
 * deterministic head+tail truncation inspired by Python deer-flow {@code _build_fallback}.
 * This ensures the research loop never breaks even when the compression service is unstable.
 */
@Component
public class ResearchEvidenceCompressor {

    private static final Logger log = LoggerFactory.getLogger(ResearchEvidenceCompressor.class);

    private final AgentModelClient modelClient;

    public ResearchEvidenceCompressor(AgentModelClient modelClient) {
        this.modelClient = modelClient;
    }

    public String compressFetchContent(String title, String url, String sourceId, String content) {
        String systemPrompt = "You are a precise research compressor. Your task is to compress the provided web page content into a highly informative, citable summary.\n" +
                "Extract the core facts, statistics, key findings, and main claims relevant for research.\n" +
                "You MUST preserve and output the source metadata at the top: Source ID: " + sourceId + ", URL: " + url + ", Title: " + title + ".\n" +
                "Keep the final output under 1500 characters, written in a clear, concise bullet-point style.";
        ModelPrompt prompt = new ModelPrompt(systemPrompt, content, null);
        try {
            ModelResponse response = modelClient.generate(prompt).block();
            return response != null ? response.content() : "[Empty Summary]";
        } catch (Exception e) {
            log.warn("Failed to compress fetch content for URL: {}, error: {}. Falling back to head+tail truncation.", url, e.getMessage());
            // Head+tail fallback (Python deer-flow _build_fallback equivalent)
            return buildFallback(content, sourceId, url, title, 2000, 700, 700);
        }
    }

    public String compressGenericOutput(String toolName, String content) {
        String systemPrompt = "Summarize the following tool output for tool: " + toolName + ". Keep it under 1000 characters.";
        ModelPrompt prompt = new ModelPrompt(systemPrompt, content, null);
        try {
            ModelResponse response = modelClient.generate(prompt).block();
            return response != null ? response.content() : "[Empty Summary]";
        } catch (Exception e) {
            log.warn("Failed to compress generic tool output, error: {}. Falling back to head+tail truncation.", e.getMessage());
            return buildFallback(content, null, null, toolName, 1500, 500, 500);
        }
    }

    // ---------------------------------------------------------------------------
    // Head+tail fallback (Python deer-flow _build_fallback equivalent)
    // ---------------------------------------------------------------------------

    private String buildFallback(String content, String sourceId, String url, String title, int maxChars, int headChars, int tailChars) {
        int total = content.length();
        if (maxChars <= 0 || total <= maxChars) {
            return content;
        }

        String markerTemplate = "\n\n[... {n} chars omitted. Persistent storage / LLM compression unavailable. Consider narrowing the query or using more specific parameters.]\n\n";
        String marker = markerTemplate.replace("{n}", String.valueOf(total));
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

        String actualMarker = markerTemplate.replace("{n}", String.valueOf(omitted));

        StringBuilder sb = new StringBuilder();
        if (sourceId != null || url != null || title != null) {
            sb.append("Source ID: ").append(sourceId).append("\n");
            sb.append("URL: ").append(url).append("\n");
            sb.append("Title: ").append(title).append("\n");
            sb.append("Content (Truncated due to compression error):\n");
        }
        sb.append(head).append(actualMarker);
        if (!tail.isEmpty()) {
            sb.append(tail);
        }
        return sb.toString();
    }

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
