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
                        Complex tasks must use `write_todos` to manage the plan.
                        For work that needs 3+ steps, research, coding, verification, comparison, or report output:
                        1. Call `write_todos` to create the plan before external tool work.
                        2. Submit the full todo list on every `write_todos` call.
                        3. Mark a todo completed immediately after finishing that step.
                        4. Keep at most one todo as `in_progress`.
                        5. Do not output a final answer while any todo is pending or in_progress.
                        </todo_system>
                        """;
                String baseSystem = prompt.systemPrompt();
                String updatedSystem = (baseSystem == null || baseSystem.isBlank())
                        ? todoInstruction.trim()
                        : baseSystem + "\n" + todoInstruction.trim();
                return new ModelPrompt(updatedSystem, prompt.userPrompt(), prompt.modelName());
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("\n<todo_state>\n");
                for (TodoItem item : todos) {
                    sb.append("- [").append(item.getStatus()).append("] ").append(item.getContent()).append(" (id: ").append(item.getId()).append(")\n");
                }
                sb.append("Update this full list with `write_todos` as work progresses. Do not finish until all items are completed.\n");
                sb.append("</todo_state>\n");
                
                String baseUser = prompt.userPrompt();
                String updatedUser = (baseUser == null || baseUser.isBlank())
                        ? sb.toString().trim()
                        : baseUser + "\n" + sb.toString().trim();
                return new ModelPrompt(prompt.systemPrompt(), updatedUser, prompt.modelName());
            }
        });
    }
}
