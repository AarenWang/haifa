import { useState } from 'react';
import { AlertTriangle, CheckCircle2, Clock3, Loader2, Users } from 'lucide-react';
import type { DeerFlowEvent, SubagentRunSummary } from '../../types';
import SubagentDetailDrawer from './SubagentDetailDrawer';
import { deriveSubagentRuns, subagentStatusLabel } from './subagentState';

const statusIcon = (run: SubagentRunSummary) => {
  if (run.status === 'COMPLETED') return <CheckCircle2 size={15} />;
  if (run.status === 'FAILED' || run.status === 'TIMED_OUT') return <AlertTriangle size={15} />;
  if (run.status === 'RUNNING') return <Loader2 size={15} className="animate-spin" />;
  return <Clock3 size={15} />;
};

export default function SubagentSummaryPanel({ events }: { events: DeerFlowEvent[] }) {
  const runs = deriveSubagentRuns(events);
  const [selected, setSelected] = useState<SubagentRunSummary | null>(null);
  if (!runs.length) return null;
  const running = runs.filter((run) => run.status === 'RUNNING').length;
  const failed = runs.filter((run) => run.status === 'FAILED' || run.status === 'TIMED_OUT').length;
  return <section className="subagent-summary-panel" aria-label="子智能体执行状态">
    <header><span><Users size={15} />子智能体 <strong>{runs.length}</strong> 个</span><small>{running} 运行中 · {failed} 失败</small></header>
    <div className="subagent-card-grid">{runs.map((run, index) => <button key={run.parentToolCallId} type="button" className={`subagent-run-card subagent-run-${run.status.toLowerCase()}`} onClick={() => setSelected(run)}><span className="subagent-card-status">{statusIcon(run)}{subagentStatusLabel(run.status)}</span><strong>{run.displayName || `子智能体 ${index + 1}`}</strong><span className="subagent-card-detail">{run.progress ? `第 ${run.progress.current}${run.progress.total ? ` / ${run.progress.total}` : ''} 步` : run.error || run.latestAction || '等待执行'}</span>{run.durationMs !== undefined && <small>{run.durationMs}ms</small>}</button>)}</div>
    {selected && <SubagentDetailDrawer run={selected} onClose={() => setSelected(null)} />}
  </section>;
}
