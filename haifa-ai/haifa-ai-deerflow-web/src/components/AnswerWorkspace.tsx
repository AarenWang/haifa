import { useRef, useEffect, useState, useCallback } from 'react';
import { Inbox, Loader2, Copy, Check, AlertTriangle, RotateCcw, RefreshCw, Brain, BookOpen, Search, Globe, Terminal, Tag, Sparkles, ChevronDown, ChevronUp, FolderOpen, FileCode, Image, Clock, Users } from 'lucide-react';
import type { AppPhase, AppStatus, ClarificationAnswer, MessageRecord, DeerFlowEvent, ArtifactRecord, RunObservability } from '../types';
import { renderMarkdown } from '../utils/markdownRenderer';
import ApprovalCard from './ApprovalCard';
import ClarificationCard from './ClarificationCard';
import type { PendingClarification } from './TaskComposer';

interface AnswerWorkspaceProps {
  phase: AppPhase;
  status: AppStatus;
  messages: MessageRecord[];
  events: DeerFlowEvent[];
  observability?: RunObservability;
  threadId?: string;
  finalAnswer?: string;
  error?: string;
  onReRun?: () => void;
  onRefreshMessage?: (messageId: string) => void;
  onResumeRun?: (runId: string) => void;
  pendingClarification?: PendingClarification;
  onAnswerClarification?: (answer: string, clarification: PendingClarification, answers?: ClarificationAnswer[]) => void;
  onSubmitQuestion?: (question: string) => void;
  onOpenCanvas?: (artifactId?: string) => void;
  artifacts?: ArtifactRecord[];
}

interface ExecutionStep {
  id: string;
  type: string;
  name: string;
  detail: string;
  status: 'running' | 'completed' | 'failed' | 'denied';
  durationMs?: number;
}

type StepArguments = Record<string, unknown>;

const STEP_DETAIL_MAX = 220;
const TOKEN_NUMBER_FORMATTER = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 });

const providerTokenCount = (value: number | null | undefined): number | null => {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) return null;
  return Math.trunc(value);
};

const parseToolArguments = (rawArgs: unknown): StepArguments | undefined => {
  if (!rawArgs) return undefined;
  if (typeof rawArgs === 'object' && !Array.isArray(rawArgs)) return rawArgs as StepArguments;
  if (typeof rawArgs !== 'string') return undefined;
  const trimmed = rawArgs.trim();
  if (!trimmed) return undefined;
  try {
    const parsed = JSON.parse(trimmed);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as StepArguments : undefined;
  } catch {
    return undefined;
  }
};

const textValue = (value: unknown): string => {
  if (value === null || value === undefined) return '';
  if (typeof value === 'string') return value.trim();
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  if (Array.isArray(value)) return value.map(textValue).filter(Boolean).join(', ');
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
};

const firstText = (record: StepArguments | undefined, keys: string[]): string => {
  if (!record) return '';
  for (const key of keys) {
    const value = textValue(record[key]);
    if (value) return value;
  }
  return '';
};

const shortenDetail = (value: string, max = STEP_DETAIL_MAX): string => {
  const compact = value.replace(/\s+/g, ' ').trim();
  return compact.length > max ? `${compact.slice(0, max - 1)}…` : compact;
};

const summarizeRecord = (record: StepArguments | undefined): string => {
  if (!record) return '';
  const pairs = Object.entries(record)
    .filter(([, value]) => value !== undefined && value !== null && textValue(value) !== '')
    .slice(0, 4)
    .map(([key, value]) => `${key}: ${textValue(value)}`);
  return shortenDetail(pairs.join('；'));
};

const summarizeTodos = (args: StepArguments | undefined): string => {
  const todos = Array.isArray(args?.todos) ? args.todos as StepArguments[] : undefined;
  if (!todos) return '读取当前 Todo 列表';
  const current = todos.find((todo) => textValue(todo.status) === 'in_progress');
  const completed = todos.filter((todo) => textValue(todo.status) === 'completed').length;
  const pending = todos.filter((todo) => textValue(todo.status) === 'pending').length;
  const currentText = current ? `，当前进行：${shortenDetail(textValue(current.content), 90)}` : '';
  return `更新 Todo：共 ${todos.length} 项，已完成 ${completed} 项，待处理 ${pending} 项${currentText}`;
};

