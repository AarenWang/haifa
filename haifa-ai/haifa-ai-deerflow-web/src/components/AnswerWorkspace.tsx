import { useRef, useEffect, useState, useCallback } from 'react';
import { Inbox, Loader2, Copy, Check, AlertTriangle, RotateCcw, RefreshCw, Brain, BookOpen, Search, Globe, Terminal, Tag, Sparkles, ChevronDown, ChevronUp } from 'lucide-react';
import type { AppPhase, AppStatus, ClarificationAnswer, MessageRecord, DeerFlowEvent } from '../types';
import { renderMarkdown } from '../utils/markdownRenderer';
import ApprovalCard from './ApprovalCard';
import ClarificationCard from './ClarificationCard';
import type { PendingClarification } from './TaskComposer';

interface AnswerWorkspaceProps {
  phase: AppPhase;
  status: AppStatus;
  messages: MessageRecord[];
  events: DeerFlowEvent[];
  threadId?: string;
  finalAnswer?: string;
  error?: string;
  onReRun?: () => void;
  onRefreshMessage?: (messageId: string) => void;
  onResumeRun?: (runId: string) => void;
  pendingClarification?: PendingClarification;
  onAnswerClarification?: (answer: string, clarification: PendingClarification, answers?: ClarificationAnswer[]) => void;
  onSubmitQuestion?: (question: string) => void;
}

interface ExecutionStep {
  id: string;
  type: string;
  name: string;
  detail: string;
  status: 'running' | 'completed' | 'failed' | 'denied';
  durationMs?: number;
}

const parseExecutionSteps = (events: DeerFlowEvent[]): ExecutionStep[] => {
  const stepsMap: Record<string, ExecutionStep> = {};
  const orderedIds: string[] = [];

  (events || []).forEach((evt) => {
    const meta = evt.metadata || {};
    const toolCallId = (meta.toolCallId as string) || evt.eventId;

    if (evt.type === 'TOOL_STARTED') {
      const toolName = (meta.toolName as string) || '';
      const rawArgs = meta.arguments;
      let argString = '';
      if (rawArgs) {
        if (typeof rawArgs === 'string') {
          argString = rawArgs;
        } else {
          argString = JSON.stringify(rawArgs);
        }
      }

      let detail = argString;
      if (toolName === 'read_file' || toolName === 'view_file') {
        try {
          const parsed = typeof rawArgs === 'string' ? JSON.parse(rawArgs) : rawArgs;
          detail = parsed.path || parsed.TargetFile || argString;
        } catch {
          // ignore
        }
      } else if (toolName === 'web_search') {
        try {
          const parsed = typeof rawArgs === 'string' ? JSON.parse(rawArgs) : rawArgs;
          detail = parsed.query || argString;
        } catch {
          // ignore
        }
      } else if (toolName === 'web_fetch' || toolName === 'read_url_content') {
        try {
          const parsed = typeof rawArgs === 'string' ? JSON.parse(rawArgs) : rawArgs;
          detail = parsed.url || argString;
        } catch {
          // ignore
        }
      } else if (toolName === 'run_script') {
        try {
          const parsed = typeof rawArgs === 'string' ? JSON.parse(rawArgs) : rawArgs;
          detail = parsed.language ? `${parsed.language} script` : 'script';
        } catch {
          // ignore
        }
      }

      if (!stepsMap[toolCallId]) {
        orderedIds.push(toolCallId);
      }

      stepsMap[toolCallId] = {
        id: toolCallId,
        type: toolName,
        name: toolName,
        detail: detail,
        status: 'running',
      };
    } else if (evt.type === 'TOOL_COMPLETED') {
      const step = stepsMap[toolCallId];
      if (step) {
        step.status = 'completed';
        step.durationMs = Number(meta.durationMs) || undefined;
      }
    } else if (evt.type === 'TOOL_DENIED') {
      const step = stepsMap[toolCallId];
      if (step) {
        step.status = 'denied';
        step.detail = `${step.detail} (Blocked: ${meta.deniedReason || ''})`;
      }
    }
  });

  return orderedIds.map((id) => stepsMap[id]);
};

const getStepIcon = (type: string) => {
  switch (type) {
    case 'read_file':
    case 'view_file':
      return <BookOpen size={14} style={{ marginRight: 6, verticalAlign: 'middle', color: 'var(--amber)' }} />;
    case 'web_search':
      return <Search size={14} style={{ marginRight: 6, verticalAlign: 'middle', color: 'var(--blue)' }} />;
    case 'web_fetch':
    case 'read_url_content':
      return <Globe size={14} style={{ marginRight: 6, verticalAlign: 'middle', color: 'var(--green)' }} />;
    case 'run_script':
      return <Terminal size={14} style={{ marginRight: 6, verticalAlign: 'middle', color: 'var(--purple)' }} />;
    default:
      return <Terminal size={14} style={{ marginRight: 6, verticalAlign: 'middle', color: 'var(--gray)' }} />;
  }
};

