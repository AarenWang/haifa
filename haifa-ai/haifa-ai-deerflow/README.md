# haifa-ai-deerflow

## Skill execution and image generation

A Skill is an instruction package (`SKILL.md`, scripts, templates, and references); it is not a Tool and does not create a Spring `AgentTool` bean. `tool_search` lists callable tools only. Media skills use the same atomic runtime as every other skill:

`read_file -> write_file -> bash -> present_files`

The model sees stable virtual paths. The host `skills-root` is exposed read-only as `/mnt/skills`; uploads are read-only, while workspace and outputs are writable. A non-zero exit code, timeout, policy denial, or process-start error is a failed tool result. `present_files` registers only non-empty regular files below `/mnt/user-data/outputs` and supplies final-answer delivery evidence.

Image generation is implemented by `skills/public/image-generation/scripts/generate.py`, not by an `ImageGenerationTool`. See [the Skill/Tool runtime guide](docs/skill-tool-runtime.md) for provider models and configuration.

Sandbox execution supports `local-restricted`, opt-in `local-trusted`, and `docker`. Local Trusted inherits the host toolchain after filtering sensitive variables and is only for trusted single-user development; see [the sandbox runtime guide](docs/sandbox-runtime.md).

`haifa-ai-deerflow` 是一个 Java / Spring Boot WebFlux 实现的 DeerFlow 风格 Agent Runtime。它把 SSE run、thread/message 持久化、工具调用、skills、deep research、uploads/artifacts、memory/persona、SQLite 审计和 Spring AI Graph 运行时组合在一个本地可运行的后端服务中。

本文按当前代码事实维护。更详细的架构说明见 [docs/architecture.md](docs/architecture.md)。

## 当前能力

| 能力 | 当前代码事实 |
| --- | --- |
| 运行接口 | `POST /api/deerflow/runs/stream` 创建 run，并以 SSE 返回 `AgentEvent`。 |
| 运行模式 | 支持 `CHAT` 和 `RESEARCH`；research 当前是统一 agent runtime 下的模式，不是独立旧 pipeline。 |
| Graph runtime | 默认 `haifa.ai.deerflow.graph.enabled=true`、`mode=GRAPH_FIRST`。chat/research active 路径当前都进入 `GraphChatRuntime`；`GraphResearchRuntime` 仍在代码中但 `SimpleAgentRuntime.shouldUseActiveResearchGraph()` 返回 `false`。 |
| Legacy loop | `AgentLoop` 作为 graph 关闭或 fallback 路径保留。 |
| 模型接入 | 通过 Spring AI `ChatClient`；`openai` Maven profile 引入 OpenAI starter；未配置真实 provider 时使用 fallback 行为。 |
| 工具系统 | 内置文件、上传文件、web search/fetch、image、bash、run_script、todo、clarification、subagent、research evidence/claim/citation 等工具。 |
| Skills | 从 `${skillsRoot}/public` 和 `${skillsRoot}/custom` 加载 Markdown skill；research mode 自动激活 `deep-research`。 |
| Deep research | 由 `RunMode.RESEARCH`、`deep-research` skill、middleware、research observer、plan/source/evidence/claim/citation/quality/budget stores 和 report artifact 共同实现。 |
| 持久化 | SQLite + JPA 保存 threads、runs、messages、events、model steps、tool calls/executions、todos、clarifications、memory、research plans/sources、graph checkpoints 等。 |
| 文件状态 | uploads、outputs、workspace 在 `${user.dir}/data/user-data/**` 下；artifact registry 会保存到 `${userDataRoot}/artifacts.json`。 |
| 安全边界 | 当前 YAML 开启 local sandbox、脚本执行和网络访问，并关闭 approval；适合本地实验，不是生产安全默认值。 |

## 本地运行

从仓库根目录运行：

```bash
mvn -pl haifa-ai/haifa-ai-deerflow -am spring-boot:run
```

