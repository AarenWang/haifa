import { useReducer, useRef, useCallback, useEffect, useState } from 'react';
import type { RunRequest, UploadRecord } from './types';
import { deerflowReducer, initialState } from './state/deerflowReducer';
import {
  readDeerFlowStream,
  fetchArtifacts,
  checkBackendHealth,
  listUploads,
  deleteUpload,
  listThreads,
  listThreadMessages,
  listThreadRuns,
  fetchRunEvidence,
  fetchRunEvents,
  fetchRunPlan,
  fetchRunProgress,
  fetchRunQualityGate,
  fetchRunSources,
  readDeerFlowResumeStream,
} from './api/deerflowClient';
import Header from './components/Header';
import TaskComposer from './components/TaskComposer';
import AnswerWorkspace from './components/AnswerWorkspace';
import ActivityTrace from './components/ActivityTrace';
import ArtifactPanel from './components/ArtifactPanel';
import ResearchInspector from './components/ResearchInspector';
import ResearchPlanView from './components/ResearchPlanView';
import WorkspaceSidebar from './components/WorkspaceSidebar';
import MemorySettingsModal from './components/MemorySettingsModal';

function App() {
  const [state, dispatch] = useReducer(deerflowReducer, initialState);
  const abortRef = useRef<AbortController | null>(null);
  const [backendStatus, setBackendStatus] = useState<'connected' | 'disconnected' | 'unknown'>('unknown');
  const [externalMessage, setExternalMessage] = useState<string | undefined>(undefined);
  const [isMemoryOpen, setIsMemoryOpen] = useState<boolean>(false);

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

  const refreshResearchData = useCallback(async (runId?: string) => {
    if (!runId) {
      dispatch({ type: 'SET_RESEARCH_SOURCES', payload: [] });
      dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: [] });
      dispatch({ type: 'SET_RESEARCH_PLAN' });
      dispatch({ type: 'SET_RESEARCH_PROGRESS' });
      dispatch({ type: 'SET_QUALITY_GATE' });
      return;
    }
    try {
      const [sources, evidenceItems, planResult, progressResult, qualityResult] = await Promise.allSettled([
        fetchRunSources(runId),
        fetchRunEvidence(runId),
        fetchRunPlan(runId),
        fetchRunProgress(runId),
        fetchRunQualityGate(runId),
      ]);
      if (sources.status === 'fulfilled') {
        dispatch({ type: 'SET_RESEARCH_SOURCES', payload: sources.value });
      } else {
        dispatch({ type: 'SET_RESEARCH_SOURCES', payload: [] });
      }
      if (evidenceItems.status === 'fulfilled') {
        dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: evidenceItems.value });
      } else {
        dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: [] });
      }
      if (planResult.status === 'fulfilled') {
        dispatch({ type: 'SET_RESEARCH_PLAN', payload: planResult.value });
      } else {
        dispatch({ type: 'SET_RESEARCH_PLAN' });
      }
      if (progressResult.status === 'fulfilled') {
        dispatch({ type: 'SET_RESEARCH_PROGRESS', payload: progressResult.value });
      } else {
        dispatch({ type: 'SET_RESEARCH_PROGRESS' });
      }
      if (qualityResult.status === 'fulfilled') {
        dispatch({ type: 'SET_QUALITY_GATE', payload: qualityResult.value });
      } else {
        dispatch({ type: 'SET_QUALITY_GATE' });
      }
    } catch (err) {
      console.error('Failed to load research artifacts', err);
      dispatch({ type: 'SET_RESEARCH_SOURCES', payload: [] });
      dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: [] });
    }
  }, []);

  const refreshArtifactData = useCallback(async (threadId?: string, runId?: string) => {
    if (!threadId && !runId) {
      dispatch({ type: 'SET_ARTIFACTS', payload: [] });
      return;
    }
    try {
      const artifacts = await fetchArtifacts({ threadId, runId });
      dispatch({ type: 'SET_ARTIFACTS', payload: artifacts });
    } catch {
      dispatch({ type: 'SET_ARTIFACTS', payload: [] });
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
        if (latestRun.mode === 'research') {
          refreshResearchData(latestRun.runId);
        } else {
          refreshResearchData();
        }
        refreshArtifactData(threadId, latestRun.runId);
      } else {
        dispatch({ type: 'SET_EVENTS', payload: [] });
        refreshResearchData();
        refreshArtifactData(threadId);
      }
    } catch (err) {
      console.error('Failed to load historical events', err);
      dispatch({ type: 'SET_EVENTS', payload: [] });
      refreshResearchData();
      refreshArtifactData(threadId);
    }
  }, [refreshArtifactData, refreshResearchData]);

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
      let streamThreadId = fullReq.threadId;

      dispatch({ type: 'START_RUN', payload: fullReq });

      readDeerFlowStream(
        fullReq,
        {
          onEvent: (evt) => {
            if (evt.threadId) {
              streamThreadId = evt.threadId;
            }
            dispatch({ type: 'ADD_EVENT', payload: evt });
            const isResearchRun = fullReq.mode === 'research';
            const isResearchEvent =
              evt.type === 'RESEARCH_PLAN_CREATED' ||
              evt.type === 'RESEARCH_DIMENSION_STARTED' ||
              evt.type === 'RESEARCH_DIMENSION_COMPLETED' ||
              evt.type === 'SOURCE_FOUND' ||
              evt.type === 'SOURCE_FETCHED' ||
              evt.type === 'EVIDENCE_EXTRACTED' ||
              evt.type === 'QUALITY_GATE_STARTED' ||
              evt.type === 'QUALITY_GATE_PASSED' ||
              evt.type === 'QUALITY_GATE_FAILED' ||
              evt.type === 'REPORT_STARTED' ||
              evt.type === 'REPORT_COMPLETED' ||
              evt.type === 'ARTIFACT_CREATED' ||
              evt.type === 'MODEL_COMPLETED' ||
              evt.type === 'RUN_COMPLETED';
            if (
              evt.type === 'RUN_STARTED' ||
              evt.type === 'MODEL_COMPLETED' ||
              evt.type === 'RUN_COMPLETED' ||
              evt.type === 'RUN_FAILED'
            ) {
              refreshThreads();
              refreshMessages(evt.threadId);
            }
            if (isResearchRun && isResearchEvent) {
              refreshResearchData(evt.runId);
            }
            if (
              evt.type === 'ARTIFACT_CREATED' ||
              evt.type === 'REPORT_COMPLETED' ||
              evt.type === 'RUN_COMPLETED'
            ) {
              refreshArtifactData(evt.threadId, evt.runId);
            }
          },
          onError: (err) => {
            dispatch({ type: 'SET_ERROR', payload: err });
          },
          onDone: () => {
            abortRef.current = null;
            if (streamThreadId) {
              void refreshMessages(streamThreadId);
            }
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
    [refreshArtifactData, refreshMessages, refreshResearchData, refreshThreads, state.selectedUploadIds, state.threadId]
  );

  const handleResumeRun = useCallback((runId: string) => {
    if (abortRef.current) {
      abortRef.current.abort();
    }
    const controller = new AbortController();
    abortRef.current = controller;

    dispatch({
      type: 'START_RUN',
      payload: state.lastRequest || { message: '' }
    });

    let streamThreadId = state.threadId;

    readDeerFlowResumeStream(
      runId,
      {
        onEvent: (evt) => {
          if (evt.threadId) {
            streamThreadId = evt.threadId;
          }
          dispatch({ type: 'ADD_EVENT', payload: evt });
          const isResearchRun = state.lastRequest?.mode === 'research';
          const isResearchEvent =
            evt.type === 'RESEARCH_PLAN_CREATED' ||
            evt.type === 'RESEARCH_DIMENSION_STARTED' ||
            evt.type === 'RESEARCH_DIMENSION_COMPLETED' ||
            evt.type === 'SOURCE_FOUND' ||
            evt.type === 'SOURCE_FETCHED' ||
            evt.type === 'EVIDENCE_EXTRACTED' ||
            evt.type === 'QUALITY_GATE_STARTED' ||
            evt.type === 'QUALITY_GATE_PASSED' ||
            evt.type === 'QUALITY_GATE_FAILED' ||
            evt.type === 'REPORT_STARTED' ||
            evt.type === 'REPORT_COMPLETED' ||
            evt.type === 'ARTIFACT_CREATED' ||
            evt.type === 'MODEL_COMPLETED' ||
            evt.type === 'RUN_COMPLETED';
          if (
            evt.type === 'RUN_STARTED' ||
            evt.type === 'MODEL_COMPLETED' ||
            evt.type === 'RUN_COMPLETED' ||
            evt.type === 'RUN_FAILED'
          ) {
            refreshThreads();
            refreshMessages(evt.threadId);
          }
          if (isResearchRun && isResearchEvent) {
            refreshResearchData(evt.runId);
          }
          if (
            evt.type === 'ARTIFACT_CREATED' ||
            evt.type === 'REPORT_COMPLETED' ||
            evt.type === 'RUN_COMPLETED'
          ) {
            refreshArtifactData(evt.threadId, evt.runId);
          }
        },
        onError: (err) => {
          dispatch({ type: 'SET_ERROR', payload: err });
        },
        onDone: () => {
          abortRef.current = null;
          if (streamThreadId) {
            void refreshMessages(streamThreadId);
          }
        },
      },
      controller.signal
    ).catch((err) => {
      if (!controller.signal.aborted) {
        dispatch({ type: 'SET_ERROR', payload: (err as Error).message });
      }
      abortRef.current = null;
    });
  }, [refreshArtifactData, refreshMessages, refreshResearchData, refreshThreads, state.lastRequest, state.threadId]);

  const handleStop = useCallback(() => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    dispatch({ type: 'STOP_RUN' });
  }, []);

  const handleReRun = useCallback(() => {
    if (state.lastRequest) {
      handleRun(state.lastRequest);
    }
  }, [state.lastRequest, handleRun]);

  const handleRefreshMessage = useCallback((messageId: string) => {
    // Find the target assistant message in the full thread
    const targetIndex = state.messages.findIndex((m) => m.messageId === messageId && m.role === 'ASSISTANT');
    if (targetIndex === -1) return;
    // Walk backwards to find the immediately preceding USER message
    for (let i = targetIndex - 1; i >= 0; i--) {
      const msg = state.messages[i];
      if (msg.role === 'USER') {
        const req: RunRequest = {
          message: msg.content,
          threadId: state.threadId,
          model: state.lastRequest?.model,
          mode: state.lastRequest?.mode || 'chat',
          uploadedFileIds: state.selectedUploadIds.length > 0 ? state.selectedUploadIds : undefined,
        };
        if (state.lastRequest?.researchOptions) {
          req.researchOptions = state.lastRequest.researchOptions;
        }
        handleRun(req);
        return;
      }
    }
  }, [state.messages, state.threadId, state.lastRequest, state.selectedUploadIds, handleRun]);

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
    dispatch({ type: 'SET_RESEARCH_SOURCES', payload: [] });
    dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: [] });
    dispatch({ type: 'SET_ARTIFACTS', payload: [] });
    dispatch({ type: 'SET_THREAD_ID', payload: threadId });
    dispatch({ type: 'SET_LAST_REQUEST' });
  }, []);

  const handleNewThread = useCallback(() => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    dispatch({ type: 'CLEAR' });
    dispatch({ type: 'SET_UPLOADS', payload: [] });
    dispatch({ type: 'SET_MESSAGES', payload: [] });
    dispatch({ type: 'SET_RESEARCH_SOURCES', payload: [] });
    dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: [] });
    dispatch({ type: 'SET_ARTIFACTS', payload: [] });
    dispatch({ type: 'SET_THREAD_ID' });
    dispatch({ type: 'SET_LAST_REQUEST' });
  }, []);

  return (
    <div className="app">
      <Header
        backendStatus={backendStatus}
        runStatus={state.status}
        onOpenMemorySettings={() => setIsMemoryOpen(true)}
      />
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
          <AnswerWorkspace
            phase={state.phase}
            status={state.status}
            messages={state.messages}
            finalAnswer={state.finalAnswer}
            error={state.error}
            onReRun={state.status === 'failed' ? handleReRun : undefined}
            onRefreshMessage={handleRefreshMessage}
            onResumeRun={handleResumeRun}
          />
          {state.lastRequest?.mode === 'research' && (
            <ResearchPlanView
              plan={state.researchPlan}
              progress={state.researchProgress}
              qualityGate={state.qualityGate}
            />
          )}
          <ArtifactPanel
            artifacts={state.artifacts}
            onFollowUp={(filename) => {
              setExternalMessage(`Please refine/expand the report ${filename} to...`);
            }}
          />
          {state.lastRequest?.mode === 'research' && (
            <ResearchInspector
              sources={state.researchSources}
              evidenceItems={state.evidenceItems}
              onFollowUp={(sourceTitle) => {
                setExternalMessage(`Please look deeper into the source "${sourceTitle}" to investigate...`);
              }}
            />
          )}
          <TaskComposer
            onRun={handleRun}
            onStop={handleStop}
            isRunning={state.status === 'running'}
            lastRequest={state.lastRequest}
            selectedUploadCount={state.selectedUploadIds.length}
            externalMessage={externalMessage}
            onClearExternalMessage={() => setExternalMessage(undefined)}
          />
        </div>
        <ActivityTrace events={state.events} />
      </div>
      {isMemoryOpen && <MemorySettingsModal onClose={() => setIsMemoryOpen(false)} />}
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
