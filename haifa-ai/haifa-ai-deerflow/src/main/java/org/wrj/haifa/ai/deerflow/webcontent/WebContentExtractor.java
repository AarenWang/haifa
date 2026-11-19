package org.wrj.haifa.ai.deerflow.webcontent;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebContentExtractor {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s)]+");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[[^\\]]+\\]\\((https?://[^)]+)\\)");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}(?:[ T]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:?\\d{2})?)?)");

    public ExtractedWebContent extract(String url, String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (looksLikeHtml(content)) {
            return extractHtml(url, content);
        }
        return extractPlain(url, content);
    }

    private ExtractedWebContent extractHtml(String url, String html) {
        Document doc = Jsoup.parse(html, url);
        doc.select("script, style, nav, footer, header, aside, noscript, form, iframe, svg, button").remove();
        doc.select("[class*=cookie], [id*=cookie], [class*=banner], [class*=nav], [class*=menu], [class*=footer], [class*=header]")
                .remove();

        String title = coalesce(
                doc.title(),
                attr(doc, "meta[property=og:title]", "content"),
                firstNonBlankText(doc.select("h1"))
        );
        String canonicalUrl = coalesce(attr(doc, "link[rel=canonical]", "abs:href"), url);
        String author = coalesce(
                attr(doc, "meta[name=author]", "content"),
                attr(doc, "meta[property=article:author]", "content"),
                attr(doc, "a[rel=author]", "text")
        );
        Instant publishedAt = parseDate(coalesce(
                attr(doc, "meta[property=article:published_time]", "content"),
                attr(doc, "meta[name=publication_date]", "content"),
                attr(doc, "meta[name=date]", "content"),
                attr(doc, "time[datetime]", "datetime")
        ));

        Element main = firstPresent(doc.select("main, article, [role=main], .content, .article, .post"));
        if (main == null) {
            main = doc.body();
        }

        List<String> textBlocks = new ArrayList<>();
        Elements candidates = main.select("h1, h2, h3, p, li, blockquote");
        for (Element candidate : candidates) {
            String text = normalizeLine(candidate.text());
            if (isUsefulContentLine(text)) {
                textBlocks.add(text);
            }
        }

        String mainText = deduplicateLines(textBlocks);
        List<String> outgoingLinks = extractLinks(doc, url);

        return new ExtractedWebContent(
                title,
                mainText,
                author,
                publishedAt,
                canonicalUrl,
                outgoingLinks,
                List.of("title", "mainText", "canonicalUrl"),
                List.of("author", "publishedAt", "outgoingLinks")
        );
    }

    private ExtractedWebContent extractPlain(String url, String rawContent) {
        List<String> lines = new ArrayList<>();
        String title = "";
        Matcher urlMatcher = URL_PATTERN.matcher(rawContent);
        Set<String> links = new LinkedHashSet<>();
        while (urlMatcher.find()) {
            links.add(urlMatcher.group());
        }
        Matcher markdownLinks = MARKDOWN_LINK_PATTERN.matcher(rawContent);
        while (markdownLinks.find()) {
            links.add(markdownLinks.group(1));
        }

        String[] split = rawContent.split("\\R");
        for (String rawLine : split) {
            String line = normalizeLine(rawLine);
            if (line.isBlank()) {
                continue;
            }
            if (title.isBlank()) {
                if (line.startsWith("#")) {
                    title = line.replaceFirst("^#+\\s*", "").trim();
                    continue;
                }
                if (!line.toLowerCase(Locale.ROOT).startsWith("fetched content from:")) {
                    title = line;
                }
            }
            if (isUsefulContentLine(line)) {
                lines.add(line.replaceFirst("^#+\\s*", ""));
            }
        }

        String canonicalUrl = coalesce(firstUrl(rawContent), url);
        Instant publishedAt = parseDate(firstMatch(DATE_PATTERN, rawContent));
        String mainText = deduplicateLines(lines);
        if (title.isBlank() && StringUtils.hasText(mainText)) {
            int splitIndex = mainText.indexOf('.');
            title = splitIndex > 0 ? mainText.substring(0, Math.min(splitIndex, 120)).trim() : mainText.substring(0, Math.min(mainText.length(), 120));
        }

        return new ExtractedWebContent(
                title,
                mainText,
                "",
                publishedAt,
                canonicalUrl,
                List.copyOf(links),
                List.of("mainText"),
                List.of("title", "publishedAt", "canonicalUrl", "outgoingLinks")
        );
    }

    private static boolean looksLikeHtml(String content) {
        String lower = content.toLowerCase(Locale.ROOT);
        return lower.contains("<html") || lower.contains("<body") || lower.contains("<article") || lower.contains("<p>");
    }

    private static String attr(Document doc, String selector, String attr) {
        Element element = doc.selectFirst(selector);
        if (element == null) {
            return "";
        }
        if ("text".equals(attr)) {
            return normalizeLine(element.text());
        }
        return normalizeLine(element.attr(attr));
    }

    private static Element firstPresent(Elements elements) {
        for (Element element : elements) {
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private static String firstNonBlankText(Elements elements) {
        for (Element element : elements) {
            String text = normalizeLine(element.text());
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private static List<String> extractLinks(Document doc, String fallbackUrl) {
        Set<String> links = new LinkedHashSet<>();
        for (Element anchor : doc.select("a[href]")) {
            String href = normalizeLine(anchor.attr("abs:href"));
            if (href.isBlank()) {
                href = normalizeLine(anchor.attr("href"));
            }
            if (href.startsWith("http")) {
                links.add(href);
            }
        }
        if (links.isEmpty() && StringUtils.hasText(fallbackUrl)) {
            links.add(fallbackUrl);
        }
        return List.copyOf(links);
    }

    private static String firstUrl(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : "";
    }

    private static String firstMatch(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Instant parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        }
        catch (DateTimeParseException ignored) {
            try {
                return Instant.parse(value);
            }
            catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private static String normalizeLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isUsefulContentLine(String line) {
        if (!StringUtils.hasText(line) || line.length() < 20) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.matches("^(home|menu|navigation|privacy|terms|sign in|sign up|cookie.*)$")) {
            return false;
        }
        return !lower.contains("accept cookies")
                && !lower.contains("cookie banner")
                && !lower.contains("javascript")
                && !lower.startsWith("skip to")
                && !lower.startsWith("all rights reserved");
    }

    private static String deduplicateLines(List<String> lines) {
        Set<String> ordered = new LinkedHashSet<>();
        for (String line : lines) {
            if (StringUtils.hasText(line)) {
                ordered.add(line);
            }
        }
        return String.join("\n\n", ordered);
    }

    private static String coalesce(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    @SuppressWarnings("unused")
    private static String domainOf(String url) {
        try {
            return URI.create(url).getHost();
        }
        catch (Exception ex) {
            return "";
        }
    }
}
