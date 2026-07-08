import type {
  ArtifactRecord,
  DeerFlowEvent,
  EvidenceItem,
  MessageListResponse,
  QualityGateResult,
  ResearchPlan,
  ResearchProgress,
  ResearchSource,
  RunRequest,
  RunResponse,
  ThreadListResponse,
  ThreadRecord,
  ClarificationAnswer,
  TodoSnapshot,
} from '../types';


export function getUserId(): string {
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);
    const userId = params.get('userId') || localStorage.getItem('userId');
    if (userId) {
      return userId;
    }
  }
  return 'default-user';
}

async function fetchWithUser(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const headers = new Headers(init?.headers);
  headers.set('X-User-Id', getUserId());
  return fetch(input, {
    ...init,
    headers,
  });
}

export interface StreamHandlers {
  onEvent: (event: DeerFlowEvent) => void;
  onError: (error: string) => void;
  onDone: () => void;
}

export async function readDeerFlowStream(
  payload: RunRequest,
  handlers: StreamHandlers,
  signal: AbortSignal
): Promise<void> {
  const response = await fetchWithUser('/api/deerflow/runs/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify(payload),
    signal,
  });

  if (!response.ok) {
    const text = await response.text().catch(() => 'Unknown error');
    handlers.onError(`HTTP ${response.status}: ${text}`);
    return;
  }

  if (!response.body) {
    handlers.onError('No response body');
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let currentData = '';
  let currentEvent = '';
  let currentId = '';

  function dispatchEvent() {
    if (currentData) {
      try {
        const parsed: DeerFlowEvent = JSON.parse(currentData);
        handlers.onEvent(parsed);
      } catch (e) {
        handlers.onError(
          `JSON parse error: ${(e as Error).message}. Raw: ${currentData}`
        );
      }
    }
    currentData = '';
    currentEvent = '';
    currentId = '';
  }

  try {
    while (!signal.aborted) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      // Normalize CRLF to LF for consistent splitting
      let normalized = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
      const lines = normalized.split('\n');
      // Keep the last (possibly incomplete) line in the buffer
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line === '') {
          // Empty line = dispatch boundary
          dispatchEvent();
        } else if (line.startsWith('data:')) {
          const value = line.slice(5).startsWith(' ') ? line.slice(6) : line.slice(5);
          currentData += (currentData ? '\n' : '') + value;
        } else if (line.startsWith('event:')) {
          const value = line.slice(6).startsWith(' ') ? line.slice(7) : line.slice(6);
          currentEvent = value;
        } else if (line.startsWith('id:')) {
          const value = line.slice(3).startsWith(' ') ? line.slice(4) : line.slice(3);
          currentId = value;
        } else {
          // Unrecognized line — treat as part of data if we have an active data block
          if (currentData) {
            currentData += '\n' + line;
          }
          // Otherwise ignore per SSE spec
        }
      }
    }

    // Flush remaining buffer
    if (buffer) {
      const line = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
      if (line === '') {
        dispatchEvent();
      } else if (line.startsWith('data:')) {
        const value = line.slice(5).startsWith(' ') ? line.slice(6) : line.slice(5);
        currentData += (currentData ? '\n' : '') + value;
        dispatchEvent();
      } else if (line.startsWith('event:')) {
        const value = line.slice(6).startsWith(' ') ? line.slice(7) : line.slice(6);
        currentEvent = value;
        dispatchEvent();
      } else if (line.startsWith('id:')) {
        const value = line.slice(3).startsWith(' ') ? line.slice(4) : line.slice(3);
        currentId = value;
        dispatchEvent();
      } else if (currentData) {
        currentData += '\n' + line;
        dispatchEvent();
      }
    }

    // Final dispatch if there's a pending event without trailing newline
    if (currentData || currentEvent || currentId) {
      dispatchEvent();
    }
  } catch (err) {
    if (signal.aborted) {
      return;
    }
    handlers.onError(`Stream error: ${(err as Error).message}`);
  } finally {
    reader.releaseLock();
  }

  handlers.onDone();
}

