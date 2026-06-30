import { useReducer, useRef, useCallback, useEffect, useState } from 'react';
import type { RunRequest, UploadRecord } from './types';
import { deerflowReducer, initialState } from './state/deerflowReducer';
import { readDeerFlowStream, checkBackendHealth, listUploads } from './api/deerflowClient';
import Header from './components/Header';
import TaskComposer from './components/TaskComposer';
import AnswerWorkspace from './components/AnswerWorkspace';
import ActivityTrace from './components/ActivityTrace';
import WorkspaceSidebar from './components/WorkspaceSidebar';

function App() {
  const [state, dispatch] = useReducer(deerflowReducer, initialState);
  const abortRef = useRef<AbortController | null>(null);
  const [backendStatus, setBackendStatus] = useState<'connected' | 'disconnected' | 'unknown'>('unknown');

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

  // Load uploads on mount
  useEffect(() => {
    let mounted = true;
    const load = async () => {
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
        uploadedFileIds: state.selectedUploadIds.length > 0 ? state.selectedUploadIds : undefined,
      };

      dispatch({ type: 'START_RUN', payload: fullReq });

      readDeerFlowStream(
        fullReq,
        {
          onEvent: (evt) => {
            dispatch({ type: 'ADD_EVENT', payload: evt });
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
    [state.selectedUploadIds]
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

  const handleClearUploads = useCallback(() => {
    dispatch({ type: 'SET_UPLOADS', payload: [] });
    // Stale selectedUploadIds are harmless; they will not match any uploads.
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
          runHistory={state.runHistory}
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

export default App;
