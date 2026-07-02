import { ArrowUp, Square, RotateCcw, ChevronDown, Copy, Check, Search, MessageCircle, SlidersHorizontal } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import type { AppStatus, RunRequest, ClarificationQuestion, ClarificationAnswer } from '../types';

interface TaskComposerProps {
  onRun: (req: RunRequest) => void;
  onAnswerClarification?: (answer: string, clarification: PendingClarification, answers?: ClarificationAnswer[]) => void;
  onStop: () => void;
  isRunning: boolean;
  status: AppStatus;
  lastRequest?: RunRequest;
  selectedUploadCount?: number;
  externalMessage?: string;
  onClearExternalMessage?: () => void;
  pendingClarification?: PendingClarification;
  activeThreadId?: string;
}

export interface PendingClarification {
  clarificationId?: string;
  runId: string;
  threadId?: string;
  question?: string;
  questions?: ClarificationQuestion[];
}

export default function TaskComposer({
  onRun,
  onAnswerClarification,
  onStop,
  isRunning,
  status,
  lastRequest,
  selectedUploadCount = 0,
  externalMessage,
  onClearExternalMessage,
  pendingClarification,
  activeThreadId,
}: TaskComposerProps) {
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (externalMessage) {
      setMessage(externalMessage);
      if (onClearExternalMessage) {
        onClearExternalMessage();
      }
    }
  }, [externalMessage, onClearExternalMessage]);
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [threadId, setThreadId] = useState('');

  useEffect(() => {
    setThreadId(activeThreadId || '');
  }, [activeThreadId]);

  const [model, setModel] = useState('');
  const [mode, setMode] = useState<'chat' | 'research'>('chat');
  const [depth, setDepth] = useState<'quick' | 'standard' | 'deep'>('standard');
  const [maxSources, setMaxSources] = useState(10);
  const [outputAsReport, setOutputAsReport] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    textarea.style.height = '0px';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 140)}px`;
  }, [message]);

  useEffect(() => {
    if (lastRequest) {
      if (status === 'failed') {
        setMessage(lastRequest.message);
      }
      setThreadId(lastRequest.threadId || '');
      setModel(lastRequest.model || '');
      setMode(lastRequest.mode || 'chat');
      setDepth(lastRequest.researchOptions?.depth || 'standard');
      setMaxSources(lastRequest.researchOptions?.maxSources || 10);
      setOutputAsReport(lastRequest.researchOptions?.outputFormat === 'report');
    }
  }, [lastRequest, status]);

  const handleRun = () => {
    if (!message.trim() || isRunning) return;
    if (pendingClarification && onAnswerClarification) {
      onAnswerClarification(message.trim(), pendingClarification);
      setMessage('');
      return;
    }
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
    setMessage('');
  };

  const handleReRun = () => {
    if (!lastRequest || isRunning) return;
    onRun(lastRequest);
    setMessage('');
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      e.preventDefault();
      handleRun();
    }
  };

  const hasStructuredClarification = !!pendingClarification?.questions?.length;
  const canRun = !!message.trim() && !isRunning && !hasStructuredClarification;
  const submitTitle = pendingClarification
    ? '提交澄清回答'
    : mode === 'research'
      ? 'Start research'
      : 'Send';

  return (
    <div className="composer">
      <div className="composer-input-shell">
        <textarea
          ref={textareaRef}
          className="composer-textarea"
          placeholder={pendingClarification
            ? (hasStructuredClarification ? "请填写上方澄清表单..." : "回答上方澄清问题...")
            : mode === 'research'
              ? "Research a topic deeply..."
              : "Message DeerFlow..."}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={isRunning || hasStructuredClarification}
          rows={1}
        />
        <div className="composer-inline-actions">
          {lastRequest && !isRunning && (
            <button
              type="button"
              className="composer-icon-btn"
              onClick={handleReRun}
              disabled={isRunning}
              title="Re-run last request"
            >
              <RotateCcw size={15} />
            </button>
          )}
          {selectedUploadCount > 0 && (
            <span className="selected-files-badge composer-badge">
              {selectedUploadCount} file{selectedUploadCount > 1 ? 's' : ''} selected
            </span>
          )}
          {!pendingClarification && (
            <button
              type="button"
              className={`advanced-toggle ${advancedOpen ? 'open' : ''}`}
              onClick={() => setAdvancedOpen((v) => !v)}
              title="Advanced options"
            >
              <SlidersHorizontal size={14} />
              Advanced
              <ChevronDown size={14} className="chevron" />
            </button>
          )}
          {isRunning ? (
            <button
              type="button"
              className="composer-send-btn stop"
              onClick={onStop}
              title="Stop"
            >
              <Square size={16} />
            </button>
          ) : (
            <button
              type="button"
              className="composer-send-btn"
              onClick={handleRun}
              disabled={!canRun}
              title={submitTitle}
            >
              {pendingClarification ? <ArrowUp size={18} /> : (mode === 'research' ? <Search size={17} /> : <ArrowUp size={18} />)}
            </button>
          )}
        </div>
      </div>

      {advancedOpen && !pendingClarification && (
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
