# Spring AI 1.1.8 MCP compatibility spike

## Verified coordinates

- Spring Boot `3.5.7`
- Spring AI `1.1.8`
- MCP Java SDK `0.18.3`（由 Spring AI BOM 解析）
- Server starter `spring-ai-starter-mcp-server-webmvc`
- Client starter `spring-ai-starter-mcp-client`
- Transport: stateless Streamable HTTP, endpoint `/mcp`

`McpProtocolIntegrationTest` 启动真实 Spring Server，并以 SDK `HttpClientStreamableHttpTransport` 完成 initialize、tools/list、tools/call 和 close。实际协商协议版本为 `2025-11-25`；连续调用不依赖 session id。`McpConnectionManagerIntegrationTest` 从 DeerFlow 真实连接 utility，并验证 namespace、list/call、结构化错误和关闭。

## Capability matrix

| Capability | Result |
| --- | --- |
| tools/list and tools/call | Supported and tested |
| TextContent | Supported and tested |
| structuredContent | Supported and tested |
| tool outputSchema | SDK model supports it; catalog contract tested |
| annotations | SDK model supports it; 16 contracts tested |
| tools/list pagination | Client loop implemented; utility v1 emits one page |
| tools/list_changed | Client consumer implemented; server v1 catalog is static |
| resources/prompts/completions | Deliberately not declared |
| sampling/elicitation | Deliberately not declared |

Spring AI STATELESS server emits an SDK warning for an unregistered `notifications/initialized` handler, but initialize/list/call complete correctly. This is recorded as an upstream integration limitation, not hidden by custom protocol emulation.

## Protocol and error behavior

- Tool validation/business errors are successful HTTP MCP responses containing `isError=true` and a stable utility error code.
- Unknown JSON-RPC method/tool and malformed protocol messages remain protocol errors.
- SDK transport sends compatible JSON/SSE Accept headers and protocol-version negotiation headers.
- Production auth is implemented at the `/mcp` HTTP resource boundary. JWT issuer, audience, both `mcp:tools:list` and `mcp:tools:call` scopes, and an Origin allowlist are mandatory. Because Streamable HTTP multiplexes discovery and calls on one path, method-level scope separation is not claimed.

Conclusion: the pinned versions can implement the required standard server and named multi-client lifecycle without upgrading dependencies. Pagination and list-change receiving are represented; dynamic server-side catalog mutation is outside utility v1.