export async function readDeerFlowResumeStream(
  runId: string,
  handlers: StreamHandlers,
  signal: AbortSignal
): Promise<void> {
  const response = await fetchWithUser(`/api/deerflow/runs/${encodeURIComponent(runId)}/resume`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
    },
    signal,
  });

  if (!response.ok) {
    const text = await response.text().catch(() => 'Unknown error');
    handlers.onError(`HTTP ${response.status}: ${text}`);
    return;
  }

  if (!response.body) {
    handlers.onError('No response body');
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let currentData = '';
  let currentEvent = '';
  let currentId = '';

  function dispatchEvent() {
    if (currentData) {
      try {
        const parsed: DeerFlowEvent = JSON.parse(currentData);
        handlers.onEvent(parsed);
      } catch (e) {
        handlers.onError(
          `JSON parse error: ${(e as Error).message}. Raw: ${currentData}`
        );
      }
    }
    currentData = '';
    currentEvent = '';
    currentId = '';
  }

  try {
    while (!signal.aborted) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      let normalized = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
      const lines = normalized.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line === '') {
          dispatchEvent();
        } else if (line.startsWith('data:')) {
          const value = line.slice(5).startsWith(' ') ? line.slice(6) : line.slice(5);
          currentData += (currentData ? '\n' : '') + value;
        } else if (line.startsWith('event:')) {
          const value = line.slice(6).startsWith(' ') ? line.slice(7) : line.slice(6);
          currentEvent = value;
        } else if (line.startsWith('id:')) {
          const value = line.slice(3).startsWith(' ') ? line.slice(4) : line.slice(3);
          currentId = value;
        } else {
          if (currentData) {
            currentData += '\n' + line;
          }
        }
      }
    }

    if (buffer) {
      const line = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
      if (line === '') {
        dispatchEvent();
      } else if (line.startsWith('data:')) {
        const value = line.slice(5).startsWith(' ') ? line.slice(6) : line.slice(5);
        currentData += (currentData ? '\n' : '') + value;
        dispatchEvent();
      } else if (line.startsWith('event:')) {
        const value = line.slice(6).startsWith(' ') ? line.slice(7) : line.slice(6);
        currentEvent = value;
        dispatchEvent();
      } else if (line.startsWith('id:')) {
        const value = line.slice(3).startsWith(' ') ? line.slice(4) : line.slice(3);
        currentId = value;
        dispatchEvent();
      } else if (currentData) {
        currentData += '\n' + line;
        dispatchEvent();
      }
    }

    if (currentData || currentEvent || currentId) {
      dispatchEvent();
    }
  } catch (err) {
    if (signal.aborted) {
      return;
    }
    handlers.onError(`Stream error: ${(err as Error).message}`);
  } finally {
    reader.releaseLock();
  }

  handlers.onDone();
}

interface ClarificationRecord {
  clarificationId: string;
  runId: string;
  threadId: string;
  question: string;
  answer?: string;
  status: string;
}

export async function answerClarification(params: {
  clarificationId?: string;
  threadId?: string;
  answer: string;
  answers?: ClarificationAnswer[];
}): Promise<ClarificationRecord> {
  let clarificationId = params.clarificationId;
  if (!clarificationId && params.threadId) {
    const pendingRes = await fetchWithUser(
      `/api/deerflow/clarifications/pending?threadId=${encodeURIComponent(params.threadId)}`
    );
    if (!pendingRes.ok) {
      const text = await pendingRes.text().catch(() => 'Unknown error');
      throw new Error(`HTTP ${pendingRes.status}: ${text}`);
    }
    const pending = await pendingRes.json() as ClarificationRecord;
    clarificationId = pending.clarificationId;
  }
  if (!clarificationId) {
    throw new Error('No pending clarification id found');
  }

  const res = await fetchWithUser(`/api/deerflow/clarifications/${encodeURIComponent(clarificationId)}/answer`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ answer: params.answer, answers: params.answers }),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => 'Unknown error');
    throw new Error(`HTTP ${res.status}: ${text}`);
  }
  return await res.json() as ClarificationRecord;
}

