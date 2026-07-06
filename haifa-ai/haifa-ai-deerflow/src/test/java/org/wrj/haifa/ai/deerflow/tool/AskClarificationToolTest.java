package org.wrj.haifa.ai.deerflow.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentClarificationStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;

@SpringBootTest
@ActiveProfiles("test")
class AskClarificationToolTest {

    @Autowired
    private AgentClarificationStore store;

    @BeforeEach
    void clearStore() {
        store.clearAll();
    }

    @Test
    void createsStructuredQuestionsWithGeneratedChoiceLabels() {
        AskClarificationTool tool = new AskClarificationTool(store);

        ToolResult result = tool.execute(new ToolRequest("""
                {
                  "title": "Need song details",
                  "clarification_type": "ambiguous_requirement",
                  "context": "The user asked for a personalized song.",
                  "questions": [
                    {
                      "id": "music_style",
                      "title": "Music style preference",
                      "prompt": "Choose a direction or enter your own.",
                      "answer_type": "SINGLE_CHOICE_WITH_CUSTOM",
                      "allow_custom": true,
                      "choices": [
                        "Energetic electronic rock",
                        "Ambient folk",
                        "French jazz",
                        "Other"
                      ]
                    },
                    {
                      "id": "lyrics_language",
                      "title": "Lyrics language",
                      "choices": ["Chinese", "English", "Bilingual"]
                    }
                  ]
                }
                """, Path.of("."), java.util.List.of(), "thread-1", "run-1"));

        assertThat(result.metadata()).containsEntry("clarificationRequired", true);
        ClarificationRecord record = store.findPending("thread-1").orElseThrow();
        assertThat(record.question()).isEqualTo("Need song details");
        assertThat(record.questions()).hasSize(2);
        assertThat(record.questions().get(0).allowCustom()).isTrue();
        assertThat(record.questions().get(0).choices())
                .extracting("label")
                .containsExactly("A", "B", "C", "D");
        assertThat(record.questions().get(1).answerType()).isEqualTo("SINGLE_CHOICE_WITH_CUSTOM");
    }

    @Test
    void questionsWithoutChoicesRemainAnswerableEvenWhenCustomIsFalse() {
        AskClarificationTool tool = new AskClarificationTool(store);

        tool.execute(new ToolRequest("""
                {
                  "title": "Need power skill details",
                  "questions": [
                    {
                      "id": "auto_elevation",
                      "title": "Confirm auto-elevation requirement",
                      "prompt": "Is it acceptable to trigger UAC elevation automatically?",
                      "answer_type": "TEXT",
                      "allow_custom": false
                    }
                  ]
                }
                """, Path.of("."), java.util.List.of(), "thread-2", "run-2"));

        ClarificationRecord record = store.findPending("thread-2").orElseThrow();
        assertThat(record.questions()).hasSize(1);
        assertThat(record.questions().get(0).choices()).isEmpty();
        assertThat(record.questions().get(0).allowCustom()).isTrue();
    }
}
