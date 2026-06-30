package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses tool call requests from model responses.
 * Expected format: <tool_call name="tool_name">{"arg1":"value1"}</tool_call>
 */
public class ToolCallParser {

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "<tool_call\\s+name=\"([^\"]+)\"\\s*>([^<]*)</tool_call>");

    private static final Pattern INVOKE_START = Pattern.compile(
            "<[^>]*?invoke\\s+name=\"([^\"]+)\"[^>]*>");

    private static final Pattern INVOKE_END = Pattern.compile(
            "</[^>]*?invoke[^>]*>");

    private static final Pattern PARAMETER_TAG = Pattern.compile(
            "<[^>]*?parameter\\s+name=\"([^\"]+)\"[^>]*>([^<]*)</[^>]*?parameter\\s*>");

    public List<ParsedToolCall> parse(String modelResponse) {
        List<ParsedToolCall> calls = new ArrayList<>();
        if (modelResponse == null || modelResponse.isBlank()) {
            return calls;
        }

        // 1. Parse standard tool_call format
        Matcher matcher = TOOL_CALL_PATTERN.matcher(modelResponse);
        while (matcher.find()) {
            String toolName = matcher.group(1);
            String argsJson = matcher.group(2);
            calls.add(new ParsedToolCall(toolName, argsJson.trim(), matcher.start(), matcher.end()));
        }

        // 2. Parse XML invoke/parameter formats (Function calls and DSML)
        Matcher invokeStartMatcher = INVOKE_START.matcher(modelResponse);
        int searchIdx = 0;
        while (invokeStartMatcher.find(searchIdx)) {
            String toolName = invokeStartMatcher.group(1);
            int startIdx = invokeStartMatcher.start();
            int contentStart = invokeStartMatcher.end();

            Matcher invokeEndMatcher = INVOKE_END.matcher(modelResponse);
            if (invokeEndMatcher.find(contentStart)) {
                int contentEnd = invokeEndMatcher.start();
                int endIdx = invokeEndMatcher.end();

                String invokeContent = modelResponse.substring(contentStart, contentEnd);

                java.util.Map<String, String> params = new java.util.HashMap<>();
                Matcher paramMatcher = PARAMETER_TAG.matcher(invokeContent);
                while (paramMatcher.find()) {
                    String paramName = paramMatcher.group(1);
                    String paramVal = paramMatcher.group(2).trim();
                    params.put(paramName, paramVal);
                }

                String argumentsJson = "{}";
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    argumentsJson = mapper.writeValueAsString(params);
                } catch (Exception e) {
                    StringBuilder sb = new StringBuilder("{");
                    int i = 0;
                    for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
                        if (i > 0) sb.append(",");
                        sb.append("\"").append(entry.getKey().replace("\"", "\\\"")).append("\":\"")
                          .append(entry.getValue().replace("\"", "\\\"").replace("\\", "\\\\")).append("\"");
                        i++;
                    }
                    sb.append("}");
                    argumentsJson = sb.toString();
                }

                calls.add(new ParsedToolCall(toolName, argumentsJson, startIdx, endIdx));
                searchIdx = endIdx;
            } else {
                searchIdx = contentStart;
            }
        }

        return calls;
    }

    public boolean hasToolCall(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            return false;
        }
        return TOOL_CALL_PATTERN.matcher(modelResponse).find() || INVOKE_START.matcher(modelResponse).find();
    }

    public boolean hasFinalAnswer(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            return false;
        }
        return modelResponse.contains("<final_answer>") || modelResponse.contains("FINAL ANSWER");
    }

    public String extractFinalAnswer(String modelResponse) {
        if (modelResponse == null) {
            return "";
        }
        int idx = modelResponse.indexOf("<final_answer>");
        if (idx >= 0) {
            int endIdx = modelResponse.indexOf("</final_answer>", idx);
            if (endIdx > idx) {
                return modelResponse.substring(idx + "<final_answer>".length(), endIdx).trim();
            }
            return modelResponse.substring(idx + "<final_answer>".length()).trim();
        }
        return modelResponse;
    }

    public String cleanResponseText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text;
        // Remove standard tool_call tags
        cleaned = Pattern.compile("<tool_call\\s+name=\"[^\"]+\"\\s*>([^<]*)</tool_call>").matcher(cleaned).replaceAll("");
        // Remove invoke and parameter tags
        cleaned = Pattern.compile("<[^>]*?invoke\\s+name=\"[^\"]+\"[^>]*>").matcher(cleaned).replaceAll("");
        cleaned = Pattern.compile("</[^>]*?invoke[^>]*>").matcher(cleaned).replaceAll("");
        cleaned = Pattern.compile("<[^>]*?parameter\\s+name=\"[^\"]+\"[^>]*>[^<]*</[^>]*?parameter\\s*>").matcher(cleaned).replaceAll("");
        // Remove container tags if any
        cleaned = Pattern.compile("<function_calls>").matcher(cleaned).replaceAll("");
        cleaned = Pattern.compile("</function_calls>").matcher(cleaned).replaceAll("");
        cleaned = Pattern.compile("<\\s*\\|\\s*\\|\\s*DSML\\s*\\|\\s*\\|\\s*toolcalls\\s*>").matcher(cleaned).replaceAll("");
        cleaned = Pattern.compile("</\\s*\\|\\s*/\\s*DSML\\s*\\|\\s*/\\s*\\|\\s*toolcalls\\s*>").matcher(cleaned).replaceAll("");
        // Remove thinking tags if present
        cleaned = Pattern.compile("<thinking>[^<]*</thinking>").matcher(cleaned).replaceAll("");
        cleaned = Pattern.compile("<thinking>[^<]*$").matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    public record ParsedToolCall(String toolName, String arguments, int startIndex, int endIndex) {
    }
}
