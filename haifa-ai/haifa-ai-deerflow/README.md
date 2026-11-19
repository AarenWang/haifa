# haifa-ai-deerflow

## Skill execution and image generation

A Skill is an instruction package (`SKILL.md`, scripts, templates, and references); it is not a Tool and does not create a Spring `AgentTool` bean. `tool_search` lists callable tools only. Media skills use the same atomic runtime as every other skill:

`read_file -> write_file -> bash -> present_files`

The model sees stable virtual paths. The host `skills-root` is exposed read-only as `/mnt/skills`; uploads are read-only, while workspace and outputs are writable. A non-zero exit code, timeout, policy denial, or process-start error is a failed tool result. `present_files` registers only non-empty regular files below `/mnt/user-data/outputs` and supplies final-answer delivery evidence.

Image generation is implemented by `skills/public/image-generation/scripts/generate.py`, not by an `ImageGenerationTool`. See [the Skill/Tool runtime guide](docs/skill-tool-runtime.md) for provider models and configuration.

Sandbox execution supports `local-restricted`, opt-in `local-trusted`, and `docker`. Local Trusted inherits the host toolchain after filtering sensitive variables and is only for trusted single-user development; see [the sandbox runtime guide](docs/sandbox-runtime.md).

`haifa-ai-deerflow` 是一个 Java / Spring Boot WebFlux 实现的 DeerFlow 风格 Agent Runtime。它把 SSE run、thread/message 持久化、工具调用、skills、deep research、uploads/artifacts、memory/persona、SQLite 审计和 Spring AI Graph 运行时组合在一个本地可运行的后端服务中。

本文按当前代码事实维护。更详细的架构说明见 [docs/architecture.md](docs/architecture.md)，
语音模型接入与运行方式见 [docs/voice-conversation.md](docs/voice-conversation.md)。

## 当前能力

| 能力 | 当前代码事实 |
| --- | --- |
| 运行接口 | `POST /api/deerflow/runs/stream` 创建 run，并以 SSE 返回 `AgentEvent`。 |
| 运行模式 | 支持 `CHAT` 和 `RESEARCH`；research 当前是统一 agent runtime 下的模式，不是独立旧 pipeline。 |
| Graph runtime | 默认 `haifa.ai.deerflow.graph.enabled=true`、`mode=GRAPH_FIRST`。chat/research active 路径当前都进入 `GraphChatRuntime`；`GraphResearchRuntime` 仍在代码中但 `SimpleAgentRuntime.shouldUseActiveResearchGraph()` 返回 `false`。 |
| Legacy loop | `AgentLoop` 作为 graph 关闭或 fallback 路径保留。 |
| 模型接入 | 通过 Spring AI `ChatClient`；`openai` 和 `google-genai` Maven profile 分别引入 OpenAI-compatible 与 Google 原生 starter；未配置真实 provider 时使用 fallback 行为。 |
| 工具系统 | 内置文件、上传文件、web search/fetch、image、bash、run_script、todo、clarification、subagent、research evidence/claim/citation 等工具。 |
| Skills | 从 `${skillsRoot}/public` 和 `${skillsRoot}/custom` 加载 Markdown skill；research mode 自动激活 `deep-research`。 |
| Deep research | 由 `RunMode.RESEARCH`、`deep-research` skill、middleware、research observer、plan/source/evidence/claim/citation/quality/budget stores 和 report artifact 共同实现。 |
| 持久化 | SQLite + JPA 保存 threads、runs、messages、events、model steps、tool calls/executions、todos、clarifications、memory、research plans/sources、graph checkpoints 等。 |
| 文件状态 | uploads、outputs、workspace 在 `${user.dir}/data/user-data/**` 下；artifact registry 会保存到 `${userDataRoot}/artifacts.json`。 |
| 语音对话 | 浏览器 PCM 推流，支持 fake、阿里云百炼和火山引擎 ASR/TTS provider；ASR 与 TTS 可混合选厂商。 |
| 安全边界 | 当前 YAML 开启 local sandbox、脚本执行和网络访问，并关闭 approval；适合本地实验，不是生产安全默认值。 |

## 本地运行

从仓库根目录运行：

```bash
mvn -pl haifa-ai/haifa-ai-deerflow -am spring-boot:run
```

