package org.wrj.haifa.ai.deerflow.agent.loop;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.todo.InMemoryTodoStore;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;

import static org.assertj.core.api.Assertions.assertThat;

class TodoCompletionGateTest {

    @Test
    void rejectsResearchFinalAnswerWhenTodoListIsMissing() {
        InMemoryTodoStore todoStore = new InMemoryTodoStore();
        DefaultAgentLoopObserver observer = new DefaultAgentLoopObserver(todoStore);

        FinalAnswerDecision decision = observer.onFinalAnswerProposed(
                runConfig("thread-missing", "run-missing"),
                "final",
                List.<AgentEvent>of(),
                new AtomicInteger(),
                1,
                0);

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.retryInstruction()).contains("write_todos");
        assertThat(decision.metadata()).containsEntry("reason", "missing_todos");
    }

    @Test
    void rejectsFinalAnswerWhenTodosAreIncomplete() {
        InMemoryTodoStore todoStore = new InMemoryTodoStore();
        todoStore.saveTodos("thread-incomplete", "run-incomplete", List.of(
                new TodoItem("t1", "Collect evidence", "completed"),
                new TodoItem("t2", "Write synthesis", "pending")
        ));
        DefaultAgentLoopObserver observer = new DefaultAgentLoopObserver(todoStore);

        FinalAnswerDecision decision = observer.onFinalAnswerProposed(
                runConfig("thread-incomplete", "run-incomplete"),
                "final",
                List.<AgentEvent>of(),
                new AtomicInteger(),
                2,
                1);

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.retryInstruction()).contains("Write synthesis");
        assertThat(decision.metadata()).containsEntry("reason", "incomplete_todos");
    }

    @Test
    void acceptsFinalAnswerWhenAllTodosAreCompleted() {
        InMemoryTodoStore todoStore = new InMemoryTodoStore();
        todoStore.saveTodos("thread-complete", "run-complete", List.of(
                new TodoItem("t1", "Collect evidence", "completed"),
                new TodoItem("t2", "Write synthesis", "completed")
        ));
        DefaultAgentLoopObserver observer = new DefaultAgentLoopObserver(todoStore);

        FinalAnswerDecision decision = observer.onFinalAnswerProposed(
                runConfig("thread-complete", "run-complete"),
                "final",
                List.<AgentEvent>of(),
                new AtomicInteger(),
                3,
                2);

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.answer()).isEqualTo("final");
    }

    private static AgentRunConfig runConfig(String threadId, String runId) {
        return new AgentRunConfig(threadId, runId, "test-model", false, false,
                4, Path.of("."), RunMode.RESEARCH, ResearchOptions.defaults(), Map.of());
    }
}
