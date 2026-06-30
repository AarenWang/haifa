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
    default:
      return 'idle';
  }
}

export const initialState: AppState = {
  status: 'idle',
  phase: 'idle',
  events: [],
};

export function deerflowReducer(state: AppState, action: AppAction): AppState {
  switch (action.type) {
    case 'START_RUN':
      return {
        ...initialState,
        status: 'running',
        phase: 'preparing',
        lastRequest: action.payload,
        threadId: action.payload.threadId,
      };
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

      return {
        ...state,
        status: nextStatus,
        phase: nextPhase,
        events: nextEvents,
        runId: nextRunId,
        threadId: nextThreadId,
        finalAnswer: nextFinalAnswer,
        error: nextError,
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
      };
    case 'RE_RUN':
      if (!state.lastRequest) return state;
      return {
        ...initialState,
        status: 'running',
        phase: 'preparing',
        lastRequest: state.lastRequest,
        threadId: state.lastRequest.threadId,
      };
    default:
      return state;
  }
}
