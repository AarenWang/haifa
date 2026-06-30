export type DeerFlowEventType =
  | 'RUN_STARTED'
  | 'TOOL_STARTED'
  | 'TOOL_COMPLETED'
  | 'MODEL_STARTED'
  | 'MODEL_COMPLETED'
  | 'RUN_COMPLETED'
  | 'RUN_FAILED'
  | 'RUN_CANCELLED';

export interface DeerFlowEvent {
  eventId: string;
  runId: string;
  threadId: string;
  type: DeerFlowEventType;
  content: string;
  metadata: Record<string, unknown>;
  createdAt?: string;
}

export interface RunRequest {
  threadId?: string;
  message: string;
  model?: string;
}

export interface RunResponse {
  runId: string;
  threadId: string;
  modelName: string;
  status: string;
  error: string | null;
  createdAt: string;
  updatedAt: string;
}

export type AppStatus = 'idle' | 'running' | 'completed' | 'failed' | 'stopped';

export type AppPhase =
  | 'idle'
  | 'preparing'
  | 'gathering_context'
  | 'thinking'
  | 'answering'
  | 'done';

export interface AppState {
  status: AppStatus;
  phase: AppPhase;
  runId?: string;
  threadId?: string;
  events: DeerFlowEvent[];
  finalAnswer?: string;
  error?: string;
  lastRequest?: RunRequest;
}

export type AppAction =
  | { type: 'START_RUN'; payload: RunRequest }
  | { type: 'ADD_EVENT'; payload: DeerFlowEvent }
  | { type: 'SET_FINAL_ANSWER'; payload: string }
  | { type: 'SET_ERROR'; payload: string }
  | { type: 'STOP_RUN' }
  | { type: 'CLEAR' }
  | { type: 'RE_RUN' };
