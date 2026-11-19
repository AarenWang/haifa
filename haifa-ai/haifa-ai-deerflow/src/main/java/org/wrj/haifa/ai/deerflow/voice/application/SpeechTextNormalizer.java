package org.wrj.haifa.ai.deerflow.voice.application;

import java.util.regex.Pattern;

public class SpeechTextNormalizer {

    private static final Pattern CODE_BLOCK = Pattern.compile("```[a-zA-Z0-9]*\\n[\\s\\S]*?\\n```");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern MARKDOWN_URL = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^)]+)\\)");
    private static final Pattern RAW_URL = Pattern.compile("https?://[^\\s]+");
    private static final Pattern TABLE_ROW = Pattern.compile("^\\|.*\\|$", Pattern.MULTILINE);
    private static final Pattern MARKDOWN_HEADERS = Pattern.compile("#{1,6}\\s*");
    private static final Pattern MARKDOWN_BOLD_ITALIC = Pattern.compile("(\\*\\*|\\*|__|_|~~)");

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text;

        // Replace code blocks with spoken summary
        cleaned = CODE_BLOCK.matcher(cleaned).replaceAll("已生成代码，可在界面查看。");

        // Remove table rows
        cleaned = TABLE_ROW.matcher(cleaned).replaceAll("");

        // Simplify Markdown URLs to just text label
        cleaned = MARKDOWN_URL.matcher(cleaned).replaceAll("$1");

        // Simplify raw URLs
        cleaned = RAW_URL.matcher(cleaned).replaceAll("网页链接");

        // Remove inline code ticks
        cleaned = INLINE_CODE.matcher(cleaned).replaceAll("$1");

        // Remove Markdown headers (# ## ###)
        cleaned = MARKDOWN_HEADERS.matcher(cleaned).replaceAll("");

        // Remove Markdown emphasis (*, **, _, __, ~~)
        cleaned = MARKDOWN_BOLD_ITALIC.matcher(cleaned).replaceAll("");

        // Trim consecutive blank lines and spaces
        cleaned = cleaned.replaceAll("\\n{2,}", "\n").trim();

        return cleaned;
    }
}
