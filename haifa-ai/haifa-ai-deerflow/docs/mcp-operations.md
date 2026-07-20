# DeerFlow MCP operations and governance

## Topologies

Default production uses one `utility` Streamable HTTP connection. It owns WEATHER, TIME, CURRENCY, HOLIDAY_WORKDAY, CALCULATION, UNIT_CONVERSION and ENCYCLOPEDIA groups. The built-in DeerFlow fetch provider owns WEB_FETCH.

A compatibility profile may enable one of `open-meteo`, `time-reference`, `frankfurter-remote`, `wikipedia-reference`, or `fetch` only after recording a controlled `tools/list` fixture and pinning its original tool names in `allowed-tools`. Set the corresponding capability owner in the same configuration change. Empty allowlists expose nothing and are intentional defaults.

Fetch experimental additionally requires a reviewed tool allowlist, `semantic-mappings.<tool>=WEB_FETCH`, `capability-mappings.<tool>=WEB_FETCH`, `default-risk` and working approval. It is not production-suitable until server-side egress controls are verified.

## Policy order

The effective order is fail-closed: connection disabled/failed, local deny/allowlist, active catalog identity, local risk, run/skill tool policy, capability owner, approval, then call. Remote description, Schema, annotations, server identity and result never lower local policy.

Third-party tools default to `UNKNOWN`; tools with no local mapping are not callable. `UNKNOWN`, `WRITE`, `DESTRUCTIVE`, and `WEB_FETCH` require approval. Approval metadata is redacted and binds the connection, original/exposed tool, capability owner, snapshot version, destination host and argument hash.

## Lifecycle and diagnostics

Connections are application-scoped. Required initialization failure fails startup; optional failure is `DEGRADED`. Discovery follows pagination and atomically publishes an immutable versioned snapshot. `notifications/tools/list_changed` triggers publication. An explicit refresh failure preserves last-known-good definitions as `STALE`; default policy rejects new calls. Shutdown closes every SDK client and restricted STDIO transport.

`GET /api/deerflow/health` reports MCP enabled/running, snapshot version/age and sanitized per-connection state, required flag, tool count and last successful discovery. It never reports tokens, headers, command arguments, full URL queries, Schemas or descriptions.

Low-cardinality Micrometer instruments include:

- `mcp.registry.snapshot.version`
- `mcp.connection.initialize.duration`
- `mcp.discovery.duration`, `mcp.discovery.tool.count`, `mcp.discovery.refresh`
- `mcp.tool.call.duration`, `mcp.result.bytes`

Troubleshooting:

- `AUTH_FAILED`: validate issuer/token/scope without logging the token.
- `DEGRADED`: check transport reachability and protocol compatibility.
- `STALE`: inspect the last refresh error type and snapshot age; recover the server and refresh/restart.
- zero tools: verify explicit allowlist, local risk mapping and capability owner.
- owner conflict: atomically choose one provider for the capability; do not expose semantic duplicates.

Rollback is `haifa.ai.deerflow.mcp.enabled=false`, followed by application restart. This removes MCP tools without restoring the former placeholder.
