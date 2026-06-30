package org.wrj.haifa.ai.deerflow.webcontent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebContentExtractorTest {

    @Test
    void extractsMainTextWithoutNavigationAndFooterNoise() {
        WebContentExtractor extractor = new WebContentExtractor();
        String html = """
                <html>
                  <head>
                    <title>Deep Research Notes</title>
                    <link rel="canonical" href="https://example.com/research" />
                    <meta property="article:published_time" content="2026-05-01T10:00:00Z" />
                  </head>
                  <body>
                    <nav>Home Products Pricing Cookie Banner</nav>
                    <header>Sign in</header>
                    <main>
                      <article>
                        <p>Deep research requires reading full sources instead of snippets.</p>
                        <p>Evidence should stay linked to the original source and remain reusable.</p>
                      </article>
                    </main>
                    <footer>Privacy policy and all rights reserved.</footer>
                    <script>console.log('noise')</script>
                  </body>
                </html>
                """;

        ExtractedWebContent content = extractor.extract("https://example.com/research?utm_source=test", html);

        assertThat(content.title()).isEqualTo("Deep Research Notes");
        assertThat(content.canonicalUrl()).isEqualTo("https://example.com/research");
        assertThat(content.mainText()).contains("Deep research requires reading full sources");
        assertThat(content.mainText()).contains("Evidence should stay linked");
        assertThat(content.mainText()).doesNotContain("Cookie Banner");
        assertThat(content.mainText()).doesNotContain("Privacy policy");
        assertThat(content.reliableFields()).contains("title", "mainText", "canonicalUrl");
    }
}
