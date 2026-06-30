import { Play, Square, RotateCcw, ChevronDown, Copy, Check, Search, MessageCircle } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import type { RunRequest } from '../types';

interface TaskComposerProps {
  onRun: (req: RunRequest) => void;
  onStop: () => void;
  isRunning: boolean;
  lastRequest?: RunRequest;
  selectedUploadCount?: number;
}

export default function TaskComposer({
  onRun,
  onStop,
  isRunning,
  lastRequest,
  selectedUploadCount = 0,
}: TaskComposerProps) {
  const [message, setMessage] = useState('');
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [threadId, setThreadId] = useState('');
  const [model, setModel] = useState('');
  const [mode, setMode] = useState<'chat' | 'research'>('chat');
  const [depth, setDepth] = useState<'quick' | 'standard' | 'deep'>('standard');
  const [maxSources, setMaxSources] = useState(10);
  const [outputAsReport, setOutputAsReport] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (lastRequest) {
      setMessage(lastRequest.message);
      setThreadId(lastRequest.threadId || '');
      setModel(lastRequest.model || '');
      setMode(lastRequest.mode || 'chat');
      setDepth(lastRequest.researchOptions?.depth || 'standard');
      setMaxSources(lastRequest.researchOptions?.maxSources || 10);
      setOutputAsReport(lastRequest.researchOptions?.outputFormat === 'report');
    }
  }, [lastRequest]);

  const handleRun = () => {
    if (!message.trim() || isRunning) return;
    const req: RunRequest = {
      message: message.trim(),
      threadId: threadId.trim() || undefined,
      model: model.trim() || undefined,
      mode,
    };
    if (mode === 'research') {
      req.researchOptions = {
        depth,
        maxSources,
        outputFormat: outputAsReport ? 'report' : 'answer',
      };
    }
    onRun(req);
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
        placeholder={mode === 'research'
          ? "Ask DeerFlow to research a topic deeply..."
          : "Ask DeerFlow to inspect this workspace and summarize the project."}
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
            {mode === 'research' ? <Search size={16} /> : <Play size={16} />}
            {mode === 'research' ? 'Research' : 'Run'}
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
          {selectedUploadCount > 0 && (
            <span className="selected-files-badge composer-badge">
              {selectedUploadCount} file{selectedUploadCount > 1 ? 's' : ''} selected
            </span>
          )}
        </div>
        <div className="composer-right-actions">
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
          <div className="advanced-row">
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

          <div className="advanced-row">
            <div className="mode-toggle">
              <label className="field-label">Mode</label>
              <div className="mode-buttons">
                <button
                  type="button"
                  className={`mode-btn ${mode === 'chat' ? 'active' : ''}`}
                  onClick={() => setMode('chat')}
                  disabled={isRunning}
                >
                  <MessageCircle size={14} />
                  Chat
                </button>
                <button
                  type="button"
                  className={`mode-btn ${mode === 'research' ? 'active' : ''}`}
                  onClick={() => setMode('research')}
                  disabled={isRunning}
                >
                  <Search size={14} />
                  Research
                </button>
              </div>
            </div>
          </div>

          {mode === 'research' && (
            <div className="research-options">
              <div className="advanced-row">
                <div>
                  <label className="field-label">Depth</label>
                  <select
                    className="field-input"
                    value={depth}
                    onChange={(e) => setDepth(e.target.value as 'quick' | 'standard' | 'deep')}
                    disabled={isRunning}
                  >
                    <option value="quick">Quick</option>
                    <option value="standard">Standard</option>
                    <option value="deep">Deep</option>
                  </select>
                </div>
                <div>
                  <label className="field-label">Max Sources</label>
                  <input
                    className="field-input"
                    type="number"
                    min={1}
                    max={50}
                    value={maxSources}
                    onChange={(e) => setMaxSources(Math.max(1, Math.min(50, parseInt(e.target.value) || 10)))}
                    disabled={isRunning}
                  />
                </div>
              </div>
              <div className="advanced-row">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={outputAsReport}
                    onChange={(e) => setOutputAsReport(e.target.checked)}
                    disabled={isRunning}
                  />
                  Output as report
                </label>
              </div>
            </div>
          )}
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
