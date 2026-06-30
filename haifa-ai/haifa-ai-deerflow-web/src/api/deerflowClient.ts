import type { DeerFlowEvent, RunRequest } from '../types';

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
  const response = await fetch('/api/deerflow/runs/stream', {
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

export async function fetchRunStatus(runId: string): Promise<unknown> {
  const res = await fetch(`/api/deerflow/runs/${runId}`);
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function checkBackendHealth(): Promise<boolean> {
  try {
    const res = await fetch('/api/deerflow/health', {
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
  if (threadId) {
    formData.append('threadId', threadId);
  }

  const res = await fetch('/api/deerflow/uploads', {
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

  const res = await fetch(url.toString(), { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function getUpload(fileId: string): Promise<UploadRecord> {
  const res = await fetch(`/api/deerflow/uploads/${fileId}`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function getUploadContent(fileId: string): Promise<UploadContentResponse> {
  const res = await fetch(`/api/deerflow/uploads/${fileId}/content`, { method: 'GET' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function deleteUpload(fileId: string): Promise<void> {
  const res = await fetch(`/api/deerflow/uploads/${fileId}`, { method: 'DELETE' });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
}
