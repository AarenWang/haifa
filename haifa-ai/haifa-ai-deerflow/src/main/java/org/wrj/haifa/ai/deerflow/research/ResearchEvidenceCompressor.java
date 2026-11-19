package org.wrj.haifa.ai.deerflow.research;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

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
    private final CompressionCacheService compressionCacheService;
    private final DeerFlowProperties properties;

    public ResearchEvidenceCompressor(AgentModelClient modelClient) {
        this(modelClient, null, new DeerFlowProperties());
    }

    public ResearchEvidenceCompressor(AgentModelClient modelClient,
            CompressionCacheService compressionCacheService) {
        this(modelClient, compressionCacheService, new DeerFlowProperties());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ResearchEvidenceCompressor(AgentModelClient modelClient,
            CompressionCacheService compressionCacheService,
            DeerFlowProperties properties) {
        this.modelClient = modelClient;
        this.compressionCacheService = compressionCacheService;
        this.properties = properties == null ? new DeerFlowProperties() : properties;
    }

    public String compressFetchContent(String title, String url, String sourceId, String content) {
        String contentText = content == null ? "" : content;
        String contentHash = org.wrj.haifa.ai.deerflow.prompt.PromptCanonicalizer.sha256Hex(contentText);
        DeerFlowProperties.PromptCache.CompressionPromptCache config = properties.getPromptCache().getCompression();
        boolean cacheEnabled = properties.getPromptCache().isEnabled() && config.isEnabled()
                && compressionCacheService != null;
        String model = properties.getModel() == null || properties.getModel().isBlank()
                ? "default" : properties.getModel();
        String provider = providerFor(model);
        String cacheKey = cacheEnabled
                ? CompressionCacheService.computeCacheKey(config.getSchemaVersion(), provider, model,
                        config.getPromptVersion(), "contract-v1", url, contentHash)
                : "";

        String reusableBody;
        if (cacheEnabled) {
            reusableBody = compressionCacheService.getOrCompute(
                    cacheKey, config.getSchemaVersion(), provider, model, config.getPromptVersion(), url,
                    contentHash, contentText.length(), parsePositiveDuration(config.getTtl(), java.time.Duration.ofDays(7)),
                    () -> computeReusableSummary(contentText)
            );
        } else {
            reusableBody = computeReusableSummary(contentText);
        }

        if (reusableBody == null || reusableBody.isBlank()) {
            reusableBody = buildFallback(contentText, null, null, null, 2000, 700, 700);
        }

        return "Source ID: " + sourceId + "\nURL: " + url + "\nTitle: " + title + "\n\n" + reusableBody;
    }

    private String computeReusableSummary(String contentText) {
        String systemPrompt = "You are a precise research compressor. Your task is to compress the provided web page content into a highly informative, citable summary.\n" +
                "Extract the core facts, statistics, key findings, and main claims relevant for research.\n" +
                "Keep the final output under 1500 characters, written in a clear, concise bullet-point style.";
        ModelPrompt prompt = new ModelPrompt(systemPrompt, contentText, null);
        try {
            ModelResponse response = modelClient.generate(prompt).block();
            return response != null ? response.content() : "";
        } catch (Exception e) {
            log.warn("Failed to compress fetch content, error: {}. Falling back to head+tail truncation.", e.getMessage());
            return "";
        }
    }

    private static java.time.Duration parsePositiveDuration(String configured, java.time.Duration fallback) {
        try {
            java.time.Duration parsed = java.time.Duration.parse(configured);
            return parsed.isNegative() || parsed.isZero() ? fallback : parsed;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String providerFor(String model) {
        String normalized = model == null ? "" : model.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("gemini") || normalized.contains("google")) {
            return "google-genai";
        }
        if (normalized.contains("gpt") || normalized.contains("openai")) {
            return "openai";
        }
        return "generic";
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
