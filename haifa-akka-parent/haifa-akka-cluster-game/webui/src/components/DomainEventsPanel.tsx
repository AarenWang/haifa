import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';

interface DomainEventRecord {
  id: number;
  timestamp: Date;
  raw: string;
  parsed: unknown;
  type?: string;
}

interface DomainEventsPanelProps {
  baseUrl: string;
}

const DomainEventsPanel = ({ baseUrl }: DomainEventsPanelProps): JSX.Element => {
  const [listening, setListening] = useState(false);
  const [events, setEvents] = useState<DomainEventRecord[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState('');
  const counterRef = useRef(0);
  const isEventSourceSupported = typeof window !== 'undefined' && typeof window.EventSource !== 'undefined';

  const normalizedBaseUrl = useMemo(() => baseUrl.replace(/\/$/, ''), [baseUrl]);

  useEffect(() => {
    if (!listening || !isEventSourceSupported) {
      return;
    }

    setError(null);
    const params = new URLSearchParams();
    typeFilter
      .split(',')
      .map((value) => value.trim())
      .filter((value) => value.length > 0)
      .forEach((value) => params.append('type', value));

    const url = `${normalizedBaseUrl}/cluster/domain-events${params.toString() ? `?${params.toString()}` : ''}`;
    const eventSource = new EventSource(url);

    const handleMessage = (event: MessageEvent<string>) => {
      counterRef.current += 1;
      let parsed: unknown = event.data;
      let derivedType: string | undefined;
      try {
        parsed = JSON.parse(event.data);
        if (parsed && typeof parsed === 'object' && 'type' in (parsed as Record<string, unknown>)) {
          const candidate = (parsed as Record<string, unknown>).type;
          if (typeof candidate === 'string') {
            derivedType = candidate;
          }
        }
      } catch (err) {
        // Raw string kept in parsed field when JSON parsing fails.
        console.warn('Failed to parse domain event payload', err);
      }

      const record: DomainEventRecord = {
        id: counterRef.current,
        timestamp: new Date(),
        raw: event.data,
        parsed,
        type: derivedType ?? (event as unknown as { type?: string }).type
      };

      setEvents((prev) => {
        const next = [...prev, record];
        if (next.length > 200) {
          next.splice(0, next.length - 200);
        }
        return next;
      });
    };

    const handleError = (evt: Event) => {
      console.error('Domain events stream error', evt);
      setError('事件流中断，请检查管理接口或网络连接。');
      setListening(false);
      eventSource.close();
    };

    eventSource.onmessage = handleMessage;
    eventSource.onerror = handleError;

    return () => {
      eventSource.close();
    };
  }, [isEventSourceSupported, listening, normalizedBaseUrl, typeFilter]);

  useEffect(() => {
    // Reset captured events when base URL changes
    setEvents([]);
    counterRef.current = 0;
  }, [normalizedBaseUrl]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!isEventSourceSupported) {
      setError('当前浏览器不支持 Server-Sent Events (EventSource)。');
      return;
    }
    setListening((prev) => !prev);
  };

  const toggleListening = () => {
    if (!isEventSourceSupported) {
      setError('当前浏览器不支持 Server-Sent Events (EventSource)。');
      return;
    }
    setListening((prev) => !prev);
  };

  const formattedEvents = useMemo(() => events.slice().reverse(), [events]);

  return (
    <div className="panel">
      <div className="section-header">
        <h2>Cluster Domain Events (SSE)</h2>
        <div className="actions">
          <button type="button" onClick={toggleListening}>
            {listening ? '停止监听' : '开始监听'}
          </button>
          <button type="button" className="secondary" onClick={() => setEvents([])}>
            清空记录
          </button>
        </div>
      </div>
      <p>
        通过 Server-Sent Events 实时获取集群领域事件。可选输入事件类型过滤，多个类型以逗号分隔，例如
        <code>MemberUp,LeaderChanged</code>。
      </p>
      <form className="inline-form" onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="事件类型过滤 (可选)"
          value={typeFilter}
          onChange={(event) => setTypeFilter(event.target.value)}
          disabled={listening || !isEventSourceSupported}
        />
        <button type="submit" disabled={!isEventSourceSupported}>
          {listening ? '停止' : '开始'}监听
        </button>
      </form>
      {error && <div className="error-message">{error}</div>}
      {!isEventSourceSupported && <div className="error-message">浏览器未提供 EventSource API，无法监听事件。</div>}

      <div className="event-stream" aria-live="polite">
        {formattedEvents.length === 0 && <div>暂无事件</div>}
        {formattedEvents.map((event) => (
          <div className="event" key={event.id}>
            <div>
              <span className="event-type">[{event.type ?? 'Event'}]</span> {event.timestamp.toLocaleString()}
            </div>
            <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{JSON.stringify(event.parsed, null, 2)}</pre>
            <details>
              <summary>查看原始数据</summary>
              <code>{event.raw}</code>
            </details>
          </div>
        ))}
      </div>
    </div>
  );
};

export default DomainEventsPanel;
