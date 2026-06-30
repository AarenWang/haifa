package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;

/**
 * Enum of supported web search providers.
 */
public enum WebSearchProviderType {

    DUCKDUCKGO("duckduckgo", "DuckDuckGo", false, true,
            "Default search provider; no API key required."),
    TAVILY("tavily", "Tavily", true, false,
            "High-quality search with structured results. Requires TAVILY_API_KEY."),
    BRAVE("brave", "Brave Search", true, false,
            "Results from Brave's independent index. Requires BRAVE_SEARCH_API_KEY."),
    EXA("exa", "Exa", true, false,
            "Semantic search with neural retrieval. Requires EXA_API_KEY."),
    FIRECRAWL("firecrawl", "Firecrawl", true, false,
            "Firecrawl-powered search. Requires FIRECRAWL_API_KEY."),
    INFOQUEST("infoquest", "InfoQuest", true, false,
            "InfoQuest search integration. Requires INFOQUEST_API_KEY."),
    GROUNDROUTE("groundroute", "GroundRoute", true, false,
            "Meta search layer routing to multiple engines. Requires GROUNDROUTE_API_KEY."),
    SERPER("serper", "Serper", true, false,
            "Google Search results via Serper. Requires SERPER_API_KEY."),
    SEARXNG("searxng", "SearXNG", false, false,
            "Self-hosted metasearch engine. Requires a SearXNG instance."),
    FASTCRW("fastcrw", "fastCRW", false, false,
            "Firecrawl-compatible scraper. May require CRW_API_KEY for cloud.");

    private final String id;
    private final String displayName;
    private final boolean requiresApiKey;
    private final boolean defaultEnabled;
    private final String notes;

    WebSearchProviderType(String id, String displayName, boolean requiresApiKey, boolean defaultEnabled, String notes) {
        this.id = id;
        this.displayName = displayName;
        this.requiresApiKey = requiresApiKey;
        this.defaultEnabled = defaultEnabled;
        this.notes = notes;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean requiresApiKey() {
        return requiresApiKey;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public String notes() {
        return notes;
    }

    public ProviderMetadata toMetadata() {
        return new ProviderMetadata(id, displayName, requiresApiKey, defaultEnabled, true, false, notes);
    }

    /**
     * Returns the default provider type.
     */
    public static WebSearchProviderType defaultType() {
        return DUCKDUCKGO;
    }

    /**
     * Resolves a provider type from its id (case-insensitive).
     *
     * @throws IllegalArgumentException if the id is unknown
     */
    public static WebSearchProviderType fromId(String id) {
        if (id == null || id.isBlank()) {
            return defaultType();
        }
        for (WebSearchProviderType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unknown web_search provider: '" + id + "'. Available: " + allIds());
    }

    public static List<String> allIds() {
        return List.of(values()).stream().map(WebSearchProviderType::id).toList();
    }
}