const summarizeStepDetail = (toolName: string, rawArgs: unknown, metadata: Record<string, unknown>, content?: string): string => {
  const args = parseToolArguments(rawArgs);
  const rawArgText = typeof rawArgs === 'string' ? rawArgs : textValue(rawArgs);

  switch (toolName) {
    case 'web_search': {
      const query = firstText(args, ['query', 'q', 'search_query', 'keyword', 'keywords']) || firstText(metadata, ['query']);
      const maxResults = firstText(args, ['max_results', 'maxResults']) || firstText(metadata, ['maxResults']);
      return query ? `搜索关键词：${query}${maxResults ? `；最多 ${maxResults} 条结果` : ''}` : shortenDetail(rawArgText || content || '执行网络搜索');
    }
    case 'web_fetch':
    case 'read_url_content': {
      const url = firstText(args, ['url', 'Url', 'URL']) || firstText(metadata, ['url']);
      return url ? `读取网页：${url}` : shortenDetail(rawArgText || content || '提取网页内容');
    }
    case 'read_file':
    case 'view_file':
    case 'read_uploaded_file': {
      const file = firstText(args, ['path', 'filepath', 'file_path', 'file', 'AbsolutePath', 'TargetFile', 'fileId', 'fileName']) || firstText(metadata, ['path', 'virtualPath', 'filename', 'fileName']);
      return file ? `读取文件：${file}` : shortenDetail(rawArgText || content || '读取文件内容');
    }
    case 'list_uploaded_files':
      return '列出当前线程上传文件';
    case 'ls':
    case 'list_dir': {
      const dir = firstText(args, ['path', 'directory', 'DirectoryPath']) || firstText(metadata, ['path']);
      return dir ? `查看目录：${dir}` : '查看目录列表';
    }
    case 'glob': {
      const pattern = firstText(args, ['pattern', 'Pattern', 'glob']);
      return pattern ? `匹配文件：${pattern}` : shortenDetail(rawArgText || '按通配符查找文件');
    }
    case 'grep':
    case 'grep_search': {
      const query = firstText(args, ['query', 'Query', 'pattern', 'Pattern', 'regex']);
      const path = firstText(args, ['path', 'include', 'glob']);
      return query ? `搜索文本：${query}${path ? `；范围：${path}` : ''}` : shortenDetail(rawArgText || '在代码或文件中搜索文本');
    }
    case 'write_todos':
      return summarizeTodos(args);
    case 'write_file':
    case 'write_to_file':
    case 'replace_file_content':
    case 'multi_replace_file_content':
    case 'str_replace': {
      const file = firstText(args, ['path', 'file_path', 'TargetFile', 'filepath']) || firstText(metadata, ['path', 'virtualPath', 'filename']);
      return file ? `修改文件：${file}` : shortenDetail(rawArgText || content || '写入或修改文件');
    }
    case 'bash':
    case 'run_command': {
      const command = firstText(args, ['command', 'cmd', 'CommandLine']);
      return command ? `执行命令：${command}` : shortenDetail(rawArgText || content || '执行终端命令');
    }
    case 'run_script': {
      const purpose = firstText(args, ['purpose', 'description']);
      const language = firstText(args, ['language']);
      return shortenDetail([purpose && `目的：${purpose}`, language && `语言：${language}`].filter(Boolean).join('；') || rawArgText || '运行脚本');
    }
    case 'image_search': {
      const query = firstText(args, ['query', 'q']);
      return query ? `搜索图片：${query}` : shortenDetail(rawArgText || '搜索图片素材');
    }
    case 'task': {
      const task = firstText(args, ['task', 'prompt', 'description']);
      return task ? `委派任务：${task}` : shortenDetail(rawArgText || '委派子任务');
    }
    case 'ask_clarification': {
      const question = firstText(args, ['question', 'prompt']);
      return question ? `请求澄清：${question}` : shortenDetail(rawArgText || '请求用户补充信息');
    }
    default:
      return summarizeRecord(args) || shortenDetail(firstText(metadata, ['description']) || content || rawArgText || '执行工具调用');
  }
};

