export type DeerFlowEventType =
  | 'RUN_STARTED'
  | 'TOOL_STARTED'
  | 'TOOL_COMPLETED'
  | 'MODEL_STARTED'
  | 'MODEL_COMPLETED'
  | 'RUN_COMPLETED'
  | 'RUN_FAILED'
  | 'RUN_CANCELLED'
  | 'RESEARCH_PLAN_CREATED'
  | 'RESEARCH_DIMENSION_STARTED'
  | 'RESEARCH_DIMENSION_COMPLETED'
  | 'SOURCE_FOUND'
  | 'SOURCE_FETCHED'
  | 'EVIDENCE_EXTRACTED'
  | 'QUALITY_GATE_STARTED'
  | 'QUALITY_GATE_PASSED'
  | 'QUALITY_GATE_FAILED'
  | 'REPORT_STARTED'
  | 'REPORT_COMPLETED'
  | 'ARTIFACT_CREATED'
  | 'SUBAGENT_STARTED'
  | 'SUBAGENT_COMPLETED'
  | 'MODEL_DELTA'
  | 'TOOL_CALL_REQUESTED'
  | 'RESEARCH_STEP_COMPLETED';

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
  uploadedFileIds?: string[];
  mode?: 'chat' | 'research';
  researchOptions?: {
    depth?: 'quick' | 'standard' | 'deep';
    timeWindow?: 'latest' | 'last_30_days' | 'last_year' | 'all_time';
    maxSources?: number;
    requireCitations?: boolean;
    outputFormat?: 'answer' | 'report';
  };
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

export type ThreadStatus = 'ACTIVE' | 'ARCHIVED';
export type MessageRole = 'USER' | 'ASSISTANT' | 'TOOL' | 'SYSTEM';

export interface ThreadRecord {
  threadId: string;
  title: string;
  status: ThreadStatus;
  metadata: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface ThreadListResponse {
  threads: ThreadRecord[];
}

export interface MessageRecord {
  messageId: string;
  threadId: string;
  runId: string;
  role: MessageRole;
  content: string;
  metadata: Record<string, unknown>;
  createdAt: string;
}

export interface MessageListResponse {
  messages: MessageRecord[];
}

export type AppStatus = 'idle' | 'running' | 'completed' | 'failed' | 'stopped';

export type AppPhase =
  | 'idle'
  | 'preparing'
  | 'gathering_context'
  | 'thinking'
  | 'answering'
  | 'done';

export type ConversionStatus = 'pending' | 'processing' | 'completed' | 'failed';

export interface UploadRecord {
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  threadId?: string;
  status: ConversionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface UploadResponse {
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  status: ConversionStatus;
  createdAt: string;
}

export interface UploadListResponse {
  uploads: UploadRecord[];
}

export interface UploadContentResponse {
  fileId: string;
  fileName: string;
  content: string;
}

export interface RunHistoryEntry {
  runId: string;
  threadId?: string;
  message: string;
  status: AppStatus;
  startedAt: string;
  completedAt?: string;
  model?: string;
}

export interface AppState {
  status: AppStatus;
  phase: AppPhase;
  runId?: string;
  threadId?: string;
  threads: ThreadRecord[];
  messages: MessageRecord[];
  events: DeerFlowEvent[];
  finalAnswer?: string;
  error?: string;
  lastRequest?: RunRequest;
  uploads: UploadRecord[];
  selectedUploadIds: string[];
  runHistory: RunHistoryEntry[];
}

export type AppAction =
  | { type: 'START_RUN'; payload: RunRequest }
  | { type: 'ADD_EVENT'; payload: DeerFlowEvent }
  | { type: 'SET_FINAL_ANSWER'; payload: string }
  | { type: 'SET_ERROR'; payload: string }
  | { type: 'SET_THREAD_ID'; payload?: string }
  | { type: 'SET_THREADS'; payload: ThreadRecord[] }
  | { type: 'SET_MESSAGES'; payload: MessageRecord[] }
  | { type: 'STOP_RUN' }
  | { type: 'CLEAR' }
  | { type: 'RE_RUN' }
  | { type: 'SET_UPLOADS'; payload: UploadRecord[] }
  | { type: 'ADD_UPLOAD'; payload: UploadRecord }
  | { type: 'REMOVE_UPLOAD'; payload: string }
  | { type: 'TOGGLE_UPLOAD_SELECTION'; payload: string }
  | { type: 'ADD_RUN_HISTORY'; payload: RunHistoryEntry };
