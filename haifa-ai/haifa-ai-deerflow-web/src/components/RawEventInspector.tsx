import { useEffect, useRef } from 'react';
import type { DeerFlowEvent } from '../types';

interface RawEventInspectorProps {
  event: DeerFlowEvent;
  onClose: () => void;
}

export default function RawEventInspector({ event, onClose }: RawEventInspectorProps) {
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [onClose]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(JSON.stringify(event, null, 2));
    } catch {
      // ignore
    }
  };

  return (
    <div className="inspector-overlay" onClick={onClose}>
      <div
        className="inspector-panel"
        ref={panelRef}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="inspector-header">
          <div className="inspector-title">Event Inspector</div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="button" className="btn btn-secondary" onClick={handleCopy}>
              Copy JSON
            </button>
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
        <div className="inspector-body">
          <pre className="json-block">{JSON.stringify(event, null, 2)}</pre>
        </div>
      </div>
    </div>
  );
}
