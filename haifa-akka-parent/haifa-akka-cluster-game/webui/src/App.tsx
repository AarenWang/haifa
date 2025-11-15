import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  ClusterMember,
  ClusterMembersResponse,
  fetchClusterMembers,
  fetchMemberDetail,
  fetchShardInfo,
  joinCluster,
  leaveCluster,
  memberOperation,
  prepareForShutdown
} from './api/clusterApi';
import { usePersistentState } from './hooks/usePersistentState';
import ClusterShardsPanel from './components/ClusterShardsPanel';
import DomainEventsPanel from './components/DomainEventsPanel';
import MembersPanel from './components/MembersPanel';

interface ActionFeedback {
  type: 'success' | 'error';
  message: string;
}

function App(): JSX.Element {
  const [baseUrl, setBaseUrl] = usePersistentState<string>('akka-management-base-url', 'http://127.0.0.1:8558');
  const [clusterData, setClusterData] = useState<ClusterMembersResponse | null>(null);
  const [membersError, setMembersError] = useState<string | null>(null);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [actionFeedback, setActionFeedback] = useState<ActionFeedback | null>(null);

  const loadMembers = useCallback(async () => {
    setLoadingMembers(true);
    setMembersError(null);
    try {
      const data = await fetchClusterMembers(baseUrl);
      setClusterData(data);
      setLastUpdated(new Date());
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setMembersError(message);
    } finally {
      setLoadingMembers(false);
    }
  }, [baseUrl]);

  useEffect(() => {
    void loadMembers();
  }, [loadMembers]);

  const runAction = useCallback(
    async (action: () => Promise<void>, successMessage: string) => {
      setActionFeedback(null);
      try {
        await action();
        setActionFeedback({ type: 'success', message: successMessage });
        await loadMembers();
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        setActionFeedback({ type: 'error', message });
      }
    },
    [loadMembers]
  );

  const handleJoin = useCallback(
    async (address: string) => {
      await runAction(() => joinCluster(baseUrl, address), `已向 ${address} 发送加入指令`);
    },
    [baseUrl, runAction]
  );

  const handleDeleteLeave = useCallback(
    async (address: string) => {
      await runAction(() => leaveCluster(baseUrl, address), `已请求 ${address} 立即离开集群`);
    },
    [baseUrl, runAction]
  );

  const handleGracefulLeave = useCallback(
    async (address: string) => {
      await runAction(() => memberOperation(baseUrl, address, 'Leave'), `已请求 ${address} 优雅离开`);
    },
    [baseUrl, runAction]
  );

  const handleDown = useCallback(
    async (address: string) => {
      await runAction(() => memberOperation(baseUrl, address, 'Down'), `已请求 Down 节点 ${address}`);
    },
    [baseUrl, runAction]
  );

  const handlePrepareShutdown = useCallback(async () => {
    await runAction(() => prepareForShutdown(baseUrl), '已触发 Prepare-for-full-shutdown 指令');
  }, [baseUrl, runAction]);

  const handleInspectMember = useCallback(
    async (address: string): Promise<ClusterMember> => fetchMemberDetail(baseUrl, address),
    [baseUrl]
  );

  const handleReload = useCallback(() => {
    void loadMembers();
  }, [loadMembers]);

  const summaryCards = useMemo(() => {
    if (!clusterData) {
      return [];
    }

    const cards: Array<{ label: string; value?: string | string[] }> = [
      { label: '当前节点', value: clusterData.selfNode },
      { label: 'Leader', value: clusterData.leader },
      { label: 'Oldest', value: clusterData.oldest }
    ];

    if (clusterData.unreachable && clusterData.unreachable.length > 0) {
      cards.push({ label: '不可达节点', value: clusterData.unreachable });
    }

    if (clusterData.seenBy && clusterData.seenBy.length > 0) {
      cards.push({ label: 'Seen By', value: clusterData.seenBy });
    }

    return cards;
  }, [clusterData]);

  const handleBaseUrlSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const url = String(formData.get('baseUrl') ?? '').trim();
    if (url.length > 0) {
      setBaseUrl(url);
    }
  };

  return (
    <div className="app-container">
      <div className="panel">
        <div className="section-header">
          <h1>Akka Cluster 管理控制台</h1>
        </div>
        <p>
          通过 Akka Management HTTP API 观察和控制集群。默认指向 <code>http://127.0.0.1:8558\</code>，可根据部署环境修改。
        </p>
        <form className="inline-form" onSubmit={handleBaseUrlSubmit}>
          <input type="text" name="baseUrl" defaultValue={baseUrl} placeholder="管理接口地址" />
          <button type="submit">保存</button>
          <button type="button" className="secondary" onClick={handleReload} disabled={loadingMembers}>
            {loadingMembers ? '刷新中…' : '立即刷新'}
          </button>
          <button type="button" className="warning" onClick={() => void handlePrepareShutdown()}>
            Prepare for full shutdown
          </button>
        </form>
        {lastUpdated && <small className="note">最近一次成功刷新：{lastUpdated.toLocaleString()}</small>}
      </div>

      <MembersPanel
        loading={loadingMembers}
        error={membersError}
        actionFeedback={actionFeedback}
        clusterData={clusterData}
        summaryCards={summaryCards}
        onJoin={handleJoin}
        onDeleteLeave={handleDeleteLeave}
        onGracefulLeave={handleGracefulLeave}
        onDown={handleDown}
        onInspect={handleInspectMember}
      />

      <ClusterShardsPanel baseUrl={baseUrl} fetchShardInfo={fetchShardInfo} />

      <DomainEventsPanel baseUrl={baseUrl} />
    </div>
  );
}

export default App;
