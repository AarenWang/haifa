import { useRef, useEffect, useState, useCallback } from 'react';
import { Inbox, Loader2, Copy, Check, AlertTriangle, RotateCcw, RefreshCw } from 'lucide-react';
import type { AppPhase, AppStatus, MessageRecord } from '../types';
import { renderMarkdown } from '../utils/markdownRenderer';

interface AnswerWorkspaceProps {
  phase: AppPhase;
  status: AppStatus;
  messages: MessageRecord[];
  finalAnswer?: string;
  error?: string;
  onReRun?: () => void;
  onRefreshMessage?: (messageId: string) => void;
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
  onRefreshMessage,
}: AnswerWorkspaceProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const [copied, setCopied] = useState(false);
  const [copiedMsgId, setCopiedMsgId] = useState<string | null>(null);

  const visibleMessages = messages.filter((message) =>
    message.role === 'USER' || message.role === 'ASSISTANT'
  );
  const hasAssistantContent = visibleMessages.some((message) => message.role === 'ASSISTANT');

  useEffect(() => {
    if (panelRef.current) {
      panelRef.current.scrollTop = panelRef.current.scrollHeight;
    }
  }, [visibleMessages.length, status]);

  const handleCopyAll = async () => {
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

  const handleCopyMessage = async (content: string, messageId: string) => {
    try {
      await navigator.clipboard.writeText(content);
      setCopiedMsgId(messageId);
      setTimeout(() => setCopiedMsgId((id) => (id === messageId ? null : id)), 2000);
    } catch {
      // ignore
    }
  };

  const handleCodeBlockClick = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    const target = e.target as HTMLElement;
    const btn = target.closest('.copy-code-btn') as HTMLButtonElement | null;
    if (!btn) return;
    const wrapper = btn.closest('.code-block-wrapper') as HTMLElement | null;
    if (!wrapper) return;
    const codeEl = wrapper.querySelector('pre code');
    if (!codeEl) return;
    const text = codeEl.textContent || '';
    navigator.clipboard.writeText(text).catch(() => {});
    const originalText = btn.textContent || '';
    btn.textContent = '已复制';
    setTimeout(() => {
      btn.textContent = originalText;
    }, 2000);
  }, []);

  const isEmpty = status === 'idle' && visibleMessages.length === 0 && !finalAnswer && !error;

  return (
    <div className="answer-panel" ref={panelRef}>
      <div className="answer-panel-header">
        <div className="answer-panel-title">Conversation</div>
        {hasAssistantContent && (
          <button type="button" className="btn btn-ghost" onClick={handleCopyAll}>
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
            <div className="conversation-list" onClick={handleCodeBlockClick}>
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
                  {message.role === 'ASSISTANT' && (
                    <div className="message-actions">
                      <button
                        type="button"
                        className="message-action-btn"
                        onClick={() => handleCopyMessage(message.content, message.messageId)}
                        title="Copy markdown"
                      >
                        {copiedMsgId === message.messageId ? <Check size={14} /> : <Copy size={14} />}
                        {copiedMsgId === message.messageId ? '已复制' : '复制'}
                      </button>
                      {onRefreshMessage && (
                        <button
                          type="button"
                          className="message-action-btn"
                          onClick={() => onRefreshMessage(message.messageId)}
                          title="Regenerate"
                        >
                          <RefreshCw size={14} />
                          刷新
                        </button>
                      )}
                    </div>
                  )}
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
