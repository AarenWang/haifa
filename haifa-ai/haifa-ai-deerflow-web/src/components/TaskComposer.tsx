import { Play, Square, RotateCcw, Trash2, ChevronDown, Copy, Check } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import type { RunRequest } from '../types';

interface TaskComposerProps {
  onRun: (req: RunRequest) => void;
  onStop: () => void;
  onClear: () => void;
  isRunning: boolean;
  lastRequest?: RunRequest;
}

export default function TaskComposer({
  onRun,
  onStop,
  onClear,
  isRunning,
  lastRequest,
}: TaskComposerProps) {
  const [message, setMessage] = useState('');
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [threadId, setThreadId] = useState('');
  const [model, setModel] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (lastRequest) {
      setMessage(lastRequest.message);
      setThreadId(lastRequest.threadId || '');
      setModel(lastRequest.model || '');
    }
  }, [lastRequest]);

  const handleRun = () => {
    if (!message.trim() || isRunning) return;
    onRun({
      message: message.trim(),
      threadId: threadId.trim() || undefined,
      model: model.trim() || undefined,
    });
    // Do not clear message on run — keep it for re-run
  };

  const handleReRun = () => {
    if (!lastRequest || isRunning) return;
    onRun(lastRequest);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      e.preventDefault();
      handleRun();
    }
  };

  const canRun = !!message.trim() && !isRunning;

  return (
    <div className="composer">
      <textarea
        ref={textareaRef}
        className="composer-textarea"
        placeholder="Ask DeerFlow to inspect this workspace and summarize the project."
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={isRunning}
        rows={4}
      />
      <div className="composer-actions">
        <div className="composer-left-actions">
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleRun}
            disabled={!canRun}
          >
            <Play size={16} />
            Run
          </button>
          {isRunning && (
            <button
              type="button"
              className="btn btn-danger"
              onClick={onStop}
            >
              <Square size={16} />
              Stop
            </button>
          )}
          {lastRequest && !isRunning && (
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleReRun}
              disabled={isRunning}
            >
              <RotateCcw size={16} />
              Re-run
            </button>
          )}
        </div>
        <div className="composer-right-actions">
          <button
            type="button"
            className="btn btn-ghost"
            onClick={onClear}
            title="Clear output"
          >
            <Trash2 size={16} />
            Clear
          </button>
          <button
            type="button"
            className={`advanced-toggle ${advancedOpen ? 'open' : ''}`}
            onClick={() => setAdvancedOpen((v) => !v)}
          >
            Advanced Options
            <ChevronDown size={14} className="chevron" />
          </button>
        </div>
      </div>

      {advancedOpen && (
        <div className="advanced-panel">
          <div>
            <label className="field-label">Thread ID</label>
            <input
              className="field-input"
              placeholder="empty means auto"
              value={threadId}
              onChange={(e) => setThreadId(e.target.value)}
              disabled={isRunning}
            />
          </div>
          <div>
            <label className="field-label">Model</label>
            <input
              className="field-input"
              placeholder="backend default"
              value={model}
              onChange={(e) => setModel(e.target.value)}
              disabled={isRunning}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  };

  return (
    <button type="button" className="btn btn-ghost" onClick={handleCopy}>
      {copied ? <Check size={16} /> : <Copy size={16} />}
      {copied ? 'Copied' : 'Copy answer'}
    </button>
  );
}

export { CopyButton };
