import { useReducer, useRef, useCallback, useEffect, useState } from 'react';
import type { RunRequest, UploadRecord, AppStatus, ClarificationQuestion, ClarificationAnswer } from './types';
import { deerflowReducer, initialState } from './state/deerflowReducer';
import {
  answerClarification,
  readDeerFlowStream,
  fetchArtifacts,
  checkBackendHealth,
  listUploads,
  deleteUpload,
  listThreads,
  listThreadMessages,
  listThreadRuns,
  fetchRunUnifiedEvidence,
  fetchRunEvents,
  fetchRunUnifiedSources,
  fetchRunTodos,
  fetchRunObservability,
  readDeerFlowResumeStream,
  fetchRunSkillActivations,
  fetchRunWorkItems,
  fetchRunClaims,
  fetchRunCitations,
  fetchRunQuality,
  fetchRunBudget,
  fetchThreadFiles,
  cancelRun,
} from './api/deerflowClient';
import Header from './components/Header';
import TaskComposer, { type PendingClarification } from './components/TaskComposer';
import AnswerWorkspace from './components/AnswerWorkspace';
import ActivityTrace from './components/ActivityTrace';
import ArtifactPanel from './components/ArtifactPanel';
import ArtifactCanvas from './components/ArtifactCanvas';
import ResearchInspector from './components/ResearchInspector';
import WorkspaceSidebar from './components/WorkspaceSidebar';
import MemorySettingsModal from './components/MemorySettingsModal';
import { Activity, Menu } from 'lucide-react';

