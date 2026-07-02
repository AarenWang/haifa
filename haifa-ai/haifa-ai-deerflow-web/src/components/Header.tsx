import { Zap, Brain } from 'lucide-react';
import type { AppStatus } from '../types';

interface HeaderProps {
  backendStatus: 'connected' | 'disconnected' | 'unknown';
  runStatus: AppStatus;
  onOpenMemorySettings: () => void;
}

export default function Header({ backendStatus, runStatus, onOpenMemorySettings }: HeaderProps) {
  const backendPill = (() => {
    if (backendStatus === 'connected') {
      return (
        <span className="status-pill connected">
          <span className="dot" />
          Backend Connected
        </span>
      );
    }
    if (backendStatus === 'disconnected') {
      return (
        <span className="status-pill disconnected">
          <span className="dot" />
          Backend Disconnected
        </span>
      );
    }
    return (
      <span className="status-pill">
        <span className="dot" />
        Backend Unknown
      </span>
    );
  })();

  const runPill = (() => {
    const map: Record<AppStatus, string> = {
      idle: 'Idle',
      running: 'Running',
      completed: 'Completed',
      failed: 'Failed',
      stopped: 'Stopped',
      suspended: 'Waiting for approval',
    };
    return (
      <span className={`status-pill ${runStatus}`}>
        <span className="dot" />
        {map[runStatus]}
      </span>
    );
  })();

  return (
    <header className="header">
      <div className="header-brand">
        <Zap size={22} className="logo-icon" />
        DeerFlow Studio
      </div>
      <div className="header-meta">
        {backendPill}
        {runPill}
        <button
          type="button"
          className="btn btn-ghost flex-row align-center gap-1"
          onClick={onOpenMemorySettings}
          title="Agent Persona & Memory"
          style={{
            padding: '4px 8px',
            fontSize: '12px',
            borderRadius: '4px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            border: '1px solid var(--border)',
            background: 'var(--bg-secondary)',
            cursor: 'pointer'
          }}
        >
          <Brain size={14} style={{ color: 'var(--accent-blue)' }} />
          <span>Memory & Persona</span>
        </button>
      </div>
    </header>
  );
}
