import { Activity, Database, GitBranch, Inbox, Link2, X } from 'lucide-react';
import { useRef, useEffect, useState } from 'react';
import type { DeerFlowEvent, RunObservability, TodoSnapshot } from '../types';
import EventCard from './EventCard';
import TodoListPanel from './TodoListPanel';

interface ActivityTraceProps {
  events: DeerFlowEvent[];
  isOpen?: boolean;
  onClose?: () => void;
  todoSnapshot?: TodoSnapshot;
  todoGateBlocked?: boolean;
  todoGateMessage?: string;
  observability?: RunObservability;
}

export default function ActivityTrace({
  events,
  isOpen = false,
  onClose,
  todoSnapshot,
  todoGateBlocked,
  todoGateMessage,
  observability,
}: ActivityTraceProps) {
  const listRef = useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);
  const prevLength = useRef(events.length);

  useEffect(() => {
    if (events.length > prevLength.current && autoScroll && listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
    prevLength.current = events.length;
  }, [events.length, autoScroll]);

  const handleScroll = () => {
    if (!listRef.current) return;
    const el = listRef.current;
    const nearBottom =
      el.scrollHeight - el.scrollTop - el.clientHeight < 40;
    setAutoScroll(nearBottom);
  };

  return (
    <aside className={`trace-panel ${isOpen ? 'open' : ''}`}>
      <TodoListPanel
        snapshot={todoSnapshot}
        gateBlocked={todoGateBlocked}
        gateMessage={todoGateMessage}
      />
      <GraphReplayPanel observability={observability} />
      <div className="trace-header">
        <div className="trace-header-title">
          <Activity size={14} style={{ marginRight: 6, verticalAlign: 'middle' }} />
          Activity Trace
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className="trace-count">{events.length}</span>
          {onClose && (
            <button
              type="button"
              className="trace-close-btn"
              onClick={onClose}
              title="Close trace"
            >
              <X size={14} />
            </button>
          )}
        </div>
      </div>
      <div
        className="trace-list"
        ref={listRef}
        onScroll={handleScroll}
      >
        {events.length === 0 ? (
          <div className="answer-empty" style={{ padding: 20 }}>
            <Inbox size={28} className="empty-icon" />
            <span style={{ fontSize: 13 }}>
              Events will appear here once a run starts.
            </span>
          </div>
        ) : (
          events.map((evt, index) => (
            <EventCard key={`${evt.runId}:${evt.eventId}:${index}`} event={evt} />
          ))
        )}
      </div>
    </aside>
  );
}

function GraphReplayPanel({ observability }: { observability?: RunObservability }) {
  if (!observability) {
    return null;
  }
  const latestCheckpoints = observability.checkpointTimeline.slice(-4).reverse();
  const coverage = Math.round((observability.citationCoverage || 0) * 100);
  return (
    <div className="graph-replay-panel">
      <div className="graph-replay-header">
        <div className="graph-replay-title">
          <GitBranch size={14} />
          <span>{observability.graphName || 'Graph run'}</span>
        </div>
        <span className="graph-replay-status">{observability.status}</span>
      </div>
      <div className="graph-replay-metrics">
        <div>
          <Database size={13} />
          <span>{observability.checkpointCount}</span>
          <small>checkpoints</small>
        </div>
        <div>
          <Link2 size={13} />
          <span>{observability.sourceCount}</span>
          <small>sources</small>
        </div>
        <div>
          <Activity size={13} />
          <span>{observability.evidenceCount}</span>
          <small>evidence</small>
        </div>
      </div>
      {observability.mode === 'research' && (
        <div className="graph-replay-coverage">
          <span>Citation coverage</span>
          <strong>{coverage}%</strong>
        </div>
      )}
      {latestCheckpoints.length > 0 && (
        <div className="graph-checkpoint-strip">
          {latestCheckpoints.map((checkpoint) => (
            <div key={checkpoint.checkpointId} className="graph-checkpoint-chip" title={checkpoint.checkpointId}>
              <span>{checkpoint.nodeId || 'unknown'}</span>
              {checkpoint.nextNodeId && <small>{'->'} {checkpoint.nextNodeId}</small>}
            </div>
          ))}
        </div>
      )}
      {observability.orphanEvidenceCount > 0 && (
        <div className="graph-replay-warning">
          {observability.orphanEvidenceCount} evidence item{observability.orphanEvidenceCount === 1 ? '' : 's'} without source trace
        </div>
      )}
    </div>
  );
}
