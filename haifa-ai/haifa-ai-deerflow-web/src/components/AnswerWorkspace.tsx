import { useRef, useEffect, useState } from 'react';
import { Inbox, Loader2, Copy, Check, AlertTriangle, RotateCcw } from 'lucide-react';
import type { AppPhase, AppStatus, MessageRecord } from '../types';
import { renderMarkdown } from '../utils/markdownRenderer';

interface AnswerWorkspaceProps {
  phase: AppPhase;
  status: AppStatus;
  messages: MessageRecord[];
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
  messages,
  finalAnswer,
  error,
  onReRun,
}: AnswerWorkspaceProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const [copied, setCopied] = useState(false);

  const visibleMessages = messages.filter((message) =>
    message.role === 'USER' || message.role === 'ASSISTANT'
  );
  const hasAssistantContent = visibleMessages.some((message) => message.role === 'ASSISTANT');

  useEffect(() => {
    if (panelRef.current) {
      panelRef.current.scrollTop = panelRef.current.scrollHeight;
    }
  }, [visibleMessages.length, status]);

  const handleCopy = async () => {
    const assistantMessages = visibleMessages
      .filter((message) => message.role === 'ASSISTANT')
      .map((message) => message.content)
      .join('\n\n');
    const copyText = assistantMessages || finalAnswer;
    if (!copyText) return;
    try {
      await navigator.clipboard.writeText(copyText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  };

  const isEmpty = status === 'idle' && visibleMessages.length === 0 && !finalAnswer && !error;

  return (
    <div className="answer-panel" ref={panelRef}>
      <div className="answer-panel-header">
        <div className="answer-panel-title">Conversation</div>
        {hasAssistantContent && (
          <button type="button" className="btn btn-ghost" onClick={handleCopy}>
            {copied ? <Check size={16} /> : <Copy size={16} />}
            {copied ? 'Copied' : 'Copy answers'}
          </button>
        )}
      </div>

      {isEmpty ? (
        <div className="answer-empty">
          <Inbox size={40} className="empty-icon" />
          <span>Select a thread or send a message to start a conversation.</span>
        </div>
      ) : (
        <>
          {visibleMessages.length > 0 && (
            <div className="conversation-list">
              {visibleMessages.map((message) => (
                <div
                  key={message.messageId}
                  className={`conversation-message ${message.role.toLowerCase()}`}
                >
                  <div className="conversation-message-role">
                    {message.role === 'USER' ? 'You' : 'DeerFlow'}
                  </div>
                  <div
                    className="conversation-message-content"
                    dangerouslySetInnerHTML={{ __html: renderMarkdown(message.content) }}
                  />
                </div>
              ))}
            </div>
          )}

          {status === 'running' && (
            <div className={`phase-indicator ${phase}`}>
              <div className="spinner" />
              {phaseLabels[phase]}
            </div>
          )}

          {status === 'stopped' && (
            <div className="stopped-card">
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

          {visibleMessages.length === 0 && finalAnswer && (
            <div className="answer-content" style={{ whiteSpace: 'pre-wrap' }}>
              {finalAnswer}
            </div>
          )}
        </>
      )}
    </div>
  );
}
