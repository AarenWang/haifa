# Akka Cluster Management Web UI

React 单页应用，帮助通过 Akka Management HTTP API 观察和管理 `haifa-akka-cluster-game` 集群。

## 功能概览

- **集群概览**：展示成员列表、角色、状态以及 `selfNode`、`leader` 等信息。
- **成员操作**：可对节点执行 `join`、`DELETE leave`、`PUT leave` 以及 `PUT down` 等操作。
- **Shard Region 查询**：通过 `/cluster/shards/<name>` 获取指定 Region 的分片分布。
- **领域事件监听**：使用 Server-Sent Events 订阅 `/cluster/domain-events`，实时查看集群事件。

## 本地开发

```bash
cd haifa-akka-parent/haifa-akka-cluster-game/webui
npm install
npm run dev
```

默认会在 `http://127.0.0.1:5173` 启动开发服务器。确保 Akka Management HTTP API 可通过浏览器访问（例如 `http://127.0.0.1:8558`）。

## 生产构建

```bash
npm run build
npm run preview
```

构建产物位于 `dist/` 目录，可托管于任意静态资源服务器。
