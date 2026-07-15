package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Sinks;

import static org.assertj.core.api.Assertions.assertThat;

class ChatExecuteToolsNodeTest {

    @Test
    void executesAllowedToolAndWritesSuccessObservation() {
        AtomicInteger executions = new AtomicInteger();
        AtomicReference<ToolRequest> capturedRequest = new AtomicReference<>();
        AgentTool tool = tool("safe_tool", request -> {
            executions.incrementAndGet();
            capturedRequest.set(request);
            return ToolResult.of("safe_tool", "safe result");
        });
        ChatExecuteToolsNode node = new ChatExecuteToolsNode(
                new ToolRegistry(List.of(tool)),
                new DeerFlowProperties(),
                new ToolPolicyService(List.of(tool))
        );

        Map<String, Object> update = node.apply(state("safe_tool")).join();

        assertThat(executions).hasValue(1);
        assertThat(capturedRequest.get().threadId()).isEqualTo("thread-1");
        assertThat(capturedRequest.get().runId()).isEqualTo("run-1");
        assertThat(capturedRequest.get().uploadedFileIds()).containsExactly("file-1");
        assertObservation(update, "safe_tool", "safe result", "SUCCESS");
    }

    @Test
    void deniedToolIsNotExecutedAndWritesDeniedObservation() {
        AtomicInteger executions = new AtomicInteger();
        AgentTool tool = tool("write_file", request -> {
            executions.incrementAndGet();
            return ToolResult.of("write_file", "should not execute");
        });
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWriteFileEnabled(false);
        ChatExecuteToolsNode node = new ChatExecuteToolsNode(
                new ToolRegistry(List.of(tool)),
                properties,
                new ToolPolicyService(List.of(), properties)
        );

        Map<String, Object> update = node.apply(state("write_file")).join();

        assertThat(executions).hasValue(0);
        Map<String, Object> metadata = assertObservation(update, "write_file",
                "Error: tool denied by policy: write_file is disabled by haifa.ai.deerflow.write-file-enabled=false", "DENIED");
        assertThat(metadata)
                .containsEntry("deniedByPolicy", true)
                .containsEntry("denied", true)
                .containsEntry("reason", "write_file is disabled by haifa.ai.deerflow.write-file-enabled=false");
    }

    @Test
    void configuredRunScriptExecutesAndReceivesActiveSkills() {
        AtomicInteger executions = new AtomicInteger();
        AtomicReference<ToolRequest> capturedRequest = new AtomicReference<>();
        AgentTool tool = tool("run_script", request -> {
            executions.incrementAndGet();
            capturedRequest.set(request);
            return ToolResult.of("run_script", "script result");
        });
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        ChatExecuteToolsNode node = new ChatExecuteToolsNode(
                new ToolRegistry(List.of(tool)),
                properties,
                new ToolPolicyService(List.of(), properties)
        );

        Map<String, Object> update = node.apply(state("run_script", List.of(skill("script-skill", "run_script")))).join();

        assertThat(executions).hasValue(1);
        assertThat(capturedRequest.get().activeSkills())
                .extracting(Skill::name)
                .containsExactly("script-skill");
        assertObservation(update, "run_script", "script result", "SUCCESS");
    }

    @Test
    void missingToolWritesNotFoundObservationWithoutFailingGraph() {
        ChatExecuteToolsNode node = new ChatExecuteToolsNode(
                new ToolRegistry(List.of()),
                new DeerFlowProperties(),
                new ToolPolicyService(List.of())
        );

        Map<String, Object> update = node.apply(state("missing_tool")).join();

        assertObservation(update, "missing_tool", "Error: tool not found: missing_tool", "NOT_FOUND");
    }

    @Test
    void toolExceptionWritesFailedObservationWithoutFailingGraph() {
        AgentTool tool = tool("unstable_tool", request -> {
            throw new IllegalStateException("boom");
        });
        ChatExecuteToolsNode node = new ChatExecuteToolsNode(
                new ToolRegistry(List.of(tool)),
                new DeerFlowProperties(),
                new ToolPolicyService(List.of(tool))
        );

        Map<String, Object> update = node.apply(state("unstable_tool")).join();

        Map<String, Object> metadata = assertObservation(update, "unstable_tool",
                "Error executing tool unstable_tool: boom", "FAILED");
        assertThat(metadata).containsEntry("errorType", "IllegalStateException");
    }