使用 OpenAI-compatible provider：

```powershell
$env:OPENAI_API_KEY = "your-key"
$env:OPENAI_BASE_URL = "https://api.openai.com/v1/chat/completions"
$env:HAIFA_DEERFLOW_MODEL = "gpt-4o-mini"
$env:LLM_NETWORK_PROXY_URL = "socks5://127.0.0.1:1080" # Optional: http, https, or socks5
$env:LLM_NETWORK_PROXY_USERNAME = "proxy-user"         # Optional
$env:LLM_NETWORK_PROXY_PASSWORD = "proxy-password"     # Optional
mvn -pl haifa-ai/haifa-ai-deerflow -Popenai spring-boot:run
```

Gemini 3 工具调用必须使用 Google 原生 profile，Spring AI 1.1.x 的 OpenAI-compatible
adapter 不会保留 Gemini `thought_signature`：

```powershell
$env:GEMINI_API_KEY = "your-gemini-key" # OPENAI_API_KEY 也可作为兼容回退
$env:HAIFA_DEERFLOW_MODEL = "gemini-3-flash-preview"
$env:LLM_NETWORK_PROXY_URL = "socks5://127.0.0.1:1080" # Optional
mvn -pl haifa-ai/haifa-ai-deerflow -Pgoogle-genai spring-boot:run
```

不要同时启用 `openai` 和 `google-genai` profile。Google 原生 SDK 使用 Google endpoint，
不会读取 OpenAI-compatible 的完整 `OPENAI_BASE_URL`。

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
| `haifa.ai.deerflow.network-proxy-url` | `${LLM_NETWORK_PROXY_URL:}`; optional `http`, `https`, or `socks5` proxy for blocking and streaming LLM calls |
| `haifa.ai.deerflow.network-proxy-username` | `${LLM_NETWORK_PROXY_USERNAME:}`; optional proxy username |
| `haifa.ai.deerflow.network-proxy-password` | `${LLM_NETWORK_PROXY_PASSWORD:}`; optional proxy password |
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
- MCP 已接入 Graph First 与 legacy AgentLoop；默认关闭，启用后从动态、版本化的工具快照暴露受治理工具。

## MCP integration

DeerFlow supports named Streamable HTTP and restricted STDIO MCP connections. Remote tools are exposed as `mcp__<connection>__<tool>` and routed through a structured snapshot identity; runtime routing never reparses the exposed string. The default topology connects only the self-hosted utility server and assigns it weather, time, currency, holiday/workday, calculation, unit and encyclopedia ownership.

Enable the local utility connection with environment-backed configuration:

```powershell
$env:HAIFA_DEERFLOW_MCP_ENABLED='true'
$env:UTILITY_MCP_URL='http://127.0.0.1:8091'
```

The local utility profile does not require a token or Origin. For a secured production utility endpoint, additionally
set `UTILITY_MCP_ORIGIN` to an allowlisted Origin, put the bearer token in a secret environment variable, and set
`UTILITY_MCP_TOKEN_ENV` to that variable's **name** (for example `UTILITY_MCP_TOKEN`), never to the token value itself.

Third-party compatibility connections are disabled by default. Each requires a static URL/command, explicit tool allowlist, local risk, semantic mappings and capability ownership. Unknown tools are not exposed. Required connection failure prevents startup; optional failure is degraded. Refresh failure preserves last-known-good definitions as `STALE` and denies new calls by default.

Microsoft Learn is provided by the self-hosted `utility` connection as the read-only `mcp__utility__microsoft_docs_search`, `mcp__utility__microsoft_docs_fetch`, and `mcp__utility__microsoft_code_sample_search` tools. DeerFlow does not directly connect to the Microsoft endpoint. Configure or disable the integration on Utility MCP with `UTILITY_MCP_MICROSOFT_LEARN_ENABLED`; no API key is required.

Fetch MCP is an experimental, high-risk connection and remains disabled until server-side egress controls are proven. See [Fetch security and parity](docs/fetch-mcp-security-and-parity.md).

Health output includes the active tool count; MCP connection status is available from the connection manager without secrets. Disable the feature with `haifa.ai.deerflow.mcp.enabled=false` to return to the pre-MCP runtime without a placeholder tool.