function App() {
  const [state, dispatch] = useReducer(deerflowReducer, initialState);
  const abortRef = useRef<AbortController | null>(null);
  const [backendStatus, setBackendStatus] = useState<'connected' | 'disconnected' | 'unknown'>('unknown');
  const [externalMessage, setExternalMessage] = useState<string | undefined>(undefined);
  const [isMemoryOpen, setIsMemoryOpen] = useState<boolean>(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(false);
  const [isTraceOpen, setIsTraceOpen] = useState<boolean>(false);
  const [canvasArtifactId, setCanvasArtifactId] = useState<string | null>(null);
  const allowArtifactAutoOpenRef = useRef(false);

  const toggleSidebar = useCallback(() => {
    setIsSidebarOpen((open) => !open);
    setIsTraceOpen(false);
  }, []);

  const toggleTrace = useCallback(() => {
    setIsTraceOpen((open) => !open);
    setIsSidebarOpen(false);
  }, []);

  const pendingClarification = getPendingClarification(
    state.messages,
    state.status,
    state.threadId
  );
  const hasResearchPanelData =
    state.lastRequest?.mode === 'research' ||
    (state.skillActivations || []).some((skill) => skill.skillName === 'deep-research') ||
    (state.workItems || []).length > 0 ||
    (state.sources || []).length > 0 ||
    (state.evidenceItems || []).length > 0 ||
    (state.claims || []).length > 0 ||
    !!state.budgetLedger ||
    !!state.qualityAssessment;
  const handleOpenCanvas = useCallback((artifactId?: string) => {
    const target = artifactId || state.artifacts[0]?.artifactId;
    if (target) {
      setCanvasArtifactId(target);
      setIsSidebarOpen(false);
      setIsTraceOpen(false);
    }
  }, [state.artifacts]);

  const handleCloseCanvas = useCallback(() => {
    setCanvasArtifactId(null);
  }, []);

  useEffect(() => {
    if (canvasArtifactId && !state.artifacts.some((artifact) => artifact.artifactId === canvasArtifactId)) {
      setCanvasArtifactId(null);
    }
  }, [canvasArtifactId, state.artifacts]);

  const previousThreadId = useRef(state.threadId);
  const previousArtifactsCount = useRef(state.artifacts.length);

  useEffect(() => {
    if (state.threadId !== previousThreadId.current) {
      previousThreadId.current = state.threadId;
      previousArtifactsCount.current = state.artifacts.length;
      allowArtifactAutoOpenRef.current = false;
      setCanvasArtifactId(null);
      return;
    }
    if (allowArtifactAutoOpenRef.current && state.artifacts.length > previousArtifactsCount.current) {
      const newest = state.artifacts[0];
      if (newest) {
        handleOpenCanvas(newest.artifactId);
        allowArtifactAutoOpenRef.current = false;
      }
    }
    previousArtifactsCount.current = state.artifacts.length;
  }, [state.artifacts, state.threadId, handleOpenCanvas]);

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

  const refreshObservabilityData = useCallback(async (runId?: string) => {
    if (!runId) {
      dispatch({ type: 'SET_RUN_OBSERVABILITY' });
      return;
    }
    try {
      const observability = await fetchRunObservability(runId);
      dispatch({ type: 'SET_RUN_OBSERVABILITY', payload: observability });
    } catch {
      dispatch({ type: 'SET_RUN_OBSERVABILITY' });
    }
  }, []);

  const refreshResearchData = useCallback(async (runId?: string, threadId?: string) => {
    if (!runId) {
      dispatch({ type: 'SET_UNIFIED_SOURCES', payload: [] });
      dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: [] });
      dispatch({ type: 'SET_WORK_ITEMS', payload: [] });
      dispatch({ type: 'SET_CLAIMS', payload: [] });
      dispatch({ type: 'SET_CITATIONS', payload: [] });
      dispatch({ type: 'SET_BUDGET_LEDGER', payload: null });
      dispatch({ type: 'SET_QUALITY_ASSESSMENT', payload: null });
      dispatch({ type: 'SET_SKILL_ACTIVATIONS', payload: [] });
      dispatch({ type: 'SET_THREAD_FILES', payload: [] });
      return;
    }
    try {
      const [
        sourcesRes,
        evidenceRes,
        workItemsRes,
        claimsRes,
        citationsRes,
        budgetRes,
        qualityRes,
        activityRes,
        filesRes
      ] = await Promise.allSettled([
        fetchRunUnifiedSources(runId),
        fetchRunUnifiedEvidence(runId),
        fetchRunWorkItems(runId),
        fetchRunClaims(runId),
        fetchRunCitations(runId),
        fetchRunBudget(runId),
        fetchRunQuality(runId),
        fetchRunSkillActivations(runId),
        threadId ? fetchThreadFiles(threadId) : Promise.resolve([])
      ]);

      if (sourcesRes.status === 'fulfilled') {
        dispatch({ type: 'SET_UNIFIED_SOURCES', payload: sourcesRes.value });
      } else {
        dispatch({ type: 'SET_UNIFIED_SOURCES', payload: [] });
      }
      if (evidenceRes.status === 'fulfilled') {
        dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: evidenceRes.value });
      } else {
        dispatch({ type: 'SET_EVIDENCE_ITEMS', payload: [] });
      }
      if (workItemsRes.status === 'fulfilled') {
        dispatch({ type: 'SET_WORK_ITEMS', payload: workItemsRes.value });
      } else {
        dispatch({ type: 'SET_WORK_ITEMS', payload: [] });
      }
      if (claimsRes.status === 'fulfilled') {
        dispatch({ type: 'SET_CLAIMS', payload: claimsRes.value });
      } else {
        dispatch({ type: 'SET_CLAIMS', payload: [] });
      }
      if (citationsRes.status === 'fulfilled') {
        dispatch({ type: 'SET_CITATIONS', payload: citationsRes.value });
      } else {
        dispatch({ type: 'SET_CITATIONS', payload: [] });
      }
      if (budgetRes.status === 'fulfilled') {
        dispatch({ type: 'SET_BUDGET_LEDGER', payload: budgetRes.value });
      } else {
        dispatch({ type: 'SET_BUDGET_LEDGER', payload: null });
      }
      if (qualityRes.status === 'fulfilled') {
        dispatch({ type: 'SET_QUALITY_ASSESSMENT', payload: qualityRes.value });
      } else {
        dispatch({ type: 'SET_QUALITY_ASSESSMENT', payload: null });
      }
      if (activityRes.status === 'fulfilled') {
        dispatch({ type: 'SET_SKILL_ACTIVATIONS', payload: activityRes.value });
      } else {
        dispatch({ type: 'SET_SKILL_ACTIVATIONS', payload: [] });
      }
      if (filesRes.status === 'fulfilled') {
        dispatch({ type: 'SET_THREAD_FILES', payload: filesRes.value });
      } else {
        dispatch({ type: 'SET_THREAD_FILES', payload: [] });
      }
    } catch (err) {
      console.error('Failed to load research artifacts', err);
      dispatch({ type: 'SET_UNIFIED_SOURCES', payload: [] });
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
        try {
          const todos = await fetchRunTodos(latestRun.runId);
          dispatch({ type: 'SET_TODO_SNAPSHOT', payload: todos });
        } catch {
          dispatch({ type: 'SET_TODO_SNAPSHOT' });
        }

        // Sync latest run status to state
        let appStatus: AppStatus = 'idle';
        if (latestRun) {
          const runStatus = latestRun.status.toUpperCase();
          if (runStatus === 'RUNNING' || runStatus === 'PENDING') {
            appStatus = 'running';
          } else if (runStatus === 'SUSPENDED') {
            appStatus = 'suspended';
          } else if (runStatus === 'COMPLETED') {
            appStatus = 'completed';
          } else if (runStatus === 'FAILED') {
            appStatus = 'failed';
          } else if (runStatus === 'CANCELLED') {
            appStatus = 'stopped';
          }
        }
        dispatch({ type: 'SET_STATUS', payload: appStatus });

        refreshResearchData(latestRun.runId, threadId);
        refreshArtifactData(threadId, latestRun.runId);
        refreshObservabilityData(latestRun.runId);
      } else {
        dispatch({ type: 'SET_EVENTS', payload: [] });
        dispatch({ type: 'SET_TODO_SNAPSHOT' });
        dispatch({ type: 'SET_STATUS', payload: 'idle' });
        refreshResearchData();
        refreshArtifactData(threadId);
        refreshObservabilityData();
      }
    } catch (err) {
      console.error('Failed to load historical events', err);
      dispatch({ type: 'SET_EVENTS', payload: [] });
      dispatch({ type: 'SET_TODO_SNAPSHOT' });
      dispatch({ type: 'SET_STATUS', payload: 'idle' });
      refreshResearchData();
      refreshArtifactData(threadId);
      refreshObservabilityData();
    }
  }, [refreshArtifactData, refreshObservabilityData, refreshResearchData]);

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
      allowArtifactAutoOpenRef.current = true;
      setCanvasArtifactId(null);

      dispatch({ type: 'START_RUN', payload: fullReq });

      readDeerFlowStream(
        fullReq,
        {
          onEvent: (evt) => {
            if (evt.threadId) {
              streamThreadId = evt.threadId;
            }
            dispatch({ type: 'ADD_EVENT', payload: evt });
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
              evt.type === 'TOOL_COMPLETED' ||
              evt.type === 'MODEL_COMPLETED' ||
              evt.type === 'RUN_COMPLETED' ||
              evt.type === 'RUN_FAILED';
            if (
              evt.type === 'RUN_STARTED' ||
              evt.type === 'MODEL_COMPLETED' ||
              evt.type === 'RUN_COMPLETED' ||
              evt.type === 'RUN_FAILED'
            ) {
              refreshThreads();
              refreshMessages(evt.threadId);
            }
            if (isResearchEvent) {
              refreshResearchData(evt.runId, evt.threadId);
            }
            if (
              evt.type === 'ARTIFACT_CREATED' ||
              evt.type === 'REPORT_COMPLETED' ||
              evt.type === 'RUN_COMPLETED' ||
              hasArtifactMetadata(evt)
            ) {
              refreshArtifactData(evt.threadId, evt.runId);
              refreshObservabilityData(evt.runId);
            }
          },
          onError: (err) => {
            abortRef.current = null;
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
    [refreshArtifactData, refreshMessages, refreshObservabilityData, refreshResearchData, refreshThreads, state.selectedUploadIds, state.threadId]
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
    allowArtifactAutoOpenRef.current = true;
    setCanvasArtifactId(null);

    readDeerFlowResumeStream(
      runId,
      {
        onEvent: (evt) => {
          if (evt.threadId) {
            streamThreadId = evt.threadId;
          }
          dispatch({ type: 'ADD_EVENT', payload: evt });
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
            evt.type === 'TOOL_COMPLETED' ||
              evt.type === 'MODEL_COMPLETED' ||
              evt.type === 'RUN_COMPLETED' ||
              evt.type === 'RUN_FAILED';
          if (
            evt.type === 'RUN_STARTED' ||
            evt.type === 'MODEL_COMPLETED' ||
            evt.type === 'RUN_COMPLETED' ||
            evt.type === 'RUN_FAILED'
          ) {
            refreshThreads();
            refreshMessages(evt.threadId);
          }
          if (isResearchEvent) {
            refreshResearchData(evt.runId, evt.threadId);
          }
          if (
            evt.type === 'ARTIFACT_CREATED' ||
            evt.type === 'REPORT_COMPLETED' ||
            evt.type === 'RUN_COMPLETED' ||
            hasArtifactMetadata(evt)
          ) {
            refreshArtifactData(evt.threadId, evt.runId);
          }
        },
        onError: (err) => {
          abortRef.current = null;
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
  }, [refreshArtifactData, refreshMessages, refreshObservabilityData, refreshResearchData, refreshThreads, state.lastRequest, state.threadId]);

  const handleAnswerClarification = useCallback((answer: string, clarification: PendingClarification, answers?: ClarificationAnswer[]) => {
    answerClarification({
      clarificationId: clarification.clarificationId,
      threadId: clarification.threadId || state.threadId,
      answer,
      answers,
    }).then((record) => {
      handleResumeRun(record.runId || clarification.runId);
    }).catch((err) => {
      setExternalMessage(answer);
      dispatch({ type: 'SET_ERROR', payload: (err as Error).message });
    });
  }, [handleResumeRun, state.threadId]);

  const handleSubmitQuestion = useCallback((question: string) => {
    handleRun({
      message: question,
      mode: state.lastRequest?.mode || 'chat',
    });
  }, [handleRun, state.lastRequest?.mode]);

  const handleStop = useCallback(() => {
    const runId = state.runId;
    if (runId) {
      void cancelRun(runId).catch(() => {
        // The local stream is still stopped even if the cancel request races the server terminal state.
      }).finally(() => {
        refreshThreads();
        if (state.threadId) {
          void refreshMessages(state.threadId);
        }
      });
    }
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    dispatch({ type: 'STOP_RUN' });
  }, [refreshMessages, refreshThreads, state.runId, state.threadId]);

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
    dispatch({ type: 'SET_RUN_OBSERVABILITY' });
    dispatch({ type: 'SET_THREAD_ID', payload: threadId });
    dispatch({ type: 'SET_LAST_REQUEST' });
    allowArtifactAutoOpenRef.current = false;
    setCanvasArtifactId(null);
    setIsSidebarOpen(false);
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
    dispatch({ type: 'SET_RUN_OBSERVABILITY' });
    dispatch({ type: 'SET_THREAD_ID' });
    dispatch({ type: 'SET_LAST_REQUEST' });
    allowArtifactAutoOpenRef.current = false;
    setCanvasArtifactId(null);
    setIsSidebarOpen(false);
  }, []);

  return (
    <div className="app">
      <Header
        backendStatus={backendStatus}
        runStatus={state.status}
        onOpenMemorySettings={() => setIsMemoryOpen(true)}
        hasPendingClarification={!!pendingClarification}
        isSidebarOpen={isSidebarOpen}
        isTraceOpen={isTraceOpen}
        onToggleSidebar={toggleSidebar}
        onToggleTrace={toggleTrace}
      />
      {state.threadId && (
        <div className="mobile-drawer-bar" aria-label="Mobile panels">
          <button
            type="button"
            className={`mobile-drawer-btn ${isSidebarOpen ? 'active' : ''}`}
            onClick={toggleSidebar}
          >
            <Menu size={16} />
            <span>Threads</span>
            <span className="mobile-drawer-count">{state.threads.length}</span>
          </button>
          <button
            type="button"
            className={`mobile-drawer-btn ${isTraceOpen ? 'active' : ''}`}
            onClick={toggleTrace}
          >
            <Activity size={16} />
            <span>Activity</span>
            <span className="mobile-drawer-count">{state.events.length}</span>
          </button>
        </div>
      )}
      <div className={`main ${state.threadId ? 'thread-selected' : 'thread-empty'}`}>
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
          isOpen={isSidebarOpen}
          onClose={() => setIsSidebarOpen(false)}
        />
        <div className="workspace">
          <AnswerWorkspace
            phase={state.phase}
            status={state.status}
            messages={state.messages}
            events={state.events}
            threadId={state.threadId}
            finalAnswer={state.finalAnswer}
            error={state.error}
            onReRun={state.status === 'failed' ? handleReRun : undefined}
            onRefreshMessage={handleRefreshMessage}
            onResumeRun={handleResumeRun}
            pendingClarification={pendingClarification}
            onAnswerClarification={handleAnswerClarification}
            onSubmitQuestion={handleSubmitQuestion}
            onOpenCanvas={handleOpenCanvas}
          />
          {hasResearchPanelData && (
            <ResearchInspector
              sources={state.sources || []}
              evidenceItems={state.evidenceItems || []}
              workItems={state.workItems || []}
              claims={state.claims || []}
              citations={state.citations || []}
              budgetLedger={state.budgetLedger || null}
              qualityAssessment={state.qualityAssessment || null}
              skillActivations={state.skillActivations || []}
            />
          )}
          <ArtifactPanel artifacts={state.artifacts} onOpenCanvas={handleOpenCanvas} />
          <TaskComposer
            onRun={handleRun}
            onAnswerClarification={handleAnswerClarification}
            onStop={handleStop}
            isRunning={state.status === 'running'}
            status={state.status}
            lastRequest={state.lastRequest}
            selectedUploadCount={state.selectedUploadIds.length}
            externalMessage={externalMessage}
            onClearExternalMessage={() => setExternalMessage(undefined)}
            pendingClarification={pendingClarification}
            activeThreadId={state.threadId}
          />
        </div>
        <ActivityTrace
          events={state.events}
          isOpen={isTraceOpen}
          onClose={() => setIsTraceOpen(false)}
          todoSnapshot={state.todoSnapshot}
          todoGateBlocked={state.todoGateBlocked}
          todoGateMessage={state.todoGateMessage}
          observability={state.runObservability}
        />
      </div>
      {/* Sidebar/Drawer Backdrop */}
      {(isSidebarOpen || isTraceOpen) && (
        <div
          className="sidebar-backdrop"
          onClick={() => {
            setIsSidebarOpen(false);
            setIsTraceOpen(false);
          }}
        />
      )}
      {canvasArtifactId && (
        <ArtifactCanvas
          artifacts={state.artifacts}
          selectedArtifactId={canvasArtifactId}
          onSelectArtifact={setCanvasArtifactId}
          onClose={handleCloseCanvas}
        />
      )}
      {isMemoryOpen && <MemorySettingsModal onClose={() => setIsMemoryOpen(false)} />}
    </div>
  );
}

function getPendingClarification(
  messages: Array<{ role: string; runId: string; metadata?: Record<string, unknown> }>,
  status: string,
  threadId?: string
): PendingClarification | undefined {
  if (status !== 'suspended' && status !== 'failed') {
    return undefined;
  }
  for (let i = messages.length - 1; i >= 0; i -= 1) {
    const message = messages[i];
    if (message.role !== 'SYSTEM' || !message.metadata?.clarificationPending) {
      continue;
    }
    const metadata = message.metadata;
    const clarificationId = typeof metadata.clarificationId === 'string' && metadata.clarificationId
      ? metadata.clarificationId
      : undefined;
    return {
      clarificationId,
      runId: typeof metadata.resumeRunId === 'string' ? metadata.resumeRunId : message.runId,
      threadId: typeof metadata.resumeThreadId === 'string' ? metadata.resumeThreadId : threadId,
      question: typeof metadata.question === 'string' ? metadata.question : undefined,
      questions: parseClarificationQuestions(metadata.questions),
    };
  }
  return undefined;
}

function parseClarificationQuestions(value: unknown): ClarificationQuestion[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }
  const questions = value
    .map((item, index) => {
      if (!item || typeof item !== 'object') {
        return undefined;
      }
      const record = item as Record<string, unknown>;
      const choices = Array.isArray(record.choices)
        ? record.choices
          .map((choice, choiceIndex) => {
            if (!choice || typeof choice !== 'object') return undefined;
            const choiceRecord = choice as Record<string, unknown>;
            const text = stringValue(choiceRecord.text) || stringValue(choiceRecord.label);
            if (!text) return undefined;
            return {
              id: stringValue(choiceRecord.id) || String.fromCharCode(97 + choiceIndex),
              label: stringValue(choiceRecord.label) || String.fromCharCode(65 + choiceIndex),
              text,
            };
          })
          .filter((choice): choice is { id: string; label: string; text: string } => !!choice)
        : [];
      return {
        id: stringValue(record.id) || `q${index + 1}`,
        title: stringValue(record.title) || stringValue(record.prompt) || `闂 ${index + 1}`,
        prompt: stringValue(record.prompt) || stringValue(record.title) || '',
        answerType: stringValue(record.answerType) || stringValue(record.answer_type) || (choices.length ? 'SINGLE_CHOICE_WITH_CUSTOM' : 'TEXT'),
        choices,
        allowCustom: choices.length === 0 || booleanValue(record.allowCustom, booleanValue(record.allow_custom, true)),
        required: booleanValue(record.required, true),
      };
    })
    .filter((question): question is ClarificationQuestion => !!question);
  return questions.length ? questions : undefined;
}

function stringValue(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function booleanValue(value: unknown, fallback: boolean) {
  return typeof value === 'boolean' ? value : fallback;
}

function getThreadIdFromUrl() {
  if (typeof window === 'undefined') {
    return undefined;
  }
  return new URLSearchParams(window.location.search).get('threadId') || undefined;
}

function hasArtifactMetadata(evt: { metadata?: Record<string, unknown> }) {
  return typeof evt.metadata?.artifactId === 'string' && evt.metadata.artifactId.length > 0;
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
