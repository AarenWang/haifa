package org.wrj.haifa.ai.deerflow.tool;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class CurrentTimeTool implements ParallelSafeAgentTool {

    @Override
    public String name() {
        return "current_time";
    }

    @Override
    public String description() {
        return "Returns the current local date and time.";
    }

    @Override
    public boolean supports(String userMessage) {
        String text = normalize(userMessage);
        return text.contains("time") || text.contains("date") || text.contains("today") || text.contains("now")
                || text.contains("时间") || text.contains("日期") || text.contains("今天") || text.contains("现在");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        return ToolResult.of(name(), ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