const parseExecutionSteps = (events: DeerFlowEvent[]): ExecutionStep[] => {
  const stepsMap: Record<string, ExecutionStep> = {};
  const orderedIds: string[] = [];

  (events || []).forEach((evt) => {
    const meta = evt.metadata || {};
    const toolCallId = (meta.toolCallId as string) || evt.eventId;
    const toolName = ((meta.toolName as string) || (meta.tool as string) || '').trim();

    if (evt.type === 'TOOL_STARTED' || evt.type === 'TOOL_CALL_REQUESTED') {
      const rawArgs = meta.arguments ?? meta.args ?? meta.input ?? meta.parameters;
      const detail = summarizeStepDetail(toolName, rawArgs, meta, evt.content);

      if (!stepsMap[toolCallId]) {
        orderedIds.push(toolCallId);
      }

      stepsMap[toolCallId] = {
        id: toolCallId,
        type: toolName,
        name: toolName,
        detail,
        status: meta.denied ? 'denied' : 'running',
      };
    } else if (evt.type === 'TOOL_COMPLETED') {
      const step = stepsMap[toolCallId];
      if (step) {
        step.status = (meta.status === 'FAILED' || meta.error) ? 'failed' : 'completed';
        step.durationMs = Number(meta.durationMs) || undefined;
        if (!step.detail || step.detail === '执行工具调用') {
          step.detail = summarizeStepDetail(step.type || toolName, meta.arguments, meta, evt.content);
        }
      }
    } else if (evt.type === 'TOOL_DENIED') {
      const step = stepsMap[toolCallId];
      if (step) {
        step.status = 'denied';
        const reason = firstText(meta, ['deniedReason', 'reason', 'error']);
        step.detail = reason ? `${step.detail}；已阻止：${reason}` : step.detail;
      }
    }
  });

  return orderedIds.map((id) => stepsMap[id]);
};
const stepIconStyle = { marginRight: 0, verticalAlign: 'middle' };

const getStepIcon = (type: string) => {
  switch (type) {
    case 'read_file':
    case 'view_file':
    case 'read_uploaded_file':
      return <BookOpen size={14} style={{ ...stepIconStyle, color: 'var(--amber)' }} />;
    case 'list_dir':
    case 'ls':
    case 'list_uploaded_files':
    case 'glob':
      return <FolderOpen size={14} style={{ ...stepIconStyle, color: 'var(--amber)' }} />;
    case 'grep':
    case 'grep_search':
    case 'web_search':
    case 'image_search':
      return <Search size={14} style={{ ...stepIconStyle, color: 'var(--blue)' }} />;
    case 'web_fetch':
    case 'read_url_content':
    case 'browser_subagent':
      return <Globe size={14} style={{ ...stepIconStyle, color: 'var(--green)' }} />;
    case 'run_script':
    case 'run_command':
    case 'bash':
      return <Terminal size={14} style={{ ...stepIconStyle, color: 'var(--purple)' }} />;
    case 'write_file':
    case 'write_to_file':
    case 'write_todos':
    case 'str_replace':
    case 'replace_file_content':
    case 'multi_replace_file_content':
      return <FileCode size={14} style={{ ...stepIconStyle, color: 'var(--purple)' }} />;
    case 'generate_image':
      return <Image size={14} style={{ ...stepIconStyle, color: 'var(--pink)' }} />;
    case 'schedule':
      return <Clock size={14} style={{ ...stepIconStyle, color: 'var(--blue)' }} />;
    case 'task':
    case 'invoke_subagent':
    case 'send_message':
      return <Users size={14} style={{ ...stepIconStyle, color: 'var(--orange)' }} />;
    default:
      return <Terminal size={14} style={{ ...stepIconStyle, color: 'var(--gray)' }} />;
  }
};

