import { useReducer, useRef, useCallback, useEffect, useState } from 'react';
import type { RunRequest, UploadRecord } from './types';
import { deerflowReducer, initialState } from './state/deerflowReducer';
import {
  readDeerFlowStream,
  checkBackendHealth,
  listUploads,
  deleteUpload,
  listThreads,
  listThreadMessages,
  listThreadRuns,
  fetchRunEvents,
} from './api/deerflowClient';
import Header from './components/Header';
import TaskComposer from './components/TaskComposer';
import AnswerWorkspace from './components/AnswerWorkspace';
import ActivityTrace from './components/ActivityTrace';
import WorkspaceSidebar from './components/WorkspaceSidebar';

function App() {
  const [state, dispatch] = useReducer(deerflowReducer, initialState);
  const abortRef = useRef<AbortController | null>(null);
  const [backendStatus, setBackendStatus] = useState<'connected' | 'disconnected' | 'unknown'>('unknown');

  useEffect(() => {
    const threadId = getThreadIdFromUrl();
    if (threadId) {
      dispatch({ type: 'SET_THREAD_ID', payload: threadId });
    }
  }, []);

  useEffect(() => {
    syncThreadIdToUrl(state.threadId);
  }, [state.threadId]);

  const refreshThreads = useCallback(async () => {
    try {
      const data = await listThreads();
      dispatch({ type: 'SET_THREADS', payload: data.threads });
    } catch {
      // Threads are optional for offline development; runs will still upsert them.
    }
  }, []);

  const refreshMessages = useCallback(async (threadId?: string) => {
    if (!threadId) {
      dispatch({ type: 'SET_MESSAGES', payload: [] });
      return;
    }
    try {
      const data = await listThreadMessages(threadId);
      dispatch({ type: 'SET_MESSAGES', payload: data.messages });
    } catch {
      dispatch({ type: 'SET_MESSAGES', payload: [] });
    }
  }, []);

  const refreshEvents = useCallback(async (threadId?: string) => {
    if (!threadId) {
      dispatch({ type: 'SET_EVENTS', payload: [] });
      return;
    }
    try {
      const data = await listThreadRuns(threadId);
      if (data.runs && data.runs.length > 0) {
        // Sort runs by createdAt desc to get the latest run
        const sorted = [...data.runs].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        const latestRun = sorted[0];
        const events = await fetchRunEvents(latestRun.runId);
        dispatch({ type: 'SET_EVENTS', payload: events });
      } else {
        dispatch({ type: 'SET_EVENTS', payload: [] });
      }
    } catch (err) {
      console.error('Failed to load historical events', err);
      dispatch({ type: 'SET_EVENTS', payload: [] });
    }
  }, []);

  // Poll backend health
  useEffect(() => {
    let mounted = true;
    const poll = async () => {
      const ok = await checkBackendHealth();
      if (mounted) {
        setBackendStatus(ok ? 'connected' : 'disconnected');
      }
    };
    poll();
    const id = setInterval(poll, 10000);
    return () => {
      mounted = false;
      clearInterval(id);
    };
  }, []);

  useEffect(() => {
    refreshThreads();
  }, [refreshThreads]);

  useEffect(() => {
    refreshMessages(state.threadId);
  }, [refreshMessages, state.threadId]);

  useEffect(() => {
    refreshEvents(state.threadId);
  }, [refreshEvents, state.threadId]);

  // Load uploads on mount
  useEffect(() => {
    let mounted = true;
    const load = async () => {
      if (!state.threadId) {
        dispatch({ type: 'SET_UPLOADS', payload: [] });
        return;
      }
      try {
        const data = await listUploads(state.threadId);
        if (mounted) {
          dispatch({ type: 'SET_UPLOADS', payload: data.uploads });
        }
      } catch {
        // ignore — uploads are optional
      }
    };
    load();
    return () => {
      mounted = false;
    };
  }, [state.threadId]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortRef.current) {
        abortRef.current.abort();
      }
    };
  }, []);

  const handleRun = useCallback(
    (req: RunRequest) => {
      if (abortRef.current) {
        abortRef.current.abort();
      }
      const controller = new AbortController();
      abortRef.current = controller;

      const fullReq: RunRequest = {
        ...req,
        threadId: req.threadId || state.threadId || undefined,
        uploadedFileIds: state.selectedUploadIds.length > 0 ? state.selectedUploadIds : undefined,
      };

      dispatch({ type: 'START_RUN', payload: fullReq });

      readDeerFlowStream(
        fullReq,
        {
          onEvent: (evt) => {
            dispatch({ type: 'ADD_EVENT', payload: evt });
            if (
              evt.type === 'RUN_STARTED' ||
              evt.type === 'MODEL_COMPLETED' ||
              evt.type === 'RUN_COMPLETED' ||
              evt.type === 'RUN_FAILED'
            ) {
              refreshThreads();
              refreshMessages(evt.threadId);
            }
          },
          onError: (err) => {
            dispatch({ type: 'SET_ERROR', payload: err });
          },
          onDone: () => {
            abortRef.current = null;
          },
        },
        controller.signal
      ).catch((err) => {
        if (!controller.signal.aborted) {
          dispatch({ type: 'SET_ERROR', payload: (err as Error).message });
        }
        abortRef.current = null;
      });
    },
    [refreshMessages, refreshThreads, state.selectedUploadIds, state.threadId]
  );

  const handleStop = useCallback(() => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    dispatch({ type: 'STOP_RUN' });
  }, []);

  const handleClear = useCallback(() => {
    dispatch({ type: 'CLEAR' });
  }, []);

  const handleReRun = useCallback(() => {
    if (state.lastRequest) {
      handleRun(state.lastRequest);
    }
  }, [state.lastRequest, handleRun]);

  const handleUploadsChange = useCallback((uploads: UploadRecord[]) => {
    dispatch({ type: 'SET_UPLOADS', payload: uploads });
  }, []);

  const handleToggleUploadSelection = useCallback((fileId: string) => {
    dispatch({ type: 'TOGGLE_UPLOAD_SELECTION', payload: fileId });
  }, []);

  const handleRemoveUpload = useCallback((fileId: string) => {
    dispatch({ type: 'REMOVE_UPLOAD', payload: fileId });
  }, []);

  const handleClearUploads = useCallback(async () => {
    if (!state.threadId) {
      dispatch({ type: 'SET_UPLOADS', payload: [] });
      return;
    }
    for (const upload of state.uploads) {
      try {
        await deleteUpload(upload.fileId, state.threadId);
      } catch (err) {
        console.error('Failed to delete upload', err);
      }
    }
    dispatch({ type: 'SET_UPLOADS', payload: [] });
  }, [state.threadId, state.uploads]);

  const handleSelectThread = useCallback((threadId: string) => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    dispatch({ type: 'CLEAR' });
    dispatch({ type: 'SET_UPLOADS', payload: [] });
    dispatch({ type: 'SET_MESSAGES', payload: [] });
    dispatch({ type: 'SET_THREAD_ID', payload: threadId });
  }, []);

  const handleNewThread = useCallback(() => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    dispatch({ type: 'CLEAR' });
    dispatch({ type: 'SET_UPLOADS', payload: [] });
    dispatch({ type: 'SET_MESSAGES', payload: [] });
    dispatch({ type: 'SET_THREAD_ID' });
  }, []);

  return (
    <div className="app">
      <Header backendStatus={backendStatus} runStatus={state.status} />
      <div className="main">
        <WorkspaceSidebar
          backendStatus={backendStatus}
          uploads={state.uploads}
          selectedUploadIds={state.selectedUploadIds}
          threadId={state.threadId}
          threads={state.threads}
          onSelectThread={handleSelectThread}
          onNewThread={handleNewThread}
          onUploadsChange={handleUploadsChange}
          onToggleUploadSelection={handleToggleUploadSelection}
          onRemoveUpload={handleRemoveUpload}
          onClearUploads={handleClearUploads}
        />
        <div className="workspace">
          <TaskComposer
            onRun={handleRun}
            onStop={handleStop}
            onClear={handleClear}
            isRunning={state.status === 'running'}
            lastRequest={state.lastRequest}
            selectedUploadCount={state.selectedUploadIds.length}
          />
          <AnswerWorkspace
            phase={state.phase}
            status={state.status}
            messages={state.messages}
            finalAnswer={state.finalAnswer}
            error={state.error}
            onReRun={state.status === 'failed' ? handleReRun : undefined}
          />
        </div>
        <ActivityTrace events={state.events} />
      </div>
    </div>
  );
}

function getThreadIdFromUrl() {
  if (typeof window === 'undefined') {
    return undefined;
  }
  return new URLSearchParams(window.location.search).get('threadId') || undefined;
}

function syncThreadIdToUrl(threadId?: string) {
  if (typeof window === 'undefined') {
    return;
  }
  const url = new URL(window.location.href);
  if (threadId) {
    url.searchParams.set('threadId', threadId);
  } else {
    url.searchParams.delete('threadId');
  }
  window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`);
}

export default App;
