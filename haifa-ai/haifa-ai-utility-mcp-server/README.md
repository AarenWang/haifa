# Haifa Utility MCP Server

面向 DeerFlow、Codex、Claude Code、VS Code、Cursor 等标准 MCP Client 的无状态公共工具服务。服务固定暴露 `/mcp` Streamable HTTP endpoint，v1 只声明 tools capability，不透明转发第三方 MCP；Microsoft Learn 通过本服务的三个受控稳定工具接入。

## v1 工具

- Open-Meteo：`location_search`、`weather_current`、`weather_forecast`、`air_quality`
- Time：`time_now`、`time_convert`
- Frankfurter：`currency_rate`、`currency_convert`
- Nager.Date：`holiday_list`、`holiday_next`、`workday_is_workday`、`workday_add`
- 本地安全计算：`calculate`、`unit_convert`
- Wikimedia：`wikipedia_search`、`wikipedia_summary`
- Microsoft Learn：`microsoft_docs_search`、`microsoft_docs_fetch`、`microsoft_code_sample_search`

所有工具返回等价的 JSON TextContent 与 `structuredContent`，业务错误使用 `isError=true`。公共名称和 Schema 是 v1 版本化合同，上游 DTO 变化不得直接改变合同。

## 本地运行

```powershell
mvn -pl haifa-ai/haifa-ai-utility-mcp-server -am spring-boot:run
```

本地 endpoint 为 `http://127.0.0.1:8091/mcp`。测试默认使用固定 fixture 或本地 loopback server，不访问公网。

## 生产运行

生产必须经 TLS 反向代理，并同时配置 OAuth issuer、audience 和 Origin allowlist，否则启动失败：

```powershell
$env:SPRING_PROFILES_ACTIVE='production'
$env:UTILITY_MCP_JWT_ISSUER='${UTILITY_MCP_JWT_ISSUER}'
$env:UTILITY_MCP_JWT_AUDIENCE='${UTILITY_MCP_JWT_AUDIENCE}'
$env:UTILITY_MCP_ALLOWED_ORIGINS='${UTILITY_MCP_ALLOWED_ORIGINS}'
mvn -pl haifa-ai/haifa-ai-utility-mcp-server -am spring-boot:run
```

访问 `/mcp` 的 token 需要同时具有 `mcp:tools:list` 和 `mcp:tools:call` scope。当前 Streamable HTTP 在同一路径承载发现与调用，因此生产基线在 HTTP 资源边界要求两项 scope；未来若协议 transport 提供可靠的 method-level 授权挂点，可再细分最小权限。不要将 token、完整请求/响应、用户隐私或带敏感 query 的 URL 写入日志。健康检查为 `/actuator/health`，指标为 `/actuator/metrics`；Prometheus endpoint 只有加入 registry 后才可用。

Provider base URL 只能来自服务端配置，生产仅允许 HTTPS；重定向关闭，单次响应、并发、缓存、超时、一次幂等重试和 circuit breaker 均有边界。Nager.Date 不能可靠表达中国调休时，工作日工具会明确返回不支持，而不是猜测。

## 外部 Provider 代理

代理连接参数兼容 DeerFlow 的 `LLM_NETWORK_PROXY_*` 环境变量，也可以使用 Utility MCP 专属变量覆盖。Provider 默认直连，只有名称出现在 `UTILITY_MCP_PROXY_PROVIDERS` 逗号分隔清单中才会经过代理。

例如仅让 Wikimedia 经过本地 HTTP 代理：

```powershell
$env:UTILITY_MCP_PROXY_URL='http://127.0.0.1:7890'
$env:UTILITY_MCP_PROXY_PROVIDERS='wikimedia'
mvn -pl haifa-ai/haifa-ai-utility-mcp-server -am spring-boot:run
```

多个 Provider 使用逗号分隔，例如 `UTILITY_MCP_PROXY_PROVIDERS=wikimedia,open-meteo,frankfurter`。支持的名称为 `open-meteo`、`open-meteo-geocoding`、`open-meteo-air-quality`、`frankfurter`、`nager-date`、`wikimedia`；名称忽略首尾空格和大小写，未知名称会导致启动失败，避免因拼写错误而静默直连。

## Microsoft Learn

