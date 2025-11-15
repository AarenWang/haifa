import { FormEvent, Fragment, useCallback, useMemo, useState } from 'react';
import type { ClusterMember, ClusterMembersResponse } from '../api/clusterApi';

interface SummaryCard {
  label: string;
  value?: string | string[];
}

interface MembersPanelProps {
  loading: boolean;
  error: string | null;
  actionFeedback: { type: 'success' | 'error'; message: string } | null;
  clusterData: ClusterMembersResponse | null;
  summaryCards: SummaryCard[];
  onJoin: (address: string) => Promise<void>;
  onDeleteLeave: (address: string) => Promise<void>;
  onGracefulLeave: (address: string) => Promise<void>;
  onDown: (address: string) => Promise<void>;
  onInspect: (address: string) => Promise<ClusterMember>;
}

interface InspectState {
  loading: boolean;
  detail?: ClusterMember;
  error?: string;
}

const MembersPanel = ({
  loading,
  error,
  actionFeedback,
  clusterData,
  summaryCards,
  onJoin,
  onDeleteLeave,
  onGracefulLeave,
  onDown,
  onInspect
}: MembersPanelProps): JSX.Element => {
  const [joinAddress, setJoinAddress] = useState('');
  const [joinSubmitting, setJoinSubmitting] = useState(false);
  const [expandedAddress, setExpandedAddress] = useState<string | null>(null);
  const [inspectState, setInspectState] = useState<Record<string, InspectState>>({});
  const [pendingActionKey, setPendingActionKey] = useState<string | null>(null);

  const members = useMemo(() => clusterData?.members ?? [], [clusterData]);

  const handleJoinSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = joinAddress.trim();
    if (!trimmed) {
      return;
    }

    setJoinSubmitting(true);
    try {
      await onJoin(trimmed);
      setJoinAddress('');
    } catch (err) {
      console.warn('Join action failed', err);
    } finally {
      setJoinSubmitting(false);
    }
  };

  const resolveAddress = useCallback((member: ClusterMember): string => {
    return member.address ?? member.node ?? member.uniqueAddress?.address ?? '';
  }, []);

  const handleInspect = useCallback(
    async (address: string) => {
      if (!address) {
        return;
      }
      setInspectState((prev) => ({
        ...prev,
        [address]: { ...prev[address], loading: true, error: undefined }
      }));
      try {
        const detail = await onInspect(address);
        setInspectState((prev) => ({
          ...prev,
          [address]: { loading: false, detail }
        }));
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        setInspectState((prev) => ({
          ...prev,
          [address]: { loading: false, error: message }
        }));
      }
    },
    [onInspect]
  );

  const toggleInspect = (address: string) => {
    if (expandedAddress === address) {
      setExpandedAddress(null);
      return;
    }

    setExpandedAddress(address);
    void handleInspect(address);
  };

  const runMemberAction = useCallback(async (address: string, action: (addr: string) => Promise<void>, key: string) => {
    if (!address) {
      return;
    }
    const actionKey = `${address}:${key}`;
    setPendingActionKey(actionKey);
    try {
      await action(address);
    } catch (err) {
      console.warn('Member action failed', err);
    } finally {
      setPendingActionKey(null);
    }
  }, []);

  return (
    <div className="panel">
      <div className="section-header">
        <h2>集群成员</h2>
        {loading && <span>刷新成员信息中…</span>}
      </div>

      {summaryCards.length > 0 && (
        <div className="card-grid">
          {summaryCards.map((card) => (
            <div className="card" key={card.label}>
              <strong>{card.label}</strong>
              {Array.isArray(card.value) ? (
                card.value.length === 0 ? (
                  <div>—</div>
                ) : (
                  card.value.map((item) => (
                    <span className="badge" key={item}>
                      {item}
                    </span>
                  ))
                )
              ) : card.value ? (
                <div>{card.value}</div>
              ) : (
                <div>—</div>
              )}
            </div>
          ))}
        </div>
      )}

      <form className="inline-form" onSubmit={handleJoinSubmit}>
        <input
          type="text"
          placeholder="akka://system@host:port"
          value={joinAddress}
          onChange={(event) => setJoinAddress(event.target.value)}
        />
        <button type="submit" disabled={joinSubmitting}>
          {joinSubmitting ? '提交中…' : 'Join 节点'}
        </button>
        <small className="note">使用管理节点地址触发 join 操作。</small>
      </form>

      {error && <div className="error-message">{error}</div>}
      {actionFeedback && (
        <div className={actionFeedback.type === 'success' ? 'success-message' : 'error-message'}>
          {actionFeedback.message}
        </div>
      )}

      <div className="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>地址</th>
              <th>状态</th>
              <th>角色</th>
              <th>Up 序号</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {members.length === 0 && (
              <tr>
                <td colSpan={5}>{loading ? '加载中…' : '暂无成员信息'}</td>
              </tr>
            )}
            {members.map((member, index) => {
              const address = resolveAddress(member);
              const statusClass = member.status ? `badge status-${member.status}` : 'badge';
              const roles = member.roles ?? [];
              const isExpanded = expandedAddress === address;
              const inspectInfo = inspectState[address];
              const hasAddress = address.length > 0;
              const actionDisabled = (suffix: string) => !hasAddress || pendingActionKey === `${address}:${suffix}`;

              return (
                <Fragment key={`${address || member.uniqueAddress?.address || 'member'}-${index}`}>
                  <tr>
                    <td>
                      <div>{address || '未知地址'}</div>
                      {member.uniqueAddress?.longUid !== undefined && (
                        <small className="note">UID: {member.uniqueAddress.longUid}</small>
                      )}
                    </td>
                    <td>
                      <span className={statusClass}>{member.status}</span>
                    </td>
                    <td>
                      {roles.length === 0 ? '—' : roles.map((role) => (
                        <span className="badge" key={role}>
                          {role}
                        </span>
                      ))}
                    </td>
                    <td>{member.upNumber ?? '—'}</td>
                    <td>
                      <div className="actions">
                        <button
                          type="button"
                          className="secondary"
                          onClick={() => toggleInspect(address)}
                          disabled={!hasAddress}
                        >
                          {isExpanded ? '收起详情' : '查看详情'}
                        </button>
                        <button
                          type="button"
                          className="warning"
                          disabled={actionDisabled('gracefulLeave')}
                          onClick={() => void runMemberAction(address, onGracefulLeave, 'gracefulLeave')}
                        >
                          优雅离开 (PUT)
                        </button>
                        <button
                          type="button"
                          className="danger"
                          disabled={actionDisabled('deleteLeave')}
                          onClick={() => void runMemberAction(address, onDeleteLeave, 'deleteLeave')}
                        >
                          立即离开 (DELETE)
                        </button>
                        <button
                          type="button"
                          className="danger"
                          disabled={actionDisabled('down')}
                          onClick={() => void runMemberAction(address, onDown, 'down')}
                        >
                          Down 节点
                        </button>
                      </div>
                    </td>
                  </tr>
                  {isExpanded && (
                    <tr>
                      <td colSpan={5}>
                        {inspectInfo?.loading && <div>详情加载中…</div>}
                        {inspectInfo?.error && <div className="error-message">{inspectInfo.error}</div>}
                        {!inspectInfo?.loading && !inspectInfo?.error && (
                          <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                            {JSON.stringify(inspectInfo?.detail ?? member, null, 2)}
                          </pre>
                        )}
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default MembersPanel;
