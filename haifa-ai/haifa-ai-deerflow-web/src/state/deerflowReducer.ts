import type { AppState, AppAction, AppPhase } from '../types';

function getPhaseFromEvent(type: string): AppPhase {
  switch (type) {
    case 'RUN_STARTED':
      return 'preparing';
    case 'TOOL_STARTED':
    case 'TOOL_COMPLETED':
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
  uploads: [],
  selectedUploadIds: [],
  runHistory: [],
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
        uploads: state.uploads,
        selectedUploadIds: state.selectedUploadIds,
        runHistory: [entry, ...state.runHistory],
      };
    }
    case 'ADD_EVENT': {
      const event = action.payload;
      const nextEvents = [...state.events, event];
      const nextPhase = getPhaseFromEvent(event.type);
      let nextStatus: AppState['status'] = state.status;
      let nextFinalAnswer = state.finalAnswer;
      let nextError = state.error;
      let nextRunId = state.runId;
      let nextThreadId = state.threadId;

      if (event.type === 'RUN_COMPLETED') {
        nextStatus = 'completed';
      } else if (event.type === 'RUN_FAILED') {
        nextStatus = 'failed';
        nextError = event.content || 'Run failed';
      } else if (event.type === 'RUN_CANCELLED') {
        nextStatus = 'stopped';
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
        runId: nextRunId,
        threadId: nextThreadId,
        finalAnswer: nextFinalAnswer,
        error: nextError,
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
        error: action.payload,
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
    case 'SET_EVENTS':
      return {
        ...state,
        events: action.payload,
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