Utility MCP 默认发布三个 Microsoft Learn 工具：`microsoft_docs_search`、`microsoft_docs_fetch` 和 `microsoft_code_sample_search`。服务在首次调用时以 Streamable HTTP 连接官方端点、动态发现工具，并只调用这三个本地审核过的工具合同；远端不可用不会阻止 Utility MCP 启动。

无需 API Key。默认端点为 `https://learn.microsoft.com/api/mcp`，可按需配置：

```powershell
$env:UTILITY_MCP_MICROSOFT_LEARN_ENABLED='true'
$env:UTILITY_MCP_MICROSOFT_LEARN_ENDPOINT='https://learn.microsoft.com/api/mcp'
```

`microsoft_docs_fetch` 仅接受 `https://learn.microsoft.com/...` 域名下的文档 URL。结果统一封装为 Utility MCP 的 `data + meta` 合同，来源标记为 `microsoft-learn`。

代理支持 `http://`、`https://` 和 `socks5://`，认证信息使用 `UTILITY_MCP_PROXY_USERNAME`、`UTILITY_MCP_PROXY_PASSWORD`；未设置 Utility 专属变量时回退到 `LLM_NETWORK_PROXY_URL`、`LLM_NETWORK_PROXY_USERNAME`、`LLM_NETWORK_PROXY_PASSWORD`。

启用代理的 Provider 会记录 `mcp_provider_proxy_configured`，只包含 Provider、代理协议、主机和端口，不记录代理用户名或密码。若 Provider 已开启代理但代理地址为空，服务会在启动时直接失败，避免静默回退到直连。

## MCP 请求日志

`/mcp` 默认以 INFO 级别记录三类结构化日志：

- `mcp_connection_received`：HTTP 连接进入，包含请求 ID、HTTP 方法、匿名化 peer hash 和协议版本 header；
- `mcp_initialize_request_completed`：初始化请求处理完成，包含安全化后的 client name/version 和请求协议版本；
- `mcp_request_completed`：每个请求的 JSON-RPC method、`tools/call` 的工具名、HTTP 状态、耗时和结果类型。

服务优先复用合法的 `X-Request-Id`，否则生成 UUID，并在响应中返回同名 header。日志不会记录 Authorization、token、工具参数、完整请求/响应正文或原始客户端地址。例如：

```text
event=mcp_connection_received requestId=... httpMethod=POST path=/mcp peerHash=... protocolHeader=2025-11-25
event=mcp_initialize_request_completed requestId=... clientName=haifa-test-client clientVersion=1.0 requestedProtocolVersion=2025-11-25 peerHash=...
event=mcp_request_completed requestId=... rpcMethod=tools/call toolName=time_now status=200 durationMs=16 outcome=completed
```

外部 Provider 调用异常统一记录：

- `mcp_provider_attempt_failed`：单次调用失败，记录 provider、受控 path、attempt、错误码、是否重试和根异常类型；
- `mcp_provider_request_failed`：全部尝试失败，记录最终错误码、可重试性和总耗时；
- `mcp_provider_request_rejected`：circuit breaker 已打开或 bulkhead 拒绝请求；
- `mcp_provider_request_build_failed`：Provider 请求路径不合法或构造失败。

日志只包含不带 query 的受控 path，不包含查询值、完整 URL、上游响应正文、token 或其他请求 header：

```text
event=mcp_provider_attempt_failed provider=open-meteo path=/v1/forecast attempt=1 maxAttempts=2 code=UPSTREAM_UNAVAILABLE retryable=true willRetry=true errorType=UtilityToolException rootCause=HttpConnectTimeoutException detail=open-meteo request failed
event=mcp_provider_request_failed provider=open-meteo path=/v1/forecast code=UPSTREAM_UNAVAILABLE retryable=true errorType=UtilityToolException rootCause=HttpConnectTimeoutException detail=open-meteo request failed durationMs=2012
```

## 验证

```powershell
mvn -pl haifa-ai/haifa-ai-utility-mcp-server -am verify
mvn -pl haifa-ai/haifa-ai-deerflow -am verify
```

协议证据见 [mcp-compatibility-spike.md](docs/mcp-compatibility-spike.md)，客户端模板见 [client-compatibility.md](docs/client-compatibility.md)，发布结论见 [release-readiness.md](docs/release-readiness.md)。
