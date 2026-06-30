import { Zap } from 'lucide-react';
import type { AppStatus } from '../types';

interface HeaderProps {
  backendStatus: 'connected' | 'disconnected' | 'unknown';
  runStatus: AppStatus;
}

export default function Header({ backendStatus, runStatus }: HeaderProps) {
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
      </div>
    </header>
  );
}
