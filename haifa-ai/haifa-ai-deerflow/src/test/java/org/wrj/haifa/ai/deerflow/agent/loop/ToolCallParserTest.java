package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ToolCallParserTest {

    private final ToolCallParser parser = new ToolCallParser();

    @Test
    void parsesStandardToolCall() {
        String response = "Let me search: <tool_call name=\"web_search\">{\"query\":\"spring boot 3\"}</tool_call>";
        List<ToolCallParser.ParsedToolCall> calls = parser.parse(response);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).toolName()).isEqualTo("web_search");
        assertThat(calls.get(0).arguments()).isEqualTo("{\"query\":\"spring boot 3\"}");
    }

    @Test
    void parsesNestedToolNameAndJsonToolCall() {
        String response = """
                Let me search.
                <tool_call>
                  <tool_name>web_search</tool_name>
                  <json>
                    {"query": "linear algebra learning roadmap", "max_results": 10}
                  </json>
                </tool_call>
                """;

        List<ToolCallParser.ParsedToolCall> calls = parser.parse(response);

        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).toolName()).isEqualTo("web_search");
        assertThat(calls.get(0).arguments()).contains("\"query\": \"linear algebra learning roadmap\"");
        assertThat(calls.get(0).arguments()).contains("\"max_results\": 10");
    }

    @Test
    void parsesFunctionCallsXmlFormat() {
        String response = """
            Here is a function call:
            <function_calls>
            <invoke name="read_file">
            <parameter name="file_path" string="true">D:\\workspace\\haifa\\pom.xml</parameter>
            </invoke>
            </function_calls>
            """;
        List<ToolCallParser.ParsedToolCall> calls = parser.parse(response);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).toolName()).isEqualTo("read_file");
        // Check JSON arguments conversion
        assertThat(calls.get(0).arguments()).contains("\"file_path\"");
        assertThat(calls.get(0).arguments()).contains("pom.xml");
    }

    @Test
    void parsesDsmlFormat() {
        String response = """
            < | | DSML | | toolcalls>
            < / | DSML | / |invoke name="readfile">
            < | | DSML | | parameter name="filepath" string="true">D:\\workspace\\haifa\\skills\\SKILL.md</ | / DSML | / | parameter>
            </ | / DSML | / | invoke>
            </ | / DSML | / | toolcalls>
            """;
        List<ToolCallParser.ParsedToolCall> calls = parser.parse(response);
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).toolName()).isEqualTo("readfile");
        assertThat(calls.get(0).arguments()).contains("\"filepath\"");
        assertThat(calls.get(0).arguments()).contains("SKILL.md");
    }

    @Test
    void hasToolCallChecksBothFormats() {
        String standard = "Use <tool_call name=\"foo\">{}</tool_call>";
        String nested = "Use <tool_call><tool_name>foo</tool_name><json>{}</json></tool_call>";
        String invoke = "Use <invoke name=\"bar\"></invoke>";
        String plain = "No tool call here";

        assertThat(parser.hasToolCall(standard)).isTrue();
        assertThat(parser.hasToolCall(nested)).isTrue();
        assertThat(parser.hasToolCall(invoke)).isTrue();
        assertThat(parser.hasToolCall(plain)).isFalse();
    }

    @Test
    void cleansResponseTextCorrectly() {
        String mixedText = "Here is some thinking: <thinking>I will read file now</thinking> " +
                "Reading file: <tool_call name=\"read_file\">{\"path\":\"pom.xml\"}</tool_call> and done.";
        String cleaned = parser.cleanResponseText(mixedText);
        assertThat(cleaned).isEqualTo("Here is some thinking:  Reading file:  and done.");
    }

    @Test
    void cleansNestedToolCallText() {
        String text = "Search now <tool_call><tool_name>web_search</tool_name><json>{\"query\":\"x\"}</json></tool_call> done.";

        String cleaned = parser.cleanResponseText(text);

        assertThat(cleaned).isEqualTo("Search now  done.");
    }
}
