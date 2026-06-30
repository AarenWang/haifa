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

    public List<ParsedToolCall> parse(String modelResponse) {
        List<ParsedToolCall> calls = new ArrayList<>();
        if (modelResponse == null || modelResponse.isBlank()) {
            return calls;
        }
        Matcher matcher = TOOL_CALL_PATTERN.matcher(modelResponse);
        while (matcher.find()) {
            String toolName = matcher.group(1);
            String argsJson = matcher.group(2);
            calls.add(new ParsedToolCall(toolName, argsJson.trim(), matcher.start(), matcher.end()));
        }
        return calls;
    }

    public boolean hasToolCall(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            return false;
        }
        return TOOL_CALL_PATTERN.matcher(modelResponse).find();
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

    public record ParsedToolCall(String toolName, String arguments, int startIndex, int endIndex) {
    }
}