使用 OpenAI-compatible provider：

```powershell
$env:OPENAI_API_KEY = "your-key"
$env:OPENAI_BASE_URL = "https://api.openai.com"
$env:HAIFA_DEERFLOW_MODEL = "gpt-4o-mini"
mvn -pl haifa-ai/haifa-ai-deerflow -Popenai spring-boot:run
```

服务默认监听：

```text
http://localhost:8095
```

健康检查：

```bash
curl http://localhost:8095/api/deerflow/health
```

## 关键配置

当前 `src/main/resources/application.yml` 的重要默认值：

| 配置 | 值 |
| --- | --- |
| `server.port` | `8095` |
| `deerflow.persistence.sqlite.path` | `${user.dir}/data/deerflow.sqlite` |
| `spring.datasource.url` | `jdbc:sqlite:${deerflow.persistence.sqlite.path}?journal_mode=WAL&busy_timeout=5000` |
| `spring.datasource.hikari.maximum-pool-size` | `1` |
| `haifa.ai.deerflow.user-data-root` | `${user.dir}/data/user-data` |
| `haifa.ai.deerflow.workspace-root` | `${user.dir}/data/user-data/workspace` |
| `haifa.ai.deerflow.uploads-root` | `${user.dir}/data/user-data/uploads` |
| `haifa.ai.deerflow.outputs-root` | `${user.dir}/data/user-data/outputs` |
| `haifa.ai.deerflow.skills-root` | `${user.dir}/skills` |
| `haifa.ai.deerflow.graph.enabled` | `true` |
| `haifa.ai.deerflow.graph.mode` | `GRAPH_FIRST` |
| `haifa.ai.deerflow.graph.checkpoint.enabled` | `true` |
| `haifa.ai.deerflow.max-iterations` | `20` |
| `haifa.ai.deerflow.max-research-steps` | `100` |
| `haifa.ai.deerflow.max-fetches-per-run` | `200` |
| `haifa.ai.deerflow.bash-enabled` | `true` |
| `haifa.ai.deerflow.run-script-enabled` | `true` |
| `haifa.ai.deerflow.sandbox.enabled` | `true` |
| `haifa.ai.deerflow.sandbox.backend` | `local-trusted` |
| `haifa.ai.deerflow.approval.enabled` | `false` |

注意：`DeerFlowProperties` 的代码默认值与 YAML 不完全相同。实际从本仓库启动时，以 YAML 和环境变量覆盖后的值为准。

## Web Search / Fetch Provider

当前注册的 provider：

| 工具 | Provider |
| --- | --- |
| `web_search` | `aliyun`, `duckduckgo` |
| `web_fetch` | `aliyun`, `jina` |

YAML 默认使用 Aliyun：

```yaml
haifa:
  ai:
    deerflow:
      tools:
        web-search:
          provider: aliyun
          api-key: ${ALIYUN_API_KEY:${DASHSCOPE_API_KEY:}}
        web-fetch:
          provider: aliyun
          api-key: ${ALIYUN_API_KEY:${DASHSCOPE_API_KEY:}}
```

`ProviderConfigurationValidator` 会在启动时校验 provider 是否有注册实现，以及需要 API key 的 provider 是否配置了 key。

## API 示例

### Chat

```bash
curl -N -X POST http://localhost:8095/api/deerflow/runs/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"列出 workspace 文件，并说明你看到了什么","mode":"CHAT"}'
```

### Deep Research

```bash
curl -N -X POST http://localhost:8095/api/deerflow/runs/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Research the current state of Java agent sandbox design and produce a concise report.",
    "mode": "RESEARCH",
    "researchOptions": {
      "depth": "STANDARD",
      "timeWindow": "LATEST",
      "maxSources": 8,
      "requireCitations": true,
      "outputFormat": "REPORT"
    }
  }'
```

### 上传文件

```bash
curl -F "file=@notes.md" http://localhost:8095/api/deerflow/uploads
```

