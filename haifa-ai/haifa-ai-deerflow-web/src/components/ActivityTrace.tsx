import { Activity, Inbox, X } from 'lucide-react';
import { useRef, useEffect, useState } from 'react';
import type { DeerFlowEvent } from '../types';
import EventCard from './EventCard';

interface ActivityTraceProps {
  events: DeerFlowEvent[];
  isOpen?: boolean;
  onClose?: () => void;
}

export default function ActivityTrace({ events, isOpen = false, onClose }: ActivityTraceProps) {
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
          events.map((evt) => (
            <EventCard key={evt.eventId} event={evt} />
          ))
        )}
      </div>
    </aside>
  );
}
