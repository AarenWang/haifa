import { FormEvent, useState } from 'react';
import type { ClusterShardInfo } from '../api/clusterApi';

interface ClusterShardsPanelProps {
  baseUrl: string;
  fetchShardInfo: (baseUrl: string, regionName: string) => Promise<ClusterShardInfo>;
}

const ClusterShardsPanel = ({ baseUrl, fetchShardInfo }: ClusterShardsPanelProps): JSX.Element => {
  const [regionName, setRegionName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [shardInfo, setShardInfo] = useState<ClusterShardInfo | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = regionName.trim();
    if (!trimmed) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const info = await fetchShardInfo(baseUrl, trimmed);
      setShardInfo(info);
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      setError(message);
      setShardInfo(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="panel">
      <div className="section-header">
        <h2>分片状态</h2>
      </div>
      <p>
        通过 <code>GET /cluster/shards/&lt;region&gt;</code> 查看指定 Shard Region 的负载分布。请填写 Region 名称
        （例如 <code>player-shard-region</code>）。
      </p>
      <form className="inline-form" onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Shard Region 名称"
          value={regionName}
          onChange={(event) => setRegionName(event.target.value)}
        />
        <button type="submit" disabled={loading}>
          {loading ? '查询中…' : '查询分片'}
        </button>
        <button
          type="button"
          className="secondary"
          onClick={() => setShardInfo(null)}
        >
          清空结果
        </button>
      </form>

      {error && <div className="error-message">{error}</div>}

      {shardInfo && (
        <div className="table-wrapper" style={{ marginTop: '1rem' }}>
          <table>
            <thead>
              <tr>
                <th>Region</th>
                <th>Shard IDs</th>
                <th>实体数量</th>
              </tr>
            </thead>
            <tbody>
              {shardInfo.regions?.map((region) => (
                <tr key={region.region}>
                  <td>{region.region}</td>
                  <td>
                    {region.shardIds.length === 0
                      ? '—'
                      : region.shardIds.map((id) => (
                          <span className="badge" key={id}>
                            {id}
                          </span>
                        ))}
                  </td>
                  <td>
                    {region.entityCountByShard ? (
                      <ul style={{ margin: 0, paddingLeft: '1.2rem' }}>
                        {Object.entries(region.entityCountByShard).map(([shardId, count]) => (
                          <li key={shardId}>
                            {shardId}: {count}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      '—'
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <details style={{ marginTop: '1rem' }}>
            <summary>查看原始响应</summary>
            <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{JSON.stringify(shardInfo, null, 2)}</pre>
          </details>
        </div>
      )}
    </div>
  );
};

export default ClusterShardsPanel;