const getStepName = (type: string) => {
  switch (type) {
    case 'read_file':
    case 'view_file':
      return '读取文件';
    case 'web_search':
      return '在网络上搜索';
    case 'web_fetch':
    case 'read_url_content':
      return '查看网页';
    case 'run_script':
      return '运行脚本';
    default:
      return type || '执行步骤';
  }
};



export default function AnswerWorkspace({
  phase,
  status,
  messages,
  events,
  threadId,
  finalAnswer,
  error,
  onReRun,
  onRefreshMessage,
  onResumeRun,
  pendingClarification,
  onAnswerClarification,
  onSubmitQuestion,
}: AnswerWorkspaceProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const [copied, setCopied] = useState(false);
  const [copiedMsgId, setCopiedMsgId] = useState<string | null>(null);
  const [stepsExpanded, setStepsExpanded] = useState(false);

  // Thinking timer logic
  const [thinkingSeconds, setThinkingSeconds] = useState(0);
  useEffect(() => {
    if (status === 'running') {
      setThinkingSeconds(0);
      const interval = setInterval(() => {
        setThinkingSeconds((s) => s + 1);
      }, 1000);
      return () => clearInterval(interval);
    } else {
      setThinkingSeconds(0);
    }
  }, [status]);

  // Recommended questions logic
  const [recommendations, setRecommendations] = useState<string[]>([]);
  const [loadingRecommendations, setLoadingRecommendations] = useState(false);
  const fetchedThreadId = useRef<string | null>(null);

  useEffect(() => {
    if ((status === 'completed' || phase === 'done') && threadId) {
      if (fetchedThreadId.current === threadId && recommendations.length > 0) {
        return;
      }
      setLoadingRecommendations(true);
      fetchedThreadId.current = threadId;

      fetch(`/api/deerflow/threads/${threadId}/recommend-questions`, {
        method: 'POST',
      })
        .then((res) => res.json())
        .then((data) => {
          if (Array.isArray(data)) {
            setRecommendations(data);
          } else {
            setRecommendations([]);
          }
        })
        .catch(() => {
          setRecommendations([]);
        })
        .finally(() => {
          setLoadingRecommendations(false);
        });
    } else {
      if (status === 'running') {
        setRecommendations([]);
        fetchedThreadId.current = null;
      }
    }
  }, [status, phase, threadId]);

  // Token metrics logic
  const tokenMetrics = (() => {
    let inputTokens = 0;
    let outputTokens = 0;
    let totalTokens = 0;

    (events || []).forEach((evt) => {
      const meta = evt.metadata || {};
      if (meta.promptTokens) inputTokens += Number(meta.promptTokens);
      if (meta.completionTokens) outputTokens += Number(meta.completionTokens);
      if (meta.totalTokens) totalTokens += Number(meta.totalTokens);
      else if (meta.estimated_total_tokens) totalTokens += Number(meta.estimated_total_tokens);
    });

    if (totalTokens === 0 && events && events.length > 0) {
      const totalChars = events.reduce((sum, e) => sum + (e.content?.length || 0), 0);
      totalTokens = Math.round(totalChars / 4);
      inputTokens = Math.round(totalTokens * 0.75);
      outputTokens = Math.round(totalTokens * 0.25);
    }

    const formatNumber = (num: number) => {
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
      }
      return num.toString();
    };

    return {
      input: formatNumber(inputTokens),
      output: formatNumber(outputTokens),
      total: formatNumber(totalTokens),
      rawTotal: totalTokens,
    };
  })();

  // Find the last clarification message's ID if there is a pending clarification
  const pendingClarificationMsgId = (() => {
    if (!pendingClarification) return null;
    for (let i = messages.length - 1; i >= 0; i--) {
      const m = messages[i];
      if (m.role === 'SYSTEM' && m.metadata?.clarificationPending) {
        return m.messageId;
      }
    }
    return null;
  })();

  const latestAssistantIndexByRun = (() => {
    const map: Record<string, number> = {};
    messages.forEach((msg, index) => {
      if (msg.role === 'ASSISTANT' && msg.content.trim() !== '') {
        map[msg.runId || 'default'] = index;
      }
    });
    return map;
  })();

  const visibleMessages = messages.filter((message, index) => {
    if (message.role === 'USER') return true;
    if (message.role === 'SYSTEM' && (Boolean(message.metadata?.clarificationPending) || Boolean(message.metadata?.approvalPending))) {
      return true;
    }
    if (message.role === 'ASSISTANT' && message.content.trim() !== '') {
      const runKey = message.runId || 'default';
      return latestAssistantIndexByRun[runKey] === index;
    }
    return false;
  });

  const latestUserMessageId = (() => {
    for (let i = visibleMessages.length - 1; i >= 0; i--) {
      if (visibleMessages[i].role === 'USER') {
        return visibleMessages[i].messageId;
      }
    }
    return null;
  })();

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
                <div key={message.messageId}>
                  <div
                    className={`conversation-message ${message.role.toLowerCase()}`}
                  >
                    <div className="conversation-message-role">
                      {message.role === 'USER'
                        ? 'You'
                        : message.role === 'SYSTEM'
                          ? (message.metadata?.approvalPending ? 'Approval Request' : 'Clarification')
                          : 'DeerFlow'}
                    </div>
                    {message.metadata?.approvalPending ? (
                      <ApprovalCard
                        approvalId={message.metadata.approvalId as string}
                        runId={message.runId}
                        onResumeRun={onResumeRun}
                      />
                    ) : message.role === 'SYSTEM' && message.metadata?.clarificationPending ? (
                      <ClarificationCard
                        message={message}
                        isPending={pendingClarificationMsgId === message.messageId}
                        onAnswer={(answer, answers) => {
                          if (pendingClarification && onAnswerClarification) {
                            onAnswerClarification(answer, pendingClarification, answers);
                          }
                        }}
                      />
                    ) : (
                      <div
                        className="conversation-message-content"
                        dangerouslySetInnerHTML={{ __html: renderMarkdown(message.content) }}
                      />
                    )}
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

                  {message.messageId === latestUserMessageId && events && events.length > 0 && (
                    <div className="steps-accordion-card">
                      <div
                        className="steps-accordion-header"
                        onClick={() => setStepsExpanded(!stepsExpanded)}
                      >
                        <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          {stepsExpanded ? <ChevronDown size={14} /> : <ChevronUp size={14} />}
                          {stepsExpanded ? '隐藏执行步骤' : '显示执行步骤'}
                        </span>
                        <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                          已执行 {parseExecutionSteps(events).length} 个步骤
                        </span>
                      </div>

                      {stepsExpanded && (
                        <div className="steps-list">
                          {parseExecutionSteps(events).map((step) => (
                            <div className="step-item" key={step.id}>
                              <span className="step-item-left">
                                {getStepIcon(step.type)}
                                <span style={{ fontWeight: 500 }}>{getStepName(step.type)}</span>
                                <span style={{ color: 'var(--text-muted)', marginLeft: '4px', fontSize: '11.5px' }}>
                                  {step.detail}
                                </span>
                              </span>
                              {step.status === 'running' ? (
                                <Loader2 className="animate-spin" size={12} style={{ color: 'var(--blue)' }} />
                              ) : step.durationMs !== undefined ? (
                                <span className="step-item-duration">{step.durationMs}ms</span>
                              ) : (
                                <span className="step-item-duration" style={{ color: 'var(--green)' }}>✓</span>
                              )}
                            </div>
                          ))}
                        </div>
                      )}

                      {status !== 'running' && tokenMetrics.rawTotal > 0 && (
                        <div className="steps-token-dashboard">
                          <Tag size={11} style={{ opacity: 0.6 }} />
                          <span>Tokens 输入: <strong style={{ color: 'var(--text-color)' }}>{tokenMetrics.input}</strong></span>
                          <span style={{ margin: '0 4px', opacity: 0.3 }}>|</span>
                          <span>输出: <strong style={{ color: 'var(--text-color)' }}>{tokenMetrics.output}</strong></span>
                          <span style={{ margin: '0 4px', opacity: 0.3 }}>|</span>
                          <span>总计: <strong style={{ color: 'var(--text-color)' }}>{tokenMetrics.total}</strong></span>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {status === 'running' && (
            <div className="thinking-indicator">
              <Brain className="thinking-icon animate-pulse" size={16} />
              <span style={{ fontWeight: 500 }}>Thinking... ({thinkingSeconds}s)</span>
            </div>
          )}

          {status === 'stopped' && (
            <div className="stopped-card">
              <Loader2 size={16} style={{ opacity: 0.7 }} />
              Stopped locally
            </div>
          )}

          {status === 'suspended' && (
            <div className="stopped-card">
              <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />
              {pendingClarification
                ? 'Execution suspended. Waiting for user clarification...'
                : 'Execution suspended. Waiting for human approval...'}
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

          {/* Follow-up Recommended Questions pill row */}
          {loadingRecommendations && (
            <div className="recommendations-loading" style={{ margin: '12px 0', fontSize: '13px', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Search className="animate-spin" size={14} style={{ color: 'var(--blue)' }} />
              <span>正在生成可能的后续问题...</span>
            </div>
          )}

          {!loadingRecommendations && recommendations.length > 0 && (
            <div className="recommendations-container" style={{ margin: '16px 0 8px 0', borderTop: '1px solid var(--border-color)', paddingTop: '12px' }}>
              <div style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Sparkles size={14} style={{ color: 'var(--blue)' }} />
                推荐问题：
              </div>
              <div style={{ display: 'flex', flexWrap: 'nowrap', gap: '8px', overflowX: 'auto', paddingBottom: '4px' }}>
                {recommendations.slice(0, 3).map((q, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className="recommendation-pill"
                    onClick={() => onSubmitQuestion && onSubmitQuestion(q)}
                    style={{
                      padding: '6px 12px',
                      fontSize: '12px',
                      borderRadius: '16px',
                      border: '1px solid var(--border-color)',
                      background: 'var(--card-bg)',
                      color: 'var(--text-color)',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px',
                      whiteSpace: 'nowrap',
                      flexShrink: 0
                    }}
                  >
                    <Sparkles size={10} style={{ opacity: 0.6, color: 'var(--blue)' }} />
                    {q}
                  </button>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
