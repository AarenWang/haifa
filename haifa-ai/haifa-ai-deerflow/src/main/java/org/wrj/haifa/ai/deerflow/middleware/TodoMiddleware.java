package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;
import reactor.core.publisher.Mono;

/**
 * Middleware that appends instructions for write_todos if none exist,
 * or formats and injects active todo list status as a system reminder.
 */
@Component
@MiddlewareOrder(25)
public class TodoMiddleware implements AgentMiddleware {

    private final TodoStore todoStore;

    public TodoMiddleware(TodoStore todoStore) {
        this.todoStore = todoStore;
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        String threadId = context.config().threadId();
        String runId = context.config().runId();
        List<TodoItem> todos = todoStore.listTodos(threadId, runId);

        return next.next(context).map(prompt -> {
            if (todos.isEmpty()) {
                String todoInstruction = """
                        
                        <todo_system>
                        You have access to the `write_todos` tool to help you manage and track complex multi-step objectives.
                        - Create a plan using `write_todos` for any complex work sessions (3+ steps).
                        - Mark todos as completed IMMEDIATELY after finishing each step.
                        - Keep exactly one task as `in_progress` at any time.
                        </todo_system>
                        """;
                String baseSystem = prompt.systemPrompt();
                String updatedSystem = (baseSystem == null || baseSystem.isBlank())
                        ? todoInstruction.trim()
                        : baseSystem + "\n" + todoInstruction.trim();
                return new ModelPrompt(updatedSystem, prompt.userPrompt(), prompt.modelName());
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("\n<system_reminder>\n");
                sb.append("Your todo list is still active. Current state:\n");
                for (TodoItem item : todos) {
                    sb.append("- [").append(item.getStatus()).append("] ").append(item.getContent()).append(" (id: ").append(item.getId()).append(")\n");
                }
                sb.append("Call `write_todos` to update task statuses as you progress.\n");
                sb.append("</system_reminder>\n");
                
                String baseUser = prompt.userPrompt();
                String updatedUser = (baseUser == null || baseUser.isBlank())
                        ? sb.toString().trim()
                        : baseUser + "\n" + sb.toString().trim();
                return new ModelPrompt(prompt.systemPrompt(), updatedUser, prompt.modelName());
            }
        });
    }
}
