import type { AppState, AppAction, AppPhase, DeerFlowEvent, TodoSnapshot, AgentTodoItem } from '../types';

function getPhaseFromEvent(type: string): AppPhase {
  switch (type) {
    case 'RUN_STARTED':
      return 'preparing';
    case 'TOOL_STARTED':
    case 'TOOL_COMPLETED':
    case 'TOOL_DENIED':
      return 'gathering_context';
    case 'MODEL_STARTED':
      return 'thinking';
    case 'MODEL_COMPLETED':
      return 'answering';
    case 'RUN_COMPLETED':
      return 'done';
    // Research events map to similar phases
    case 'RESEARCH_PLAN_CREATED':
      return 'preparing';
    case 'RESEARCH_STEP_COMPLETED':
    case 'SOURCE_FOUND':
    case 'SOURCE_FETCHED':
    case 'EVIDENCE_EXTRACTED':
      return 'gathering_context';
    case 'QUALITY_GATE_STARTED':
    case 'QUALITY_GATE_PASSED':
    case 'QUALITY_GATE_FAILED':
      return 'thinking';
    case 'REPORT_STARTED':
    case 'REPORT_COMPLETED':
    case 'ARTIFACT_CREATED':
      return 'answering';
    case 'TODO_CREATED':
    case 'TODO_UPDATED':
      return 'gathering_context';
    case 'TODO_GATE_BLOCKED':
    case 'TODO_INCOMPLETE':
      return 'thinking';
    case 'RUN_SUSPENDED':
      return 'idle';
    default:
      return 'idle';
  }
}

export const initialState: AppState = {
  status: 'idle',
  phase: 'idle',
  threads: [],
  messages: [],
  events: [],
  researchSources: [],
  evidenceItems: [],
  artifacts: [],
  uploads: [],
  selectedUploadIds: [],
  runHistory: [],
  
  // New unified tracking states
  workItems: [],
  claims: [],
  citations: [],
  budgetLedger: null,
  qualityAssessment: null,
  skillActivations: [],
  threadFiles: [],
  sources: [],
};

