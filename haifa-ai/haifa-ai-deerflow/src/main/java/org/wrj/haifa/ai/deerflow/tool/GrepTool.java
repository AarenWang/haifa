package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class GrepTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_RESULTS = 50;
    private static final int MAX_LINE_LEN = 120;

    @Override
    public String name() {
        return "grep";
    }

    @Override
    public String description() {
        return "Search for patterns inside workspace files. Arguments: {\"pattern\": \"regex_pattern\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("grep");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        try {
            String jsonInput = request.userMessage();
            if (jsonInput == null || jsonInput.isBlank()) {
                return ToolResult.of(name(), "Error: arguments JSON required");
            }
            JsonNode node;
            try {
                node = MAPPER.readTree(jsonInput);
            } catch (Exception jsonEx) {
                return ToolResult.of(name(), "Error parsing tool arguments as JSON: " + jsonEx.getMessage());
            }
            String regex = node.has("pattern") ? node.get("pattern").asText() : null;
            if (regex == null || regex.isBlank()) {
                return ToolResult.of(name(), "Error: pattern is required");
            }

            Pattern pattern;
            try {
                pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                return ToolResult.of(name(), "Error: invalid regex pattern: " + e.getMessage());
            }

            Path root = request.workspaceRoot().toAbsolutePath().normalize();
            List<String> results = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(root)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .toList();
                for (Path file : files) {
                    if (results.size() >= MAX_RESULTS) {
                        break;
                    }
                    try {
                        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                        for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
                            String line = lines.get(lineNum);
                            Matcher m = pattern.matcher(line);
                            if (m.find()) {
                                String displayLine = line.trim();
                                if (displayLine.length() > MAX_LINE_LEN) {
                                    displayLine = displayLine.substring(0, MAX_LINE_LEN) + "...";
                                }
                                Path relPath = root.relativize(file.toAbsolutePath().normalize());
                                results.add(relPath.toString().replace('\\', '/') + ":" + (lineNum + 1) + ": " + displayLine);
                            }
                            if (results.size() >= MAX_RESULTS) {
                                break;
                            }
                        }
                    } catch (IOException | MalformedInputExceptionEx ignore) {
                        // ignore binary or unreadable files
                    }
                }
            }
            return ToolResult.of(name(), results.isEmpty() ? "No matches found for pattern: " + regex : String.join("\n", results));
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }

    private static class MalformedInputExceptionEx extends RuntimeException {}
}
