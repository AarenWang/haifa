import {
  Server,
  Trash2,
  MessageSquare,
  Plus,
  X,
} from 'lucide-react';
import type { UploadRecord, ThreadRecord } from '../types';
import UploadPanel from './UploadPanel';

interface WorkspaceSidebarProps {
  backendStatus: 'connected' | 'disconnected' | 'unknown';
  uploads: UploadRecord[];
  selectedUploadIds: string[];
  threadId?: string;
  threads: ThreadRecord[];
  onSelectThread: (threadId: string) => void;
  onNewThread: () => void;
  onUploadsChange: (uploads: UploadRecord[]) => void;
  onToggleUploadSelection: (fileId: string) => void;
  onRemoveUpload: (fileId: string) => void;
  onClearUploads: () => void;
  isOpen?: boolean;
  onClose?: () => void;
}

export default function WorkspaceSidebar({
  backendStatus,
  uploads,
  selectedUploadIds,
  threadId,
  threads,
  onSelectThread,
  onNewThread,
  onUploadsChange,
  onToggleUploadSelection,
  onRemoveUpload,
  onClearUploads,
  isOpen = false,
  onClose,
}: WorkspaceSidebarProps) {
  return (
    <aside className={`workspace-sidebar ${isOpen ? 'open' : ''}`}>
      <div className="sidebar-section">
        <div className="sidebar-backend-status">
          <Server size={14} />
          <span>Backend</span>
          <span
            className={`backend-dot ${backendStatus}`}
            title={backendStatus}
          />
          <span className="backend-label">{backendStatus}</span>
          {onClose && (
            <button
              type="button"
              className="sidebar-close-btn"
              onClick={onClose}
              title="Close sidebar"
            >
              <X size={14} />
            </button>
          )}
        </div>
      </div>

      <div className="sidebar-section sidebar-thread-section">
        <div className="sidebar-section-header">
          <MessageSquare size={14} />
          <span>Threads</span>
          <span className="sidebar-count">{threads.length}</span>
          <button
            type="button"
            className="sidebar-icon-btn"
            onClick={onNewThread}
            title="New thread"
          >
            <Plus size={14} />
          </button>
        </div>
        {threads.length === 0 ? (
          <div className="sidebar-empty">Send a message to create a thread</div>
        ) : (
          <div className="thread-list">
            {threads.map((thread) => {
              const selected = thread.threadId === threadId;
              return (
                <button
                  type="button"
                  key={thread.threadId}
                  className={`thread-item ${selected ? 'selected' : ''}`}
                  onClick={() => onSelectThread(thread.threadId)}
                >
                  <span className="thread-title">{thread.title || 'New thread'}</span>
                  <span className="thread-meta">{formatDate(thread.updatedAt)}</span>
                </button>
              );
            })}
          </div>
        )}
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
