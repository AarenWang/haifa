import { useEffect, useRef, useState } from 'react';
import { ChevronDown, Copy, X } from 'lucide-react';
import type { SubagentRunSummary } from '../../types';
import { subagentStatusLabel } from './subagentState';

interface Props { run: SubagentRunSummary; onClose: () => void; }

export default function SubagentDetailDrawer({ run, onClose }: Props) {
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);
  const outputRef = useRef<HTMLDivElement>(null);

  useEffect(() => { closeButtonRef.current?.focus(); }, []);
  useEffect(() => {
    if (autoScroll && outputRef.current) outputRef.current.scrollTop = outputRef.current.scrollHeight;
  }, [autoScroll, run.output.length]);

  const copyLog = async () => {
    const text = run.output.map((chunk) => chunk.text).join('\n') || run.events.map((event) => event.content).join('\n');
    await navigator.clipboard.writeText(text);
  };

  return (
    <div className="subagent-drawer-backdrop" role="presentation" onMouseDown={onClose}>
      <aside className="subagent-drawer" aria-label={`${run.displayName} 子智能体详情`} onMouseDown={(event) => event.stopPropagation()}>
        <header className="subagent-drawer-header">
          <div><strong>{run.displayName}</strong><span className={`subagent-status subagent-status-${run.status.toLowerCase()}`}>{subagentStatusLabel(run.status)}</span></div>
          <button ref={closeButtonRef} type="button" className="subagent-icon-button" onClick={onClose} aria-label="关闭子智能体详情"><X size={16} /></button>
        </header>
        <p className="subagent-description">{run.description}</p>
        {run.error && <div className="subagent-error"><strong>{run.errorCode || '执行失败'}</strong><span>{run.error}</span></div>}
        <div className="subagent-detail-columns">
          <section><h4>执行步骤</h4><ol className="subagent-timeline">{run.events.map((event) => <li key={`${event.eventId}-${event.type}`}><span>{event.type.replace('SUBAGENT_', '').replaceAll('_', ' ')}</span><small>{event.content}</small></li>)}</ol></section>
          <section><div className="subagent-output-title"><h4>实时输出</h4><button type="button" onClick={copyLog}><Copy size={12} />复制日志</button></div><div className="subagent-output" ref={outputRef} onScroll={(event) => { const node = event.currentTarget; setAutoScroll(node.scrollHeight - node.scrollTop - node.clientHeight < 24); }} aria-live="polite">{run.output.length ? run.output.map((chunk) => <p key={`${chunk.sequence}-${chunk.text}`} className={`subagent-output-${chunk.channel}`}>{chunk.text}</p>) : <span className="subagent-empty-output">暂无子智能体输出</span>}</div>{!autoScroll && <button type="button" className="subagent-back-to-bottom" onClick={() => { setAutoScroll(true); outputRef.current?.scrollTo({ top: outputRef.current.scrollHeight }); }}><ChevronDown size={13} />回到底部</button>}</section>
        </div>
      </aside>
    </div>
  );
}