const getStepName = (type: string) => {
  switch (type) {
    case 'read_file':
    case 'view_file':
    case 'read_uploaded_file':
      return '读取文件';
    case 'list_uploaded_files':
      return '查看上传文件';
    case 'ls':
    case 'list_dir':
      return '查看目录列表';
    case 'glob':
      return '检索文件通配符';
    case 'grep':
    case 'grep_search':
      return '搜索文件内容';
    case 'write_todos':
      return '更新 Todo';
    case 'write_file':
    case 'write_to_file':
      return '写入文件';
    case 'str_replace':
    case 'replace_file_content':
    case 'multi_replace_file_content':
      return '修改文件';
    case 'bash':
    case 'run_command':
      return '执行终端命令';
    case 'run_script':
      return '运行脚本';
    case 'web_search':
      return '进行网络检索';
    case 'web_fetch':
    case 'read_url_content':
      return '提取网页内容';
    case 'image_search':
      return '搜索图片';
    case 'generate_image':
      return '生成图片';
    case 'schedule':
      return '设置定时任务';
    case 'task':
    case 'invoke_subagent':
      return '委派子任务';
    case 'send_message':
      return '发送消息';
    case 'browser_subagent':
      return '浏览器自动化';
    case 'ask_clarification':
      return '请求澄清';
    default:
      return type || '执行步骤';
  }
};

