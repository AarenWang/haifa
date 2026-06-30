import {
  Server,
  Trash2,
  Clock,
  CheckCircle,
  AlertTriangle,
  Square,
  XCircle,
  Loader2,
} from 'lucide-react';
import type { UploadRecord, RunHistoryEntry, AppStatus } from '../types';
import UploadPanel from './UploadPanel';

interface WorkspaceSidebarProps {
  backendStatus: 'connected' | 'disconnected' | 'unknown';
  uploads: UploadRecord[];
  selectedUploadIds: string[];
  threadId?: string;
  runHistory: RunHistoryEntry[];
  onUploadsChange: (uploads: UploadRecord[]) => void;
  onToggleUploadSelection: (fileId: string) => void;
  onRemoveUpload: (fileId: string) => void;
  onClearUploads: () => void;
}

const statusIcon: Record<AppStatus, React.ReactNode> = {
  idle: <Square size={12} />,
  running: <Loader2 size={12} className="spin-icon" />,
  completed: <CheckCircle size={12} />,
  failed: <XCircle size={12} />,
  stopped: <AlertTriangle size={12} />,
};

const statusColor: Record<AppStatus, string> = {
  idle: 'var(--text-muted)',
  running: 'var(--accent-blue)',
  completed: 'var(--accent-green)',
  failed: 'var(--accent-red)',
  stopped: 'var(--accent-amber)',
};

export default function WorkspaceSidebar({
  backendStatus,
  uploads,
  selectedUploadIds,
  threadId,
  runHistory,
  onUploadsChange,
  onToggleUploadSelection,
  onRemoveUpload,
  onClearUploads,
}: WorkspaceSidebarProps) {
  const recentRuns = runHistory.slice(0, 5);

  return (
    <aside className="workspace-sidebar">
      <div className="sidebar-section">
        <div className="sidebar-backend-status">
          <Server size={14} />
          <span>Backend</span>
          <span
            className={`backend-dot ${backendStatus}`}
            title={backendStatus}
          />
          <span className="backend-label">{backendStatus}</span>
        </div>
      </div>

      <div className="sidebar-section">
        <UploadPanel
          uploads={uploads}
          selectedUploadIds={selectedUploadIds}
          threadId={threadId}
          onUploadsChange={onUploadsChange}
          onToggleSelection={onToggleUploadSelection}
          onRemoveUpload={onRemoveUpload}
        />
        {uploads.length > 0 && (
          <button
            type="button"
            className="btn btn-ghost sidebar-clear-btn"
            onClick={onClearUploads}
          >
            <Trash2 size={14} />
            Clear all uploads
          </button>
        )}
      </div>

      <div className="sidebar-section">
        <div className="sidebar-section-header">
          <Clock size={14} />
          <span>Recent Runs</span>
          <span className="sidebar-count">{runHistory.length}</span>
        </div>
        {recentRuns.length === 0 ? (
          <div className="sidebar-empty">No runs yet</div>
        ) : (
          <div className="run-history-list">
            {recentRuns.map((run) => (
              <div key={run.runId + run.startedAt} className="run-history-item">
                <span
                  className="run-history-icon"
                  style={{ color: statusColor[run.status] }}
                >
                  {statusIcon[run.status]}
                </span>
                <div className="run-history-info">
                  <div className="run-history-message" title={run.message}>
                    {run.message}
                  </div>
                  <div className="run-history-meta">
                    {run.model || 'default'} · {formatDate(run.startedAt)}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </aside>
  );
}

function formatDate(iso: string) {
  const d = new Date(iso);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;

  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
