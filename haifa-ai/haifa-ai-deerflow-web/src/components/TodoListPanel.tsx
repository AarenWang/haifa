import { Ban, CheckCircle2, Circle, ListTodo, PlayCircle, AlertTriangle } from 'lucide-react';
import type { AgentTodoItem, TodoSnapshot } from '../types';

interface TodoListPanelProps {
  snapshot?: TodoSnapshot;
  gateBlocked?: boolean;
  gateMessage?: string;
}

export default function TodoListPanel({ snapshot, gateBlocked, gateMessage }: TodoListPanelProps) {
  if (!snapshot || !hasActiveTodos(snapshot.todos)) {
    return null;
  }

  const orderedTodos = [...snapshot.todos].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
  const completed = snapshot.summary.completed + snapshot.summary.cancelled;
  const total = snapshot.summary.total || orderedTodos.length;
  const progress = total > 0 ? Math.round((completed / total) * 100) : 0;

  return (
    <section className="todo-panel" aria-label="Agent todo list">
      <div className="todo-panel-header">
        <div className="todo-panel-title">
          <ListTodo size={15} />
          <span>Todo List</span>
        </div>
        <span className="todo-progress-count">{completed}/{total}</span>
      </div>
      <div className="todo-progress-track" aria-hidden="true">
        <div className="todo-progress-fill" style={{ width: `${progress}%` }} />
      </div>
      {gateBlocked && gateMessage && (
        <div className="todo-gate-warning">
          <AlertTriangle size={14} />
          <span>{gateMessage}</span>
        </div>
      )}
      <div className="todo-list">
        {orderedTodos.map((todo) => (
          <div key={todo.id} className={`todo-row ${statusClass(todo.status)}`}>
            <span className="todo-status-icon">{statusIcon(todo.status)}</span>
            <span className="todo-content" title={todo.content}>{todo.content}</span>
            <span className="todo-status-label">{statusLabel(todo.status)}</span>
          </div>
        ))}
      </div>
    </section>
  );
}

function hasActiveTodos(todos: AgentTodoItem[]): boolean {
  return todos.some((todo) => todo.status === 'pending' || todo.status === 'in_progress');
}

function statusClass(status: AgentTodoItem['status']) {
  return status.replace(/_/g, '-');
}

function statusIcon(status: AgentTodoItem['status']) {
  switch (status) {
    case 'completed':
      return <CheckCircle2 size={14} />;
    case 'in_progress':
      return <PlayCircle size={14} />;
    case 'cancelled':
      return <Ban size={14} />;
    default:
      return <Circle size={14} />;
  }
}

function statusLabel(status: AgentTodoItem['status']) {
  switch (status) {
    case 'in_progress':
      return 'Doing';
    case 'completed':
      return 'Done';
    case 'cancelled':
      return 'Cancelled';
    default:
      return 'Pending';
  }
}
