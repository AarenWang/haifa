package org.wrj.haifa.ai.deerflow.research;

import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.webcontent.ExtractedWebContent;

@Component
public class SourceQualityScorer {

    public double scoreCandidate(String url, String title, String snippet) {
        double score = 0.35;
        String domain = domainOf(url);
        if (domain.endsWith(".gov") || domain.endsWith(".edu")) {
            score += 0.3;
        } else if (domain.contains("docs.") || domain.contains("developer.")) {
            score += 0.2;
        } else if (domain.contains("github.com") || domain.contains("wikipedia.org")) {
            score += 0.15;
        }
        if (StringUtils.hasText(title)) {
            score += 0.15;
        }
        if (StringUtils.hasText(snippet)) {
            score += 0.1;
        }
        return round(score);
    }

    public double scoreFetched(String url, ExtractedWebContent content, boolean duplicateContent) {
        double score = scoreCandidate(url, content.title(), content.mainText());
        if (content.hasMainText()) {
            score += 0.15;
        }
        if (content.publishedAt() != null) {
            score += 0.1;
        }
        if (StringUtils.hasText(content.author())) {
            score += 0.05;
        }
        String lower = content.mainText().toLowerCase(Locale.ROOT);
        if (lower.contains("subscribe now") || lower.contains("advertisement") || lower.contains("click here")) {
            score -= 0.15;
        }
        if (duplicateContent) {
            score -= 0.2;
        }
        return round(score);
    }

    private static String domainOf(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        }
        catch (Exception ex) {
            return "";
        }
    }

    private static double round(double value) {
        return Math.max(0.0, Math.min(1.0, Math.round(value * 100.0) / 100.0));
    }
}
