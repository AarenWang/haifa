package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.subagent.SubagentRegistry;
import org.wrj.haifa.ai.deerflow.subagent.SubagentResult;
import org.wrj.haifa.ai.deerflow.subagent.SubagentRuntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskToolTest {

    private SubagentRuntime subagentRuntime;
    private SubagentRegistry subagentRegistry;
    private TaskTool taskTool;

    @BeforeEach
    void setUp() {
        subagentRuntime = mock(SubagentRuntime.class);
        subagentRegistry = mock(SubagentRegistry.class);
        taskTool = new TaskTool(subagentRuntime, subagentRegistry);
    }

    @Test
    void executeReturnsErrorWhenArgumentsAreMissing() {
        ToolRequest request = new ToolRequest("", Path.of("."));
        ToolResult result = taskTool.execute(request);

        assertThat(result.content()).contains("arguments JSON required");
    }

    @Test
    void executeReturnsErrorWhenPromptIsMissing() {
        String arguments = "{\"description\": \"test subagent\"}";
        ToolRequest request = new ToolRequest(arguments, Path.of("."));
        ToolResult result = taskTool.execute(request);

        assertThat(result.content()).contains("prompt is required");
    }

    @Test
    void executeReturnsErrorWhenSubagentTypeIsUnknown() {
        when(subagentRegistry.isAvailable("unknown-type")).thenReturn(false);
        when(subagentRegistry.getAvailableNames()).thenReturn(List.of("general-purpose", "bash"));

        String arguments = "{\"description\": \"test\", \"prompt\": \"do research\", \"subagent_type\": \"unknown-type\"}";
        ToolRequest request = new ToolRequest(arguments, Path.of("."));
        ToolResult result = taskTool.execute(request);

        assertThat(result.content()).contains("Unknown subagent type 'unknown-type'");
    }

    @Test
    void executeDelegatesToSubagentRuntimeSuccessfully() {
        when(subagentRegistry.isAvailable("general-purpose")).thenReturn(true);

        SubagentResult subResult = new SubagentResult(
                "sub-1", "run-1", "COMPLETED", "Summary findings",
                List.of("evidence-1"), List.of("source-1"), Map.of("estimated_total_tokens", 100),
                "", 2500
        );

        when(subagentRuntime.execute(
                eq("test-desc"), eq("do research prompt"), eq("general-purpose"),
                eq("thread-1"), eq("run-1"), eq("qwen-plus"),
                isNull(), isNull(), isNull(), isNull(),
                eq(RunMode.RESEARCH), any()
        )).thenReturn(subResult);

        String arguments = "{\"description\": \"test-desc\", \"prompt\": \"do research prompt\", \"subagent_type\": \"general-purpose\"}";
        ToolRequest request = new ToolRequest(arguments, Path.of("."), List.of(), "thread-1", "run-1",
                RunMode.RESEARCH, List.of(), "qwen-plus");
        ToolResult result = taskTool.execute(request);

        assertThat(result.content()).contains("Summary findings")
                .contains("Sources found: source-1")
                .contains("Evidence extracted: evidence-1");

        assertThat(result.metadata()).containsEntry("status", "COMPLETED")
                .containsEntry("taskId", "sub-1")
                .containsEntry("parentRunId", "run-1")
                .containsEntry("sourceIds", List.of("source-1"))
                .containsEntry("evidenceIds", List.of("evidence-1"))
                .containsEntry("durationMs", 2500L);
    }

    @Test
    void executePassesParentModeModelAndActiveSkillsToSubagentRuntime() {
        when(subagentRegistry.isAvailable("general-purpose")).thenReturn(true);
        Skill skill = new Skill("shell", "Shell access", "custom", "md", Map.of(), Set.of("bash"));
        SubagentResult subResult = SubagentResult.success("sub-chat", "run-chat", "done",
                List.of(), List.of(), Map.of(), 10);
        when(subagentRuntime.execute(
                eq("chat-desc"), eq("chat prompt"), eq("general-purpose"),
                eq("thread-chat"), eq("run-chat"), eq("qwen-plus"),
                isNull(), isNull(), isNull(), isNull(),
                eq(RunMode.CHAT), eq(List.of(skill))
        )).thenReturn(subResult);

        String arguments = "{\"description\": \"chat-desc\", \"prompt\": \"chat prompt\", \"subagent_type\": \"general-purpose\"}";
        ToolRequest request = new ToolRequest(arguments, Path.of("."), List.of(), "thread-chat", "run-chat",
                RunMode.CHAT, List.of(skill), "qwen-plus");

        ToolResult result = taskTool.execute(request);

        assertThat(result.content()).contains("done");
        verify(subagentRuntime).execute(
                eq("chat-desc"), eq("chat prompt"), eq("general-purpose"),
                eq("thread-chat"), eq("run-chat"), eq("qwen-plus"),
                isNull(), isNull(), isNull(), isNull(),
                eq(RunMode.CHAT), eq(List.of(skill))
        );
    }

    @Test
    void executeTreatsInheritModelAsParentModelRequestMetadata() {
        when(subagentRegistry.isAvailable("general-purpose")).thenReturn(true);
        SubagentResult subResult = SubagentResult.success("sub-inherit", "run-1", "done",
                List.of(), List.of(), Map.of(), 10);
        when(subagentRuntime.execute(
                eq("inherit-desc"), eq("inherit prompt"), eq("general-purpose"),
                eq("thread-1"), eq("run-1"), eq("qwen-plus"),
                isNull(), isNull(), isNull(), isNull(),
                eq(RunMode.RESEARCH), any()
        )).thenReturn(subResult);

        String arguments = "{\"description\": \"inherit-desc\", \"prompt\": \"inherit prompt\", "
                + "\"subagent_type\": \"general-purpose\", \"model\": \"inherit\"}";
        ToolRequest request = new ToolRequest(arguments, Path.of("."), List.of(), "thread-1", "run-1",
                RunMode.RESEARCH, List.of(), "qwen-plus");

        ToolResult result = taskTool.execute(request);

        assertThat(result.content()).contains("done");
        verify(subagentRuntime).execute(
                eq("inherit-desc"), eq("inherit prompt"), eq("general-purpose"),
                eq("thread-1"), eq("run-1"), eq("qwen-plus"),
                isNull(), isNull(), isNull(), isNull(),
                eq(RunMode.RESEARCH), any()
        );
    }
}
