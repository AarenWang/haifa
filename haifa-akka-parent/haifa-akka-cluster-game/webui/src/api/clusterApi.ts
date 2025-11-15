export interface ClusterMember {
  address?: string;
  node?: string;
  status: string;
  roles?: string[];
  upNumber?: number;
  age?: number;
  uniqueAddress?: {
    address: string;
    longUid?: number;
  };
  [key: string]: unknown;
}

export interface ClusterMembersResponse {
  members: ClusterMember[];
  selfNode?: string;
  leader?: string;
  oldest?: string;
  unreachable?: string[];
  seenBy?: string[];
  unreachableDataCenters?: string[];
  [key: string]: unknown;
}

export interface ClusterShardInfo {
  regions: Array<{
    region: string;
    shardIds: string[];
    entityCountByShard?: Record<string, number>;
  }>;
  state?: unknown;
}

export async function fetchClusterMembers(baseUrl: string): Promise<ClusterMembersResponse> {
  const response = await fetch(buildUrl(baseUrl, '/cluster/members'));
  await ensureSuccess(response, '读取集群成员失败');
  return (await response.json()) as ClusterMembersResponse;
}

export async function fetchMemberDetail(baseUrl: string, address: string): Promise<ClusterMember> {
  const response = await fetch(buildUrl(baseUrl, `/cluster/members/${encodeURIComponent(address)}`));
  await ensureSuccess(response, `读取节点 ${address} 信息失败`);
  return (await response.json()) as ClusterMember;
}

export async function joinCluster(baseUrl: string, address: string): Promise<void> {
  const response = await fetch(buildUrl(baseUrl, '/cluster/members/'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ address }).toString()
  });
  await ensureSuccess(response, `加入节点 ${address} 失败`);
}

export async function leaveCluster(baseUrl: string, address: string): Promise<void> {
  const response = await fetch(buildUrl(baseUrl, `/cluster/members/${encodeURIComponent(address)}`), {
    method: 'DELETE'
  });
  await ensureSuccess(response, `让节点 ${address} 离开失败`);
}

export async function memberOperation(baseUrl: string, address: string, operation: 'Down' | 'Leave'): Promise<void> {
  const response = await fetch(buildUrl(baseUrl, `/cluster/members/${encodeURIComponent(address)}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ operation }).toString()
  });
  await ensureSuccess(response, `${operation} 节点 ${address} 失败`);
}

export async function prepareForShutdown(baseUrl: string): Promise<void> {
  const response = await fetch(buildUrl(baseUrl, '/cluster/'), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ operation: 'Prepare-for-full-shutdown' }).toString()
  });
  await ensureSuccess(response, '触发 Prepare-for-full-shutdown 失败');
}

export async function fetchShardInfo(baseUrl: string, regionName: string): Promise<ClusterShardInfo> {
  const response = await fetch(buildUrl(baseUrl, `/cluster/shards/${encodeURIComponent(regionName)}`));
  await ensureSuccess(response, `读取分片 ${regionName} 信息失败`);
  return (await response.json()) as ClusterShardInfo;
}

function buildUrl(baseUrl: string, path: string): string {
  const normalizedBase = baseUrl.replace(/\/$/, '');
  return `${normalizedBase}${path}`;
}

async function ensureSuccess(response: Response, defaultMessage: string): Promise<void> {
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    const message = text || `${defaultMessage} (HTTP ${response.status})`;
    throw new Error(message);
  }
}
