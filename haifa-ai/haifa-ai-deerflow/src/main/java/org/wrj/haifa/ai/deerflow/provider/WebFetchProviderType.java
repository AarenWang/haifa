package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;

/**
 * Enum of supported web fetch providers.
 */
public enum WebFetchProviderType {

    JINA("jina", "Jina AI Reader", false, true,
            "Default fetch provider; anonymous usage supported (rate limits apply). Optional JINA_API_KEY."),
    EXA("exa", "Exa", true, false,
            "Page content extraction via Exa. Requires EXA_API_KEY."),
    FIRECRAWL("firecrawl", "Firecrawl", true, false,
            "Page extraction via Firecrawl. Requires FIRECRAWL_API_KEY."),
    INFOQUEST("infoquest", "InfoQuest", true, false,
            "Page crawling via InfoQuest. Requires INFOQUEST_API_KEY."),
    GROUNDROUTE("groundroute", "GroundRoute", true, false,
            "Page extraction via GroundRoute meta layer. Requires GROUNDROUTE_API_KEY."),
    BROWSERLESS("browserless", "Browserless", false, false,
            "Headless Chrome rendering for JS-heavy pages. Requires self-hosted or cloud Browserless."),
    FASTCRW("fastcrw", "fastCRW", false, false,
            "Firecrawl-compatible scraper. May require CRW_API_KEY for cloud.");

    private final String id;
    private final String displayName;
    private final boolean requiresApiKey;
    private final boolean defaultEnabled;
    private final String notes;

    WebFetchProviderType(String id, String displayName, boolean requiresApiKey, boolean defaultEnabled, String notes) {
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
        return new ProviderMetadata(id, displayName, requiresApiKey, defaultEnabled, false, true, notes);
    }

    /**
     * Returns the default provider type.
     */
    public static WebFetchProviderType defaultType() {
        return JINA;
    }

    /**
     * Resolves a provider type from its id (case-insensitive).
     *
     * @throws IllegalArgumentException if the id is unknown
     */
    public static WebFetchProviderType fromId(String id) {
        if (id == null || id.isBlank()) {
            return defaultType();
        }
        for (WebFetchProviderType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unknown web_fetch provider: '" + id + "'. Available: " + allIds());
    }

    public static List<String> allIds() {
        return List.of(values()).stream().map(WebFetchProviderType::id).toList();
    }
}
