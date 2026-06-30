import { useRef, useEffect, useState } from 'react';
import { Inbox, Loader2, Copy, Check, AlertTriangle, RotateCcw } from 'lucide-react';
import type { AppPhase, AppStatus } from '../types';

interface AnswerWorkspaceProps {
  phase: AppPhase;
  status: AppStatus;
  finalAnswer?: string;
  error?: string;
  onReRun?: () => void;
}

const phaseLabels: Record<AppPhase, string> = {
  idle: 'Waiting',
  preparing: 'Preparing run',
  gathering_context: 'Gathering context',
  thinking: 'Thinking',
  answering: 'Writing answer',
  done: 'Done',
};

export default function AnswerWorkspace({
  phase,
  status,
  finalAnswer,
  error,
  onReRun,
}: AnswerWorkspaceProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (status === 'running' && panelRef.current) {
      panelRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [status]);

  const handleCopy = async () => {
    if (!finalAnswer) return;
    try {
      await navigator.clipboard.writeText(finalAnswer);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  };

  const isEmpty =
    status === 'idle' && !finalAnswer && !error;

  return (
    <div className="answer-panel" ref={panelRef}>
      <div className="answer-panel-header">
        <div className="answer-panel-title">Answer</div>
        {finalAnswer && (
          <button type="button" className="btn btn-ghost" onClick={handleCopy}>
            {copied ? <Check size={16} /> : <Copy size={16} />}
            {copied ? 'Copied' : 'Copy answer'}
          </button>
        )}
      </div>

      {isEmpty ? (
        <div className="answer-empty">
          <Inbox size={40} className="empty-icon" />
          <span>
            Enter a task above and click <strong>Run</strong> to get started.
          </span>
        </div>
      ) : (
        <>
          {status === 'running' && (
            <div className={`phase-indicator ${phase}`}>
              <div className="spinner" />
              {phaseLabels[phase]}
            </div>
          )}

          {status === 'stopped' && (
            <div
              style={{
                padding: '10px 12px',
                borderRadius: 'var(--radius)',
                background: 'var(--accent-amber-light)',
                color: 'var(--accent-amber)',
                fontSize: 14,
                fontWeight: 600,
                marginBottom: 12,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <Loader2 size={16} style={{ opacity: 0.7 }} />
              Stopped locally
            </div>
          )}

          {error && (
            <div className="error-card">
              <div className="error-card-title">
                <AlertTriangle size={16} />
                Run failed
              </div>
              <div>{error}</div>
              {onReRun && (
                <div style={{ marginTop: 10 }}>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={onReRun}
                  >
                    <RotateCcw size={16} />
                    Retry
                  </button>
                </div>
              )}
            </div>
          )}

          {finalAnswer && (
            <div className="answer-content">{finalAnswer}</div>
          )}
        </>
      )}
    </div>
  );
}
