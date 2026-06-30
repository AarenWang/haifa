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

  try {
    while (!signal.aborted) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      let currentData = '';

      for (const line of lines) {
        if (line.startsWith('data:')) {
          currentData += (currentData ? '\n' : '') + line.slice(5).trim();
        } else if (line.trim() === '') {
          if (currentData) {
            try {
              const parsed: DeerFlowEvent = JSON.parse(currentData);
              handlers.onEvent(parsed);
            } catch (e) {
              handlers.onError(
                `JSON parse error: ${(e as Error).message}. Raw: ${currentData}`
              );
            }
            currentData = '';
          }
        } else if (line.startsWith('id:') || line.startsWith('event:')) {
          // ignore for now, but reset data on new event boundary
        } else {
          // could be continuation of data field if multi-line
          if (currentData) {
            currentData += '\n' + line;
          }
        }
      }
    }

    // Process remaining buffer
    if (buffer.trim()) {
      const lines = buffer.split('\n');
      let currentData = '';
      for (const line of lines) {
        if (line.startsWith('data:')) {
          currentData += (currentData ? '\n' : '') + line.slice(5).trim();
        }
      }
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
    const res = await fetch('/api/deerflow/runs', {
      method: 'HEAD',
      signal: AbortSignal.timeout(3000),
    });
    return res.ok || res.status === 404 || res.status === 405;
  } catch {
    return false;
  }
}
