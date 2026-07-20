import type { DeerFlowEvent, SubagentOutputChannel, SubagentRunSummary, SubagentStatus } from '../../types';

const CHILD_EVENT_TYPES = new Set([
  'SUBAGENT_QUEUED', 'SUBAGENT_STARTED', 'SUBAGENT_STEP_STARTED', 'SUBAGENT_STEP_COMPLETED',
  'SUBAGENT_OUTPUT', 'SUBAGENT_TOOL_STARTED', 'SUBAGENT_TOOL_COMPLETED', 'SUBAGENT_FAILED',
  'SUBAGENT_CANCELLED', 'SUBAGENT_TIMED_OUT', 'SUBAGENT_COMPLETED',
]);

const asText = (value: unknown): string => typeof value === 'string' ? value.trim() : '';
const asNumber = (value: unknown): number | undefined => {
  const number = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(number) ? number : undefined;
};

const parseArguments = (value: unknown): Record<string, unknown> => {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value as Record<string, unknown>;
  if (typeof value !== 'string') return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {};
  } catch {
    return {};
  }
};

const statusFromEvent = (event: DeerFlowEvent): SubagentStatus | undefined => {
  const status = asText(event.metadata?.subagentStatus || event.metadata?.status).toUpperCase();
  if (['QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'TIMED_OUT'].includes(status)) {
    return status as SubagentStatus;
  }
  switch (event.type) {
    case 'SUBAGENT_QUEUED': return 'QUEUED';
    case 'SUBAGENT_STARTED': return 'RUNNING';
    case 'SUBAGENT_COMPLETED': return 'COMPLETED';
    case 'SUBAGENT_FAILED': return 'FAILED';
    case 'SUBAGENT_CANCELLED': return 'CANCELLED';
    case 'SUBAGENT_TIMED_OUT': return 'TIMED_OUT';
    default: return undefined;
  }
};

const channelFromEvent = (event: DeerFlowEvent): SubagentOutputChannel => {
  const channel = asText(event.metadata?.outputChannel);
  return channel === 'stdout' || channel === 'stderr' || channel === 'model_delta' ? channel : 'system';
};

const descriptionFrom = (event: DeerFlowEvent): string => {
  const args = parseArguments(event.metadata?.arguments);
  return asText(args.description) || asText(args.prompt) || '子智能体任务';
};

export function deriveSubagentRuns(events: DeerFlowEvent[]): SubagentRunSummary[] {
  const runs = new Map<string, SubagentRunSummary>();
  const keyByTaskId = new Map<string, string>();

  for (const event of events) {
    const meta = event.metadata || {};
    const isTaskTool = asText(meta.toolName) === 'task';
    const isChildEvent = CHILD_EVENT_TYPES.has(event.type);
    const isSubagentToolResult = meta.subagent === true;
    if (!isTaskTool && !isChildEvent && !isSubagentToolResult) continue;

    const toolCallId = asText(meta.toolCallId) || event.eventId;
    const taskId = asText(meta.taskId);
    const existingKey = taskId ? keyByTaskId.get(taskId) : undefined;
    const key = existingKey || toolCallId;
    let run = runs.get(key);
    if (!run) {
      const description = descriptionFrom(event);
      run = {
        taskId: taskId || toolCallId,
        subagentRunId: asText(meta.subagentRunId) || undefined,
        parentRunId: asText(meta.parentRunId) || event.runId,
        parentToolCallId: toolCallId,
        displayName: asText(meta.displayName) || `子智能体 ${runs.size + 1}`,
        description,
        status: 'QUEUED',
        durationMs: undefined,
        retryable: false,
        events: [],
        output: [],
      };
      runs.set(key, run);
    }
    if (taskId) {
      run.taskId = taskId;
      keyByTaskId.set(taskId, key);
    }
    run.subagentRunId ||= asText(meta.subagentRunId) || undefined;
    run.parentRunId ||= asText(meta.parentRunId) || event.runId;
    run.events.push(event);
    run.durationMs = asNumber(meta.durationMs) ?? run.durationMs;
    run.retryable = meta.retryable === true || run.retryable;
    run.error = asText(meta.error) || run.error;
    run.errorCode = asText(meta.errorCode) || run.errorCode;
    run.latestAction = event.content || run.latestAction;

    const status = statusFromEvent(event);
    if (status) run.status = status;
    const progress = meta.progress;
    if (progress && typeof progress === 'object') {
      const record = progress as Record<string, unknown>;
      const current = asNumber(record.current);
      const total = asNumber(record.total);
      if (current !== undefined) run.progress = { current, total };
    }
    if (event.type === 'SUBAGENT_OUTPUT' || (isSubagentToolResult && event.type === 'TOOL_COMPLETED')) {
      const sequence = asNumber(meta.subagentSequence) ?? run.output.length + 1;
      const text = event.content?.trim();
      if (text && !run.output.some((chunk) => chunk.sequence === sequence && chunk.text === text)) {
        run.output.push({ sequence, channel: channelFromEvent(event), text, createdAt: event.createdAt });
      }
    }
  }
  return [...runs.values()];
}

export function subagentStatusLabel(status: SubagentStatus): string {
  return ({ QUEUED: '排队中', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消', TIMED_OUT: '已超时' })[status];
}
