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

export interface ResearchPlan {
  planId: string;
  threadId: string;
  runId: string;
  topic: string;
  researchQuestions: string[];
  dimensions: ResearchDimension[];
  searchQueries: string[];
  sourceCriteria: string;
  expectedDeliverable: string;
  status: string;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface ResearchDimension {
  id: string;
  title: string;
  description: string;
  status: string;
  searchQueries: string[];
  expectedSourceCount: number;
  actualSourceCount: number;
  actualEvidenceCount: number;
  evidenceIds: string[];
}

export interface ResearchProgress {
  totalDimensions: number;
  completedDimensions: number;
  inProgressDimensions: number;
  totalSources: number;
  totalEvidence: number;
  planStatus: string;
  completionPercentage: number;
  gaps: string[];
}

export interface QualityGateResult {
  passed: boolean;
  score: number;
  gaps: string[];
  recommendation: string;
  dimensionCount: number;
  fetchedSourceCount: number;
  hasFacts: boolean;
  hasData: boolean;
  hasCases: boolean;
  hasOpinions: boolean;
  hasLimitations: boolean;
  hasCounterView: boolean;
  citationComplete: boolean;
}

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

export interface ResearchSource {
  sourceId: string;
  title: string;
  url: string;
  domain: string;
  publishedAt?: string | null;
  fetchedAt?: string | null;
  sourceType: string;
  credibility: number;
  snippet: string;
  contentHash: string;
  fetched: boolean;
  citationCount: number;
}

export interface EvidenceItem {
  evidenceId: string;
  sourceId: string;
  sourceTitle: string;
  sourceUrl: string;
  quoteOrParaphrase: string;
  claim: string;
  dimension: string;
  confidence: number;
  extractedAt?: string | null;
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
  researchSources: ResearchSource[];
  evidenceItems: EvidenceItem[];
  researchPlan?: ResearchPlan;
  researchProgress?: ResearchProgress;
  qualityGate?: QualityGateResult;
  uploads: UploadRecord[];
  selectedUploadIds: string[];
  runHistory: RunHistoryEntry[];
}

export type AppAction =
  | { type: 'START_RUN'; payload: RunRequest }
  | { type: 'ADD_EVENT'; payload: DeerFlowEvent }
  | { type: 'SET_FINAL_ANSWER'; payload: string }
  | { type: 'SET_ERROR'; payload: string }
  | { type: 'SET_RESEARCH_SOURCES'; payload: ResearchSource[] }
  | { type: 'SET_EVIDENCE_ITEMS'; payload: EvidenceItem[] }
  | { type: 'SET_RESEARCH_PLAN'; payload?: ResearchPlan }
  | { type: 'SET_RESEARCH_PROGRESS'; payload?: ResearchProgress }
  | { type: 'SET_QUALITY_GATE'; payload?: QualityGateResult }
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
  | { type: 'ADD_RUN_HISTORY'; payload: RunHistoryEntry }
  | { type: 'SET_EVENTS'; payload: DeerFlowEvent[] }
 | { type: 'SET_LAST_REQUEST'; payload?: RunRequest };