    @Test
    void explicitToolFailureWritesFailedObservation() {
        AgentTool tool = tool("provider_tool", request ->
                ToolResult.failed("provider_tool", "provider returned exit code 7"));
        ChatExecuteToolsNode node = new ChatExecuteToolsNode(
                new ToolRegistry(List.of(tool)), new DeerFlowProperties(), new ToolPolicyService(List.of(tool)));

        Map<String, Object> update = node.apply(state("provider_tool")).join();

        assertObservation(update, "provider_tool", "provider returned exit code 7", "FAILED");
    }

    @Test
    void publishesTodoSnapshotImmediatelyAfterWriteTodosMutation() {
        Map<String, Object> snapshot = Map.of(
                "threadId", "thread-1",
                "runId", "run-1",
                "revision", 1,
                "operation", "created",
                "todos", List.of(Map.of("id", "todo-1", "content", "First task", "status", "pending")),
                "summary", Map.of("total", 1, "pending", 1)
        );
        AgentTool tool = tool("write_todos", request -> ToolResult.of("write_todos", "Todo list created: 1 item(s)",
                Map.of("todoOperation", "created", "snapshot", snapshot)));
        ChatExecuteToolsNode node = new ChatExecuteToolsNode(
                new ToolRegistry(List.of(tool)),
                new DeerFlowProperties(),
                new ToolPolicyService(List.of(tool))
        );
        Sinks.Many<AgentEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        GraphEventRegistry.register("run-1", sink, new AtomicInteger());
        try {
            node.apply(state("write_todos")).join();
            sink.tryEmitComplete();

            List<AgentEvent> events = sink.asFlux().collectList().block();
            assertThat(events).extracting(AgentEvent::type)
                    .containsSubsequence(AgentEventType.TOOL_STARTED, AgentEventType.TOOL_COMPLETED,
                            AgentEventType.TODO_CREATED);
            AgentEvent todoEvent = events.stream()
                    .filter(event -> event.type() == AgentEventType.TODO_CREATED)
                    .findFirst()
                    .orElseThrow();
            assertThat(todoEvent.metadata()).containsEntry("snapshot", snapshot);
        } finally {
            GraphEventRegistry.deregister("run-1");
        }
    }

    private static OverAllState state(String toolName) {
        return state(toolName, List.of());
    }

    private static OverAllState state(String toolName, List<Skill> activeSkills) {
        return new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-1",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                AgentGraphStateKeys.MODE, RunMode.CHAT.name(),
                AgentGraphStateKeys.ACTIVE_SKILLS, activeSkills.stream()
                        .map(skill -> Map.of(
                                "name", skill.name(),
                                "description", skill.description(),
                                "source", skill.source(),
                                "allowedTools", List.copyOf(skill.allowedTools()),
                                "activationHints", skill.activationHints()
                        ))
                        .toList(),
                AgentGraphStateKeys.UPLOADED_FILE_IDS, List.of("file-1"),
                AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of(Map.of(
                        "id", "call-1",
                        "name", toolName,
                        "arguments", "{\"value\":1}"
                ))
        ));
    }

    private static Skill skill(String name, String allowedTool) {
        return new Skill(name, name + " description", "test", "", Map.of(), Set.of(allowedTool), List.of());
    }

    private static Map<String, Object> assertObservation(Map<String, Object> update, String toolName,
                                                         String content, String status) {
        List<Map<String, Object>> messages = (List<Map<String, Object>>) update.get(AgentGraphStateKeys.MESSAGE_WINDOW);
        assertThat(messages).hasSize(1);
        Map<String, Object> message = messages.get(0);
        assertThat(message)
                .containsEntry("role", ModelMessage.Role.TOOL.name())
                .containsEntry("name", toolName)
                .containsEntry("toolCallId", "call-1")
                .containsEntry("content", content);
        Map<String, Object> metadata = (Map<String, Object>) message.get("metadata");
        assertThat(metadata)
                .containsEntry("status", status)
                .containsEntry("toolName", toolName)
                .containsEntry("toolCallId", "call-1");

        List<Map<String, Object>> results = (List<Map<String, Object>>) update.get(AgentGraphStateKeys.TOOL_RESULTS);
        assertThat(results).hasSize(1);
        assertThat(results.get(0))
                .containsEntry("toolName", toolName)
                .containsEntry("result", content);
        assertThat((List<?>) update.get(AgentGraphStateKeys.PENDING_TOOL_CALLS)).isEmpty();
        return metadata;
    }

    private static AgentTool tool(String name, ToolBehavior behavior) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return name + " description";
            }

            @Override
            public boolean supports(String userMessage) {
                return true;
            }

            @Override
            public ToolResult execute(ToolRequest request) {
                return behavior.execute(request);
            }
        };
    }

    private interface ToolBehavior {
        ToolResult execute(ToolRequest request);
    }
}