export async function fetchRunStatus(runId: string): Promise<unknown> {
  const res = await fetchWithUser(`/api/deerflow/runs/${runId}`);
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchRunEvents(runId: string): Promise<DeerFlowEvent[]> {
  const res = await fetchWithUser(`/api/deerflow/runs/${encodeURIComponent(runId)}/events`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchRunTodos(runId: string): Promise<TodoSnapshot> {
  const res = await fetchWithUser(`/api/deerflow/runs/${encodeURIComponent(runId)}/todos`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}
export async function fetchRunSources(runId: string): Promise<ResearchSource[]> {
  const res = await fetchWithUser(`/api/deerflow/runs/${encodeURIComponent(runId)}/sources`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchRunEvidence(runId: string): Promise<EvidenceItem[]> {
  const res = await fetchWithUser(`/api/deerflow/runs/${encodeURIComponent(runId)}/evidence`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchRunPlan(runId: string): Promise<ResearchPlan> {
  const res = await fetchWithUser(`/api/deerflow/runs/${encodeURIComponent(runId)}/plan`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchRunProgress(runId: string): Promise<ResearchProgress> {
  const res = await fetchWithUser(`/api/deerflow/runs/${encodeURIComponent(runId)}/progress`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchRunQualityGate(runId: string): Promise<QualityGateResult> {
  const res = await fetchWithUser(`/api/deerflow/runs/${encodeURIComponent(runId)}/quality-gate`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchArtifacts(params: { threadId?: string; runId?: string } = {}): Promise<ArtifactRecord[]> {
  const url = new URL('/api/deerflow/artifacts', window.location.origin);
  if (params.threadId) {
    url.searchParams.set('threadId', params.threadId);
  }
  if (params.runId) {
    url.searchParams.set('runId', params.runId);
  }
  const res = await fetchWithUser(url.toString(), { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchArtifact(artifactId: string): Promise<ArtifactRecord> {
  const res = await fetchWithUser(`/api/deerflow/artifacts/${encodeURIComponent(artifactId)}`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export function artifactDownloadUrl(artifactId: string): string {
  return `/api/deerflow/artifacts/${encodeURIComponent(artifactId)}/download`;
}


export async function createThread(payload: {
  threadId?: string;
  title?: string;
  metadata?: Record<string, unknown>;
} = {}): Promise<ThreadRecord> {
  const res = await fetchWithUser('/api/deerflow/threads', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => 'Thread creation failed');
    throw new Error(`HTTP ${res.status}: ${text}`);
  }
  return res.json();
}

export async function listThreads(): Promise<ThreadListResponse> {
  const res = await fetchWithUser('/api/deerflow/threads', { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function listThreadRuns(threadId: string): Promise<{ runs: RunResponse[] }> {
  const res = await fetchWithUser(`/api/deerflow/threads/${encodeURIComponent(threadId)}/runs`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function listThreadMessages(threadId: string): Promise<MessageListResponse> {
  const res = await fetchWithUser(`/api/deerflow/threads/${encodeURIComponent(threadId)}/messages`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function checkBackendHealth(): Promise<boolean> {
  try {
    const res = await fetchWithUser('/api/deerflow/health', {
      method: 'GET',
      signal: AbortSignal.timeout(3000),
    });
    return res.ok || res.status === 404;
  } catch {
    return false;
  }
}

// Upload API
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

export async function uploadFile(
  file: File,
  threadId?: string
): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const url = new URL('/api/deerflow/uploads', window.location.origin);
  if (threadId) {
    url.searchParams.set('threadId', threadId);
  }

  const res = await fetchWithUser(url.toString(), {
    method: 'POST',
    body: formData,
  });

  if (!res.ok) {
    const text = await res.text().catch(() => 'Upload failed');
    throw new Error(`HTTP ${res.status}: ${text}`);
  }

  return res.json();
}

export async function listUploads(threadId?: string): Promise<UploadListResponse> {
  const url = new URL('/api/deerflow/uploads', window.location.origin);
  if (threadId) {
    url.searchParams.set('threadId', threadId);
  }

  const res = await fetchWithUser(url.toString(), { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function getUpload(fileId: string, threadId?: string): Promise<UploadRecord> {
  const url = new URL(`/api/deerflow/uploads/${fileId}`, window.location.origin);
  if (threadId) {
    url.searchParams.set('threadId', threadId);
  }
  const res = await fetchWithUser(url.toString(), { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function getUploadContent(fileId: string, threadId?: string): Promise<UploadContentResponse> {
  const url = new URL(`/api/deerflow/uploads/${fileId}/content`, window.location.origin);
  if (threadId) {
    url.searchParams.set('threadId', threadId);
  }
  const res = await fetchWithUser(url.toString(), { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function deleteUpload(fileId: string, threadId?: string): Promise<void> {
  const url = new URL(`/api/deerflow/uploads/${fileId}`, window.location.origin);
  if (threadId) {
    url.searchParams.set('threadId', threadId);
  }
  const res = await fetchWithUser(url.toString(), { method: 'DELETE' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
}

// Persona & Memory APIs
import type { AgentPersona, MemoryFact, MemoryCandidate } from '../types';

export async function fetchPersonas(): Promise<AgentPersona[]> {
  const res = await fetchWithUser('/api/deerflow/persona', { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function savePersona(persona: AgentPersona): Promise<AgentPersona> {
  const res = await fetchWithUser('/api/deerflow/persona', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(persona),
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchMemoryFacts(status?: string): Promise<MemoryFact[]> {
  const url = new URL('/api/deerflow/memory/facts', window.location.origin);
  if (status) {
    url.searchParams.set('status', status);
  }
  const res = await fetchWithUser(url.toString(), { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function createMemoryFact(fact: Partial<MemoryFact>): Promise<MemoryFact> {
  const res = await fetchWithUser('/api/deerflow/memory/facts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(fact),
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function updateMemoryFact(id: string, fact: Partial<MemoryFact>): Promise<MemoryFact> {
  const res = await fetchWithUser(`/api/deerflow/memory/facts/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(fact),
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function deleteMemoryFact(id: string): Promise<void> {
  const res = await fetchWithUser(`/api/deerflow/memory/facts/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
}

export async function fetchMemoryCandidates(status: string = 'pending'): Promise<MemoryCandidate[]> {
  const url = new URL('/api/deerflow/memory/candidates', window.location.origin);
  url.searchParams.set('status', status);
  const res = await fetchWithUser(url.toString(), { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function approveMemoryCandidate(id: string): Promise<MemoryFact> {
  const res = await fetchWithUser(`/api/deerflow/memory/candidates/${encodeURIComponent(id)}/approve`, {
    method: 'POST',
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function rejectMemoryCandidate(id: string): Promise<MemoryCandidate> {
  const res = await fetchWithUser(`/api/deerflow/memory/candidates/${encodeURIComponent(id)}/reject`, {
    method: 'POST',
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

