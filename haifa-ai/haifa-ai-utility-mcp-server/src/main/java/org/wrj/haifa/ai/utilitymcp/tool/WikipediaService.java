package org.wrj.haifa.ai.utilitymcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolArguments;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;
import org.wrj.haifa.ai.utilitymcp.provider.JsonProvider;
import org.wrj.haifa.ai.utilitymcp.provider.ProviderPayload;

public class WikipediaService {

    private static final Set<String> LANGUAGES = Set.of("en", "zh", "de", "fr", "es", "ja");
    private static final int MAX_EXTRACT_CHARS = 20_000;
    private final JsonProvider provider;

    public WikipediaService(JsonProvider provider) {
        this.provider = provider;
    }

    public UtilityResult search(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String query = args.requiredString("query", 300);
        String language = language(args.optionalString("language", "en", 8));
        int limit = args.intValue("limit", 5, 1, 20);
        ProviderPayload payload = provider.get("/core/v1/wikipedia/" + language + "/search/page",
                Map.of("q", query, "limit", limit));
        JsonNode pages = payload.body().path("pages");
        if (!pages.isArray()) throw JsonSupport.malformed("Wikipedia search pages are missing");
        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode page : pages) {
            if (results.size() >= limit) break;
            String title = JsonSupport.text(page, "title");
            String key = JsonSupport.optionalText(page, "key");
            if (key == null) key = title.replace(' ', '_');
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", title);
            item.put("pageKey", key);
            if (page.has("id")) item.put("pageId", page.path("id").asText());
            String description = JsonSupport.optionalText(page, "description");
            String excerpt = JsonSupport.optionalText(page, "excerpt");
            if (description != null) item.put("description", safeText(description, 500));
            if (excerpt != null) item.put("snippet", safeText(excerpt, 1_000));
            item.put("canonicalUrl", canonicalUrl(language, key));
            item.put("language", language);
            results.add(item);
        }
        return UtilityResult.external(Map.of("results", results), "wikimedia", payload.sourceUri(),
                payload.retrievedAt(), payload.cached(), results.size() >= limit, null, Map.of());
    }

    public UtilityResult summary(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String title = args.requiredString("title", 300);
        String language = language(args.optionalString("language", "en", 8));
        String key = title.replace(' ', '_');
        String encoded = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
        ProviderPayload payload = provider.get("/core/v1/wikipedia/" + language + "/page/" + encoded + "/with_html", Map.of());
        JsonNode page = payload.body();
        String canonicalTitle = JsonSupport.optionalText(page, "title");
        if (canonicalTitle == null) canonicalTitle = title;
        String html = JsonSupport.optionalText(page, "html");
        String source = JsonSupport.optionalText(page, "source");
        String extract = html != null ? Jsoup.parse(html).text() : (source == null ? "" : Jsoup.parse(source).text());
        if (extract.isBlank()) {
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    "Wikipedia response did not contain readable page content", true);
        }
        boolean partial = extract.length() > MAX_EXTRACT_CHARS;
        if (partial) extract = extract.substring(0, MAX_EXTRACT_CHARS);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", canonicalTitle);
        data.put("extract", extract);
        data.put("canonicalUrl", canonicalUrl(language, key));
        data.put("language", language);
        String latest = JsonSupport.optionalText(page, "latest");
        if (latest != null) data.put("lastModified", latest);
        return UtilityResult.external(data, "wikimedia", java.net.URI.create(canonicalUrl(language, key)),
                payload.retrievedAt(), payload.cached(), partial, latest, Map.of());
    }

    private static String language(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(normalized)) {
            throw new UtilityToolException(UtilityErrorCode.UNSUPPORTED_VALUE,
                    "language must be one of " + LANGUAGES, false);
        }
        return normalized;
    }

    private static String canonicalUrl(String language, String pageKey) {
        String encoded = URLEncoder.encode(pageKey, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://" + language + ".wikipedia.org/wiki/" + encoded;
    }

    private static String safeText(String value, int maxChars) {
        String text = Jsoup.parse(value).text().replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }
}
