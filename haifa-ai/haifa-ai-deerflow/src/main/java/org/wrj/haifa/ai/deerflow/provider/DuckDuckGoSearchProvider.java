package org.wrj.haifa.ai.deerflow.provider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * DuckDuckGo web search provider.
 *
 * <p>Default search provider. No API key required. Scrapes DuckDuckGo's HTML search interface.</p>
 */
@Component
public class DuckDuckGoSearchProvider implements WebSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoSearchProvider.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Override
    public WebSearchProviderType type() {
        return WebSearchProviderType.DUCKDUCKGO;
    }

    @Override
    public String search(String query, int maxResults) {
        log.info("Performing DuckDuckGo search for query: '{}', maxResults: {}", query, maxResults);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            Elements results = doc.select("#links .result");
            if (results.isEmpty()) {
                results = doc.select(".result");
            }

            if (results.isEmpty()) {
                log.warn("No search results found on DuckDuckGo HTML page for query: '{}'. Falling back to stub.", query);
                return getStubResponse(query);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Search results for: ").append(query).append("\n");
            sb.append("(Provider: DuckDuckGo)\n");

            int count = 0;
            for (Element result : results) {
                if (count >= maxResults) {
                    break;
                }
                Element titleElement = result.selectFirst(".result__title a");
                Element snippetElement = result.selectFirst(".result__snippet");

                if (titleElement != null) {
                    count++;
                    String title = titleElement.text();
                    String linkUrl = titleElement.attr("href");
                    String snippet = (snippetElement != null) ? snippetElement.text() : "";

                    sb.append(count).append(". [").append(title).append("] ").append(linkUrl).append("\n");
                    if (!snippet.isBlank()) {
                        sb.append("   Summary: ").append(snippet).append("\n");
                    }
                }
            }

            if (count == 0) {
                log.warn("Failed to parse any valid result entries. Falling back to stub.");
                return getStubResponse(query);
            }

            return sb.toString().strip();

        } catch (Exception e) {
            log.warn("DuckDuckGo search failed for query: '{}', falling back to stub. Error: {}", query, e.getMessage());
            return getStubResponse(query);
        }
    }

    private String getStubResponse(String query) {
        return """
                Search results for: %s
                (Provider: DuckDuckGo — stub response fallback)
                1. [Example Result] https://example.com/result-1
                   Summary: Sample result from DuckDuckGo stub provider.
                """.formatted(query).strip();
    }
}
