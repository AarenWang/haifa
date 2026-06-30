package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.Map;

/**
 * Mock web search tool for testing the agent loop without external APIs.
 * NOT a Spring component — must be registered explicitly for tests only.
 */
public class MockSearchTool implements AgentTool {

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "Mock web search tool. Arguments: {\"query\": \"search query\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        if (userMessage == null) return false;
        String lower = userMessage.toLowerCase();
        return lower.contains("web_search") || lower.contains("mock_search");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String query = request.userMessage();
        return ToolResult.of(name(), """
                Search results for: %s
                
                1. [Example Article 1] https://example.com/article-1
                   Summary: This is a sample article about the topic.
                   
                2. [Example Article 2] https://example.com/article-2
                   Summary: Another relevant article with useful information.
                   
                3. [Example Docs] https://example.com/docs
                   Summary: Official documentation with detailed reference.
                """.formatted(query), Map.of("query", query, "resultsCount", 3));
    }
}
