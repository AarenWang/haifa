package org.wrj.haifa.ai.deerflow.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;

class PromptCanonicalizerTest {

    @Test
    void normalizesLineEndingsAndTrailingSpaces() {
        String input = "Line 1  \r\nLine 2\rLine 3   ";
        String canonical = PromptCanonicalizer.canonicalizeText(input);
        assertThat(canonical).isEqualTo("Line 1\nLine 2\nLine 3");
    }

    @Test
    void sortsJsonSchemaObjectKeysRecursively() {
        String schema = """
                {
                  "z": 1,
                  "a": {
                    "y": true,
                    "b": "text"
                  }
                }
                """;
        String canonicalSchema = PromptCanonicalizer.canonicalizeJsonSchema(schema);
        assertThat(canonicalSchema).isEqualTo("{\"a\":{\"b\":\"text\",\"y\":true},\"z\":1}");
    }

    @Test
    void sortsToolDefinitionsByName() {
        ModelToolDefinition toolB = new ModelToolDefinition("web_search", "Search web", "{}");
        ModelToolDefinition toolA = new ModelToolDefinition("bash", "Run bash", "{}");

        String canonical1 = PromptCanonicalizer.canonicalizeToolDefinitions(List.of(toolB, toolA));
        String canonical2 = PromptCanonicalizer.canonicalizeToolDefinitions(List.of(toolA, toolB));

        assertThat(canonical1).isEqualTo(canonical2);
        assertThat(canonical1).contains("tool: bash");
        assertThat(canonical1).contains("tool: web_search");
    }
}