export function deerflowReducer(state: AppState, action: AppAction): AppState {
  switch (action.type) {
    case 'START_RUN': {
      const entry: AppState['runHistory'][0] = {
        runId: '',
        threadId: action.payload.threadId,
        message: action.payload.message,
        status: 'running',
        startedAt: new Date().toISOString(),
        model: action.payload.model,
      };
      return {
        ...initialState,
        status: 'running',
        phase: 'preparing',
        lastRequest: action.payload,
        threadId: action.payload.threadId,
        threads: state.threads,
        messages: state.messages,
        researchSources: [],
        evidenceItems: [],
        runObservability: undefined,
        artifacts: state.artifacts,
        uploads: state.uploads,
        selectedUploadIds: state.selectedUploadIds,
        runHistory: [entry, ...state.runHistory],
      };
    }
    case 'ADD_EVENT': {
      const event = action.payload;
      const isModelDelta = event.type === 'MODEL_DELTA';
      const isFinalModelDelta = isModelDelta && !!(event.metadata && (event.metadata.modelDurationMs !== undefined || event.metadata.persistAssistantToolCalls));
      const shouldAddEvent = !isModelDelta || isFinalModelDelta;
      const nextEvents = shouldAddEvent ? [...state.events, event] : state.events;
      const nextPhase = getPhaseFromEvent(event.type);
      let nextStatus: AppState['status'] = state.status;
      let nextFinalAnswer = state.finalAnswer;
      let nextError = state.error;
      let nextRunId = state.runId;
      let nextThreadId = state.threadId;
      let nextTodoSnapshot = state.todoSnapshot;
      let nextTodoGateBlocked = state.todoGateBlocked;
      let nextTodoGateMessage = state.todoGateMessage;
      let nextMessages = [...state.messages];
      if (event.type === 'MODEL_DELTA') {
        const chunk = event.content || '';
        const runId = event.runId;
        const isFinal = !!(event.metadata && (event.metadata.modelDurationMs !== undefined || event.metadata.persistAssistantToolCalls));
        const existingAssistantIdx = nextMessages.findIndex(
          (m) => m.role === 'ASSISTANT' && m.runId === runId
        );
        if (existingAssistantIdx >= 0) {
          const existing = nextMessages[existingAssistantIdx];
          nextMessages[existingAssistantIdx] = {
            ...existing,
            content: isFinal ? chunk : existing.content + chunk,
          };
        } else {
          nextMessages.push({
            messageId: 'temp-' + runId,
            threadId: event.threadId || nextThreadId || '',
            runId: runId,
            role: 'ASSISTANT',
            content: chunk,
            metadata: {},
            createdAt: new Date().toISOString(),
          });
        }
      }

      if (event.type === 'RUN_COMPLETED') {
        nextStatus = 'completed';
      } else if (event.type === 'RUN_FAILED') {
        nextStatus = 'failed';
        nextError = event.content || 'Run failed';
      } else if (event.type === 'RUN_CANCELLED') {
        nextStatus = 'stopped';
      } else if (event.type === 'RUN_SUSPENDED') {
        nextStatus = 'suspended';
      }

      if (event.type === 'MODEL_COMPLETED') {
        nextFinalAnswer = event.content;
      }

      if (event.runId) {
        nextRunId = event.runId;
      }
      if (event.threadId) {
        nextThreadId = event.threadId;
      }

      const eventTodoSnapshot = snapshotFromEvent(event);
      if (eventTodoSnapshot) {
        nextTodoSnapshot = eventTodoSnapshot;
      }
      if (event.type === 'TODO_GATE_BLOCKED' || event.type === 'TODO_INCOMPLETE') {
        nextTodoGateBlocked = true;
        nextTodoGateMessage = event.content;
      } else if (event.type === 'TODO_CREATED' || event.type === 'TODO_UPDATED') {
        nextTodoGateBlocked = false;
        nextTodoGateMessage = undefined;
      }

      // Update runHistory entry status if matching runId
      const nextRunHistory = state.runHistory.map((h) => {
        if (h.runId === event.runId || (h.runId === '' && !state.runId)) {
          const updated: typeof h = { ...h };
          if (event.type === 'RUN_COMPLETED') {
            updated.status = 'completed';
            updated.completedAt = new Date().toISOString();
          } else if (event.type === 'RUN_FAILED') {
            updated.status = 'failed';
            updated.completedAt = new Date().toISOString();
          } else if (event.type === 'RUN_CANCELLED') {
            updated.status = 'stopped';
            updated.completedAt = new Date().toISOString();
          } else if (event.type === 'RUN_SUSPENDED') {
            updated.status = 'suspended';
          }
          if (event.runId) updated.runId = event.runId;
          return updated;
        }
        return h;
      });

      return {
        ...state,
        status: nextStatus,
        phase: nextPhase,
        events: nextEvents,
        messages: nextMessages,
        runId: nextRunId,
        threadId: nextThreadId,
        finalAnswer: nextFinalAnswer,
        error: nextError,
        todoSnapshot: nextTodoSnapshot,
        todoGateBlocked: nextTodoGateBlocked,
        todoGateMessage: nextTodoGateMessage,
        runHistory: nextRunHistory,
      };
    }
    case 'SET_FINAL_ANSWER':
      return {
        ...state,
        finalAnswer: action.payload,
      };
    case 'SET_ERROR':
      return {
        ...state,
        status: 'failed',
        phase: 'idle',
        error: action.payload,
      };
    case 'SET_RESEARCH_SOURCES':
      return {
        ...state,
        researchSources: action.payload,
      };
    case 'SET_EVIDENCE_ITEMS':
      return {
        ...state,
        evidenceItems: action.payload,
      };
    case 'SET_WORK_ITEMS':
      return {
        ...state,
        workItems: action.payload,
      };
    case 'SET_CLAIMS':
      return {
        ...state,
        claims: action.payload,
      };
    case 'SET_CITATIONS':
      return {
        ...state,
        citations: action.payload,
      };
    case 'SET_BUDGET_LEDGER':
      return {
        ...state,
        budgetLedger: action.payload,
      };
    case 'SET_QUALITY_ASSESSMENT':
      return {
        ...state,
        qualityAssessment: action.payload,
      };
    case 'SET_SKILL_ACTIVATIONS':
      return {
        ...state,
        skillActivations: action.payload,
      };
    case 'SET_THREAD_FILES':
      return {
        ...state,
        threadFiles: action.payload,
      };
    case 'SET_UNIFIED_SOURCES':
      return {
        ...state,
        sources: action.payload,
      };
    case 'SET_RESEARCH_PLAN':
      return {
        ...state,
        researchPlan: action.payload,
      };
    case 'SET_RESEARCH_PROGRESS':
      return {
        ...state,
        researchProgress: action.payload,
      };
    case 'SET_QUALITY_GATE':
      return {
        ...state,
        qualityGate: action.payload,
      };
    case 'SET_RUN_OBSERVABILITY':
      return {
        ...state,
        runObservability: action.payload,
      };
    case 'SET_ARTIFACTS':
      return {
        ...state,
        artifacts: action.payload,
      };
    case 'SET_THREAD_ID':
      return {
        ...state,
        threadId: action.payload,
      };
    case 'SET_THREADS':
      return {
        ...state,
        threads: action.payload,
      };
    case 'SET_MESSAGES':
      return {
        ...state,
        messages: action.payload,
      };
    case 'SET_EVENTS': {
      const replayedTodo = replayTodoState(action.payload);
      return {
        ...state,
        events: action.payload,
        todoSnapshot: replayedTodo.snapshot,
        todoGateBlocked: replayedTodo.gateBlocked,
        todoGateMessage: replayedTodo.gateMessage,
      };
    }
    case 'SET_TODO_SNAPSHOT':
      return {
        ...state,
        todoSnapshot: action.payload,
        todoGateBlocked: action.payload ? state.todoGateBlocked : false,
        todoGateMessage: action.payload ? state.todoGateMessage : undefined,
      };
    case 'SET_TODO_GATE_BLOCKED':
      return {
        ...state,
        todoGateBlocked: action.payload.blocked,
        todoGateMessage: action.payload.message,
      };
    case 'SET_STATUS':
      return {
        ...state,
        status: action.payload,
      };
    case 'SET_LAST_REQUEST':
      return {
        ...state,
        lastRequest: action.payload,
      };
    case 'STOP_RUN':
      return {
        ...state,
        status: 'stopped',
        phase: 'idle',
      };
    case 'CLEAR':
      return {
        ...initialState,
        lastRequest: state.lastRequest,
        threads: state.threads,
        messages: state.threadId ? state.messages : [],
        researchSources: state.threadId ? state.researchSources : [],
        evidenceItems: state.threadId ? state.evidenceItems : [],
        artifacts: state.threadId ? state.artifacts : [],
        uploads: state.uploads,
        selectedUploadIds: state.selectedUploadIds,
        runHistory: state.runHistory,
      };
    case 'RE_RUN':
      if (!state.lastRequest) return state;
      return {
        ...initialState,
        status: 'running',
        phase: 'preparing',
        lastRequest: state.lastRequest,
        threadId: state.lastRequest.threadId,
        threads: state.threads,
        messages: state.messages,
        researchSources: state.researchSources,
        evidenceItems: state.evidenceItems,
        artifacts: state.artifacts,
        uploads: state.uploads,
        selectedUploadIds: state.selectedUploadIds,
        runHistory: state.runHistory,
      };
    case 'SET_UPLOADS':
      return {
        ...state,
        uploads: action.payload,
        selectedUploadIds: action.payload.length === 0 ? [] : state.selectedUploadIds,
      };
    case 'ADD_UPLOAD':
      return {
        ...state,
        uploads: [action.payload, ...state.uploads],
      };
    case 'REMOVE_UPLOAD':
      return {
        ...state,
        uploads: state.uploads.filter((u) => u.fileId !== action.payload),
        selectedUploadIds: state.selectedUploadIds.filter(
          (id) => id !== action.payload
        ),
      };
    case 'TOGGLE_UPLOAD_SELECTION': {
      const id = action.payload;
      const isSelected = state.selectedUploadIds.includes(id);
      return {
        ...state,
        selectedUploadIds: isSelected
          ? state.selectedUploadIds.filter((x) => x !== id)
          : [...state.selectedUploadIds, id],
      };
    }
    case 'ADD_RUN_HISTORY':
      return {
        ...state,
        runHistory: [action.payload, ...state.runHistory],
      };
    default:
      return state;
  }
}

