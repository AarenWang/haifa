import { useEffect, useState } from 'react';
import { Shield, ShieldAlert, CheckCircle2, XCircle, Clock } from 'lucide-react';
import { getUserId } from '../api/deerflowClient';

interface ApprovalCardProps {
  approvalId: string;
  runId: string;
  onResumeRun?: (runId: string) => void;
}

interface ApprovalRecord {
  approvalId: string;
  runId: string;
  threadId: string;
  toolCallId: string;
  toolName: string;
  argsJson: string;
  argsHash: string;
  riskKey: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'BLOCKED';
  reason: string;
  purpose: string;
  preview: string;
  status: 'PENDING' | 'APPROVED' | 'DENIED' | 'EXPIRED' | 'CANCELLED' | 'EXECUTED' | 'INVALIDATED';
  decisionType?: 'APPROVE_ONCE' | 'APPROVE_SESSION' | 'APPROVE_ALWAYS' | 'DENY';
  expiresAt: string;
  resolvedAt?: string;
  resolvedBy?: string;
}

export default function ApprovalCard({ approvalId, runId, onResumeRun }: ApprovalCardProps) {
  const [record, setRecord] = useState<ApprovalRecord | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [comment, setComment] = useState('');
  const [timeLeft, setTimeLeft] = useState<number | null>(null);
  const [allowAlways, setAllowAlways] = useState(false);

  // Fetch approval record on mount
  useEffect(() => {
    let active = true;
    async function fetchRecord() {
      try {
        const res = await fetch(`/api/deerflow/approvals/${approvalId}`, {
          headers: { 'X-User-Id': getUserId() }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        if (active) {
          setRecord(data);
          calculateTimeLeft(data.expiresAt, data.status);
        }
      } catch (err) {
        if (active) setError('Failed to load approval request');
      }
    }
    fetchRecord();
    return () => { active = false; };
  }, [approvalId]);

  // Fetch approval config on mount
  useEffect(() => {
    let active = true;
    async function fetchConfig() {
      try {
        const res = await fetch('/api/deerflow/approvals/config');
        if (res.ok) {
          const data = await res.json();
          if (active) setAllowAlways(!!data.allowAlwaysApproval);
        }
      } catch (err) {
        // ignore
      }
    }
    fetchConfig();
    return () => { active = false; };
  }, []);

  // Countdown timer for pending approval
  useEffect(() => {
    if (timeLeft === null || record?.status !== 'PENDING') return;
    if (timeLeft <= 0) {
      if (onResumeRun) {
        onResumeRun(runId);
      }
      return;
    }

    const timer = setInterval(() => {
      setTimeLeft(prev => {
        if (prev === null) return null;
        if (prev <= 1) {
          clearInterval(timer);
          // Trigger run resume so backend auto-expires and emits APPROVAL_EXPIRED
          if (onResumeRun) {
            onResumeRun(runId);
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [timeLeft, record?.status, approvalId, runId, onResumeRun]);

  const calculateTimeLeft = (expiresAtStr: string, status: string) => {
    if (status !== 'PENDING') {
      setTimeLeft(null);
      return;
    }
    const expiresAt = new Date(expiresAtStr).getTime();
    const now = new Date().getTime();
    const diff = Math.max(0, Math.floor((expiresAt - now) / 1000));
    setTimeLeft(diff);
  };

  const handleDecision = async (decision: 'APPROVE_ONCE' | 'APPROVE_SESSION' | 'APPROVE_ALWAYS' | 'DENY') => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/deerflow/approvals/${approvalId}/decision`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': getUserId()
        },
        body: JSON.stringify({ decision, comment })
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || `HTTP ${res.status}`);
      }

      const updated = await res.json();
      setRecord(updated);
      setTimeLeft(null);

      // Auto-resume run if decision was made
      if (onResumeRun) {
        onResumeRun(runId);
      }
    } catch (err) {
      setError((err as Error).message || 'Failed to submit decision');
    } finally {
      setLoading(false);
    }
  };

  if (!record) {
    return (
      <div className="approval-card approval-card-loading">
        <Clock size={16} className="approval-spin" />
        <span>Loading approval request...</span>
      </div>
    );
  }

  const isPending = record.status === 'PENDING';
  const isHighRisk = record.riskLevel === 'HIGH' || record.riskLevel === 'BLOCKED';
  const isApproved = record.status === 'APPROVED' || record.status === 'EXECUTED';
  const statusClass = isPending ? 'pending' : isApproved ? 'approved' : 'denied';
  const riskClass = isHighRisk ? 'high' : 'medium';
  const decisionLabel = formatDecision(record.decisionType, record.status);
  const resolvedTime = record.resolvedAt
    ? new Date(record.resolvedAt).toLocaleTimeString()
    : null;

  return (
    <div className={`approval-card ${riskClass} ${statusClass}`}>
      <div className="approval-card-header">
        <div className="approval-card-title-group">
          <span className="approval-card-icon">
            {isHighRisk ? <ShieldAlert size={20} /> : <Shield size={20} />}
          </span>
          <div>
            <div className="approval-card-eyebrow">Approval request</div>
            <div className="approval-card-title">
              {isHighRisk ? 'High risk action requires approval' : 'Action requires approval'}
            </div>
          </div>
        </div>
        {isPending && timeLeft !== null && (
          <div className="approval-countdown" title="Approval timeout">
            <Clock size={14} />
            <span>Expires in {timeLeft}s</span>
          </div>
        )}
        {!isPending && (
          <div className={`approval-status-pill ${statusClass}`}>
            {isApproved ? <CheckCircle2 size={14} /> : <XCircle size={14} />}
            <span>{isApproved ? 'Approved' : record.status}</span>
          </div>
        )}
      </div>

      <div className="approval-meta-grid">
        <div className="approval-meta-item">
          <span>Tool</span>
          <code>{record.toolName}</code>
        </div>
        <div className="approval-meta-item">
          <span>Risk</span>
          <strong className={`approval-risk ${riskClass}`}>{record.riskLevel}</strong>
        </div>
        <div className="approval-meta-item wide">
          <span>Reason</span>
          <p>{record.reason || 'Policy requires human confirmation before execution.'}</p>
        </div>
      </div>

      {record.preview && (
        <div className="approval-preview">
          <div className="approval-section-label">Preview</div>
          <pre>{record.preview}</pre>
        </div>
      )}

      {isPending ? (
        <div className="approval-actions-panel">
          <div className="approval-comment-field">
            <label>Purpose / comment</label>
            <input
              type="text"
              placeholder="Optional context for this decision"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              disabled={loading}
            />
          </div>

          {error && <div className="approval-error">{error}</div>}

          <div className="approval-actions">
            <button
              type="button"
              onClick={() => handleDecision('APPROVE_ONCE')}
              className="approval-btn primary"
              disabled={loading}
            >
              <CheckCircle2 size={14} />
              Approve Once
            </button>
            <button
              type="button"
              onClick={() => handleDecision('APPROVE_SESSION')}
              className="approval-btn secondary"
              disabled={loading}
            >
              <Shield size={14} />
              Approve Session
            </button>
            {allowAlways && (
              <button
                type="button"
                onClick={() => handleDecision('APPROVE_ALWAYS')}
                className="approval-btn secondary"
                disabled={loading}
              >
                <Shield size={14} />
                Approve Always
              </button>
            )}
            <button
              type="button"
              onClick={() => handleDecision('DENY')}
              className="approval-btn danger"
              disabled={loading}
            >
              <XCircle size={14} />
              Deny
            </button>
          </div>
        </div>
      ) : (
        <div className="approval-resolution">
          {isApproved ? (
            <div className="approval-resolution-main approved">
              <CheckCircle2 size={16} />
              <span>{decisionLabel}</span>
            </div>
          ) : (
            <div className="approval-resolution-main denied">
              <XCircle size={16} />
              <span>{record.status}</span>
            </div>
          )}
          <div className="approval-resolution-meta">
            {record.resolvedBy && <span>By {record.resolvedBy}</span>}
            {resolvedTime && <span>Decided at {resolvedTime}</span>}
          </div>
        </div>
      )}
    </div>
  );
}

function formatDecision(decisionType: ApprovalRecord['decisionType'], status: ApprovalRecord['status']) {
  switch (decisionType) {
    case 'APPROVE_ONCE':
      return 'Approve Once';
    case 'APPROVE_SESSION':
      return 'Approve Session';
    case 'APPROVE_ALWAYS':
      return 'Approve Always';
    case 'DENY':
      return 'Deny';
    default:
      return status;
  }
}
