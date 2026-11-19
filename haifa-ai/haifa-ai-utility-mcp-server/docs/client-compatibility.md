# Client compatibility smoke matrix

Endpoint template: `${UTILITY_MCP_PUBLIC_URL}/mcp`. Authentication: OAuth bearer token injected by the client/secret store; never commit it.

Minimum smoke: connect, list 19 tools, call `time_now`, call `weather_current` against a permitted environment, then call `time_now` with an invalid timezone and confirm a tool error is displayed.

| Client | Status | Evidence / limitation |
| --- | --- | --- |
| DeerFlow (repository SDK client) | PASS | `McpConnectionManagerIntegrationTest`; initialize/list/call/error/close |
| Raw Spring AI MCP client | PASS | `McpProtocolIntegrationTest`; protocol `2025-11-25` |
| Codex | NOT_RUN | Requires a running reachable candidate and installed client |
| Claude Code | NOT_RUN | Requires a running reachable candidate and installed client |
| VS Code Agent | NOT_RUN | Requires manual workspace/client approval smoke |
| Cursor | NOT_RUN | Requires installed client and manual approval smoke |
| OpenAI Responses API | NOT_RUN | Requires public or tunneled endpoint and credentials; not a CI gate |

Example generic remote configuration (adapt field names to the client):

```json
{
  "type": "streamable-http",
  "url": "${UTILITY_MCP_PUBLIC_URL}/mcp",
  "headers": {
    "Authorization": "Bearer ${UTILITY_MCP_TOKEN}",
    "Origin": "${UTILITY_MCP_CLIENT_ORIGIN}"
  }
}
```

For a release-candidate smoke, record client/server version, commit, date, negotiated protocol, transport/auth mode, list count, calls, structured-content/error behavior, sanitized log link and PASS/FAIL. Configuration presence alone is not PASS evidence.