function replayTodoState(events: DeerFlowEvent[]): {
  snapshot?: TodoSnapshot;
  gateBlocked?: boolean;
  gateMessage?: string;
} {
  let snapshot: TodoSnapshot | undefined;
  let gateBlocked = false;
  let gateMessage: string | undefined;
  for (const event of events) {
    const eventSnapshot = snapshotFromEvent(event);
    if (eventSnapshot) {
      snapshot = eventSnapshot;
    }
    if (event.type === 'TODO_GATE_BLOCKED' || event.type === 'TODO_INCOMPLETE') {
      gateBlocked = true;
      gateMessage = event.content;
    } else if (event.type === 'TODO_CREATED' || event.type === 'TODO_UPDATED') {
      gateBlocked = false;
      gateMessage = undefined;
    }
  }
  return { snapshot, gateBlocked, gateMessage };
}

function snapshotFromEvent(event: DeerFlowEvent): TodoSnapshot | undefined {
  if (
    event.type !== 'TODO_CREATED' &&
    event.type !== 'TODO_UPDATED' &&
    event.type !== 'TODO_GATE_BLOCKED' &&
    event.type !== 'TODO_INCOMPLETE'
  ) {
    return undefined;
  }
  return normalizeTodoSnapshot(event.metadata?.snapshot, event.threadId, event.runId);
}

