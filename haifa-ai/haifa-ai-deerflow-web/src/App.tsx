import { useReducer, useRef, useCallback, useEffect, useState } from 'react';
import type { RunRequest } from './types';
import { deerflowReducer, initialState } from './state/deerflowReducer';
import { readDeerFlowStream, checkBackendHealth } from './api/deerflowClient';
import Header from './components/Header';
import TaskComposer from './components/TaskComposer';
import AnswerWorkspace from './components/AnswerWorkspace';
import ActivityTrace from './components/ActivityTrace';

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

      dispatch({ type: 'START_RUN', payload: req });

      readDeerFlowStream(
        req,
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
    []
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

  return (
    <div className="app">
      <Header backendStatus={backendStatus} runStatus={state.status} />
      <div className="main">
        <div className="workspace">
          <TaskComposer
            onRun={handleRun}
            onStop={handleStop}
            onClear={handleClear}
            isRunning={state.status === 'running'}
            lastRequest={state.lastRequest}
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