export default function AnswerWorkspace({
  phase,
  status,
  messages,
  events,
  observability,
  threadId,
  finalAnswer,
  error,
  onReRun,
  onRefreshMessage,
  onResumeRun,
  pendingClarification,
  onAnswerClarification,
  onSubmitQuestion,
  onOpenCanvas,
  artifacts,
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
  const wasRunning = useRef(false);

  useEffect(() => {
    wasRunning.current = false;
    fetchedThreadId.current = null;
    setRecommendations([]);
    setLoadingRecommendations(false);
  }, [threadId]);

  useEffect(() => {
    if (status === 'running') {
      wasRunning.current = true;
    }
  }, [status]);

  useEffect(() => {
    if ((status === 'completed' || phase === 'done') && threadId) {
      if (!wasRunning.current) {
        return;
      }
      if (fetchedThreadId.current === threadId) {
        return;
      }

      const requestedThreadId = threadId;
      setLoadingRecommendations(true);
      fetchedThreadId.current = requestedThreadId;

      fetch(`/api/deerflow/threads/${encodeURIComponent(requestedThreadId)}/recommend-questions`, {
        method: 'POST',
      })
        .then((res) => res.json())
        .then((data) => {
          if (fetchedThreadId.current !== requestedThreadId) {
            return;
          }
          setRecommendations(Array.isArray(data) ? data : []);
        })
        .catch(() => {
          if (fetchedThreadId.current === requestedThreadId) {
            setRecommendations([]);
          }
        })
        .finally(() => {
          if (fetchedThreadId.current === requestedThreadId) {
            setLoadingRecommendations(false);
          }
        });
    } else {
      if (status === 'running') {
        setRecommendations([]);
        fetchedThreadId.current = null;
      }
    }
  }, [status, phase, threadId]);

  // Only display usage reported by the model provider. Text-length estimates are
  // intentionally excluded because they are misleading for multilingual prompts.
  const tokenMetrics = (() => {
    const totals = observability?.modelUsage?.totals;
    const inputTokens = providerTokenCount(totals?.inputTokens);
    const outputTokens = providerTokenCount(totals?.outputTokens);
    const reportedTotalTokens = providerTokenCount(totals?.totalTokens);
    const totalTokens = reportedTotalTokens
      ?? (inputTokens !== null && outputTokens !== null ? inputTokens + outputTokens : null);
    const cacheReadInputTokens = providerTokenCount(totals?.cacheReadInputTokens);
    const providerReportedSteps = totals?.providerReportedSteps ?? 0;
    const available = providerReportedSteps > 0
      && (inputTokens !== null || outputTokens !== null || totalTokens !== null);

    const formatNumber = (num: number | null) => num === null ? '—' : TOKEN_NUMBER_FORMATTER.format(num);

    return {
      input: formatNumber(inputTokens),
      output: formatNumber(outputTokens),
      total: formatNumber(totalTokens),
      cacheRead: formatNumber(cacheReadInputTokens),
      hasCacheRead: cacheReadInputTokens !== null,
      providerReportedSteps,
      available,
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

  const latestUserMessageIndex = (() => {
    for (let i = visibleMessages.length - 1; i >= 0; i--) {
      if (visibleMessages[i].role === 'USER') {
        return i;
      }
    }
    return -1;
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

  const handleConversationClick = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    const target = e.target as HTMLElement;

    // 1. Intercept copy code button
    const btn = target.closest('.copy-code-btn') as HTMLButtonElement | null;
    if (btn) {
      const wrapper = btn.closest('.code-block-wrapper') as HTMLElement | null;
      if (wrapper) {
        const codeEl = wrapper.querySelector('pre code');
        if (codeEl) {
          const text = codeEl.textContent || '';
          navigator.clipboard.writeText(text).catch(() => {});
          const originalText = btn.textContent || '';
          btn.textContent = '已复制';
          setTimeout(() => {
            btn.textContent = originalText;
          }, 2000);
        }
      }
      return;
    }

    // 2. Intercept artifact links
    const link = target.closest('a') as HTMLAnchorElement | null;
    if (link && onOpenCanvas) {
      const href = link.getAttribute('href') || '';
      const match = href.match(/\/api\/deerflow\/artifacts\/([a-f0-9-]+)/i);
      if (match) {
        e.preventDefault();
        onOpenCanvas(match[1]);
      }
    }
  }, [onOpenCanvas]);

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
            <div className="conversation-list" onClick={handleConversationClick}>
              {visibleMessages.map((message, messageIndex) => (
                <div key={`${message.messageId}-${messageIndex}`}>
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
                        dangerouslySetInnerHTML={{ __html: renderMarkdown(message.content, artifacts) }}
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
                        {status !== 'running'
                          && tokenMetrics.available
                          && message.runId === observability?.runId && (
                          <div className="message-token-dashboard">
                            <Tag size={11} aria-hidden="true" />
                            <span>Tokens 输入: <strong>{tokenMetrics.input}</strong></span>
                            <span className="token-separator">|</span>
                            <span>输出: <strong>{tokenMetrics.output}</strong></span>
                            <span className="token-separator">|</span>
                            <span>总计: <strong>{tokenMetrics.total}</strong></span>
                            {tokenMetrics.hasCacheRead && (
                              <>
                                <span className="token-separator">|</span>
                                <span>缓存读取: <strong>{tokenMetrics.cacheRead}</strong></span>
                              </>
                            )}
                            <span
                              className="token-source-badge"
                              title={`${tokenMetrics.providerReportedSteps} 个模型步骤返回了 usage`}
                            >
                              提供方实报
                            </span>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {messageIndex === latestUserMessageIndex && events && events.length > 0 && (
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
                                <span className="step-item-title">
                                  {getStepIcon(step.type)}
                                  <span>{getStepName(step.type)}</span>
                                </span>
                                {step.detail && (
                                  <span className="step-item-detail">{step.detail}</span>
                                )}
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
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
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
                      whiteSpace: 'normal',
                      textAlign: 'left'
                    }}
                  >
                    <Sparkles size={10} style={{ opacity: 0.6, color: 'var(--blue)', flexShrink: 0 }} />
                    <span style={{ wordBreak: 'break-word' }}>{q}</span>
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