function normalizeTodoSnapshot(value: unknown, fallbackThreadId?: string, fallbackRunId?: string): TodoSnapshot | undefined {
  if (!value || typeof value !== 'object') {
    return undefined;
  }
  const record = value as Record<string, unknown>;
  const todos = Array.isArray(record.todos)
    ? record.todos.map(normalizeTodoItem).filter((todo): todo is AgentTodoItem => !!todo)
    : [];
  const summaryRecord = record.summary && typeof record.summary === 'object'
    ? record.summary as Record<string, unknown>
    : {};
  return {
    threadId: stringValue(record.threadId) || fallbackThreadId || '',
    runId: stringValue(record.runId) || fallbackRunId || '',
    revision: numberValue(record.revision),
    operation: stringValue(record.operation),
    todos,
    summary: {
      total: numberValue(summaryRecord.total, todos.length),
      pending: numberValue(summaryRecord.pending, countStatus(todos, 'pending')),
      inProgress: numberValue(summaryRecord.inProgress, countStatus(todos, 'in_progress')),
      completed: numberValue(summaryRecord.completed, countStatus(todos, 'completed')),
      cancelled: numberValue(summaryRecord.cancelled, countStatus(todos, 'cancelled')),
    },
    updatedAt: stringValue(record.updatedAt),
  };
}

function normalizeTodoItem(value: unknown): AgentTodoItem | undefined {
  if (!value || typeof value !== 'object') {
    return undefined;
  }
  const record = value as Record<string, unknown>;
  const id = stringValue(record.id);
  const content = stringValue(record.content);
  const rawStatus = stringValue(record.status);
  if (!id || !content || !rawStatus) {
    return undefined;
  }
  const status = rawStatus === 'pending' || rawStatus === 'in_progress' || rawStatus === 'completed' || rawStatus === 'cancelled'
    ? rawStatus
    : 'pending';
  return {
    id,
    content,
    status,
    priority: stringValue(record.priority) || null,
    evidence: stringValue(record.evidence) || null,
    orderIndex: numberValue(record.orderIndex),
    createdAt: stringValue(record.createdAt) || null,
    updatedAt: stringValue(record.updatedAt) || null,
  };
}

function stringValue(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function numberValue(value: unknown, fallback = 0): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function countStatus(todos: AgentTodoItem[], status: AgentTodoItem['status']): number {
  return todos.filter((todo) => todo.status === status).length;
}