返回的 `fileId` 可传入 run：

```bash
curl -N -X POST http://localhost:8095/api/deerflow/runs/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"总结上传文件","uploadedFileIds":["<fileId>"],"mode":"CHAT"}'
```

## 常用查询接口

| 接口 | 说明 |
| --- | --- |
| `GET /api/deerflow/health` | 健康检查和配置摘要。 |
| `POST /api/deerflow/runs/stream` | 创建并流式执行 run。 |
| `POST /api/deerflow/runs/{runId}/resume` | 回答 clarification 或 approval 后恢复 run。 |
| `GET /api/deerflow/runs/{runId}` | 查询 run。 |
| `GET /api/deerflow/runs/{runId}/events` | 查询事件历史。 |
| `GET /api/deerflow/runs/{runId}/observability` | 查询运行观测视图。 |
| `GET /api/deerflow/runs/{runId}/todos` | 查询 TodoList。 |
| `GET /api/deerflow/runs/{runId}/sources` | 查询 research sources。 |
| `GET /api/deerflow/runs/{runId}/evidence` | 查询 research evidence。 |
| `GET /api/deerflow/runs/{runId}/plan` | 查询 research plan。 |
| `GET /api/deerflow/runs/{runId}/work-items` | 查询 research work items。 |
| `GET /api/deerflow/runs/{runId}/claims` | 查询 research claims。 |
| `GET /api/deerflow/runs/{runId}/citations` | 查询 citations。 |
| `GET /api/deerflow/runs/{runId}/quality` | 查询 quality assessments。 |
| `GET /api/deerflow/runs/{runId}/budget` | 查询 budget ledger。 |
| `GET /api/deerflow/threads/{threadId}/messages` | 查询 thread 消息。 |
| `GET /api/deerflow/artifacts/{artifactId}/download` | 下载 artifact。 |
| `GET /api/deerflow/clarifications/pending` | 查询待回答 clarification。 |
| `POST /api/deerflow/clarifications/{clarificationId}/answer` | 回答 clarification。 |

## Build / Test

编译：

```bash
mvn -pl haifa-ai/haifa-ai-deerflow -DskipTests clean compile
```

测试：

```bash
mvn -pl haifa-ai/haifa-ai-deerflow test
```

打包：

```bash
mvn -pl haifa-ai/haifa-ai-deerflow -am package
```

运行 jar：

```bash
java -jar haifa-ai/haifa-ai-deerflow/target/haifa-ai-deerflow-1.0-SNAPSHOT.jar
```

## 部署注意事项

- 持久化 `data/`，特别是 SQLite、`data/user-data/uploads`、`data/user-data/outputs` 和 `data/user-data/artifacts.json`。
- 当前 SQLite 配置通过 WAL、`busy_timeout=5000` 和 Hikari 单连接降低锁竞争，但仍更适合单进程/单实例部署。
- 多实例、高并发写入或多个进程共享同一个 SQLite 文件时，应迁移到服务型数据库或引入更明确的写入串行化策略。
- 生产或共享环境不建议使用当前 YAML 的 local sandbox + approval disabled 组合。需要执行脚本时优先使用 docker sandbox，并重新开启 HITL approval。
- API key 使用环境变量或密钥系统注入，不要写入仓库。

## 当前边界

- 这是 Java DeerFlow runtime 原型，不是 Python deer-flow 的完整等价实现。
- 当前 active research 走统一 `GraphChatRuntime`，不要把旧 pipeline UI 状态当作唯一事实来源。
- `GraphResearchRuntime`、`ResearchAgentGraph` 仍在代码中，但当前入口未启用。
- `ApprovalStore` 是内存态，pending approval 不能跨进程重启恢复。
- `ArtifactService` 的 registry 不是数据库表，而是内存 + `${userDataRoot}/artifacts.json`。
- `mcp-enabled` 有配置项，但主运行链路尚未接入 MCP 工具实现。
