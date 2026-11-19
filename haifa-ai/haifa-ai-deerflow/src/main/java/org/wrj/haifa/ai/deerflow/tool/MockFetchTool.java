package org.wrj.haifa.ai.deerflow.tool;

import java.util.Map;

/**
 * Mock web fetch tool for testing the agent loop without external APIs.
 * NOT a Spring component — must be registered explicitly for tests only.
 */
public class MockFetchTool implements AgentTool {

    @Override
    public String name() {
        return "web_fetch";
    }

    @Override
    public String description() {
        return "Mock web fetch tool. Arguments: {\"url\": \"url to fetch\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        if (userMessage == null) return false;
        String lower = userMessage.toLowerCase();
        return lower.contains("web_fetch") || lower.contains("mock_fetch");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String url = extractUrl(request.userMessage());
        String content = switch (url) {
            case "https://example.com/article-1" -> "This is the full content of article 1. It contains detailed information about the topic, including key findings and methodology.";
            case "https://example.com/article-2" -> "Article 2 provides a comprehensive overview with statistics, case studies, and expert opinions on the subject matter.";
            case "https://example.com/docs" -> "The official documentation includes API references, implementation guides, and best practices for working with this system.";
            default -> "No mock content available for URL: " + url + ". This is a fallback response with general information about the topic.";
        };
        return ToolResult.of(name(), content, Map.of("url", url, "contentLength", content.length()));
    }

    private static String extractUrl(String message) {
        if (message == null) return "";
        // Simple URL extraction: look for http:// or https://
        int start = message.indexOf("http");
        if (start < 0) return "";
        int end = message.indexOf(' ', start);
        if (end < 0) end = message.indexOf('\n', start);
        if (end < 0) end = message.length();
        return message.substring(start, end).trim();
    }
}
