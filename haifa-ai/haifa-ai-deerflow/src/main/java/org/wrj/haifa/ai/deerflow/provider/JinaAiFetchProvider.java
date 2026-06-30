package org.wrj.haifa.ai.deerflow.provider;

import org.springframework.stereotype.Component;

/**
 * Jina AI Reader web fetch provider.
 *
 * <p>Default fetch provider. Anonymous usage supported (with rate limits).
 * Currently returns a stub response; full HTTP implementation can be added
 * later without changing the tool contract.</p>
 */
@Component
public class JinaAiFetchProvider implements WebFetchProvider {

    @Override
    public WebFetchProviderType type() {
        return WebFetchProviderType.JINA;
    }

    @Override
    public String fetch(String url) {
        // Stub: return a formatted mock response
        // TODO: integrate with Jina AI Reader API (https://r.jina.ai/http://URL)
        return """
                Fetched content from: %s
                (Provider: Jina AI Reader — stub response)
                This is placeholder content. The Jina AI Reader provider will
                return clean Markdown extracted from the target page.
                """.formatted(url).strip();
    }
}
