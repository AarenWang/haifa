package org.wrj.haifa.ai.deerflow.graph.state;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentGraphStateFactoryTest {

    @Test
    void createsBoundedInitialStateFromRuntimeInputs() {
        AgentGraphStateFactory factory = new AgentGraphStateFactory(2, 6);
        AgentRunConfig config = new AgentRunConfig("thread-1", "run-1", "model-a", false, false, 4,
                Path.of("."), RunMode.RESEARCH, ResearchOptions.defaults(), Map.of());
        AgentRequest request = new AgentRequest("thread-1", "hello graph runtime", "model-a",
                List.of("file-1"), RunMode.RESEARCH, ResearchOptions.defaults(), "user-1",
                Map.of("requestId", "req-1"));
        List<MessageRecord> history = List.of(
                message("m1", "older content"),
                message("m2", "middle content"),
                message("m3", "newer content")
        );

        Map<String, Object> state = factory.create(config, request, history,
                new ModelPrompt("system prompt", "user prompt", "model-a"));
        AgentGraphStateView view = AgentGraphStateView.of(state);

        assertThat(view.runId()).isEqualTo("run-1");
        assertThat(view.threadId()).isEqualTo("thread-1");
        assertThat(view.mode()).isEqualTo(RunMode.RESEARCH);
        assertThat(view.userMessage()).isEqualTo("hello ");
        assertThat(view.modelName()).isEqualTo("model-a");
        assertThat(view.messageWindow())
                .extracting(message -> message.get("messageId"))
                .containsExactly("m2", "m3");
        assertThat(view.messageWindow())
                .extracting(message -> message.get("content"))
                .containsExactly("middle", "newer ");
        assertThat(view.map(AgentGraphStateKeys.MODEL_PROMPT).get("systemPrompt")).isEqualTo("system");
        assertThat(view.map(AgentGraphStateKeys.RUN_PROMPT_BASE)).isEmpty();
        assertThat(state.get(AgentGraphStateKeys.RUN_PREPARED)).isEqualTo(false);
        assertThat(state.get(AgentGraphStateKeys.PROMPT_REVISION)).isEqualTo(0);
        assertThat(view.map(AgentGraphStateKeys.REQUEST_METADATA).get("requestId")).isEqualTo("req-1");
        assertThat(view.activeSkills()).isEmpty();
        assertThat(view.toolCalls()).isEmpty();
        assertThat(view.finalAnswer()).isEmpty();
    }

    @Test
    void storesActiveSkillsAsCheckpointFriendlyState() {
        AgentGraphStateFactory factory = new AgentGraphStateFactory();
        AgentRunConfig config = new AgentRunConfig("thread-1", "run-1", "model-a", false, false, 4,
                Path.of("."), RunMode.CHAT, ResearchOptions.defaults(), Map.of());
        AgentRequest request = new AgentRequest("thread-1", "hello", "model-a");

        Map<String, Object> state = factory.create(config, request, List.of(),
                new ModelPrompt("system prompt", "user prompt", "model-a"),
                List.of(new Skill("script-skill", "Script skill", "test", "", Map.of(),
                        Set.of("run_script"), List.of("script"))));

        assertThat(AgentGraphStateView.of(state).activeSkills())
                .hasSize(1)
                .first()
                .satisfies(skill -> {
                    assertThat(skill.name()).isEqualTo("script-skill");
                    assertThat(skill.allowedTools()).containsExactly("run_script");
                });
    }

    @Test
    void viewFallsBackToSafeDefaultsForMissingOrInvalidValues() {
        AgentGraphStateView view = AgentGraphStateView.of(Map.of(
                AgentGraphStateKeys.MODE, "not-a-mode",
                AgentGraphStateKeys.MESSAGE_WINDOW, "wrong-type"
        ));

        assertThat(view.runId()).isEmpty();
        assertThat(view.mode()).isEqualTo(RunMode.CHAT);
        assertThat(view.messageWindow()).isEmpty();
    }

    private static MessageRecord message(String id, String content) {
        return new MessageRecord(id, "thread-1", "run-0", MessageRole.USER, content, Map.of(),
                Instant.parse("2026-07-01T00:00:00Z"));
    }
}
