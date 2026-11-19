# Fetch MCP security and parity

## Decision

Fetch MCP is **not production-enabled**. It is an optional compatibility connection only. The existing DeerFlow `web_fetch` remains the `WEB_FETCH` owner. A profile may atomically switch ownership to `fetch`, but both owners are never exposed in one catalog.

## Local controls implemented

- connection and tool must be statically configured and allowlisted;
- semantic mapping must explicitly mark the remote tool `WEB_FETCH`;
- local risk and approval policy cannot be weakened by remote annotations;
- every Fetch call requires approval, including a locally read-only tool; with approval disabled it is denied;
- session approval keys bind connection, original tool, capability owner, snapshot version, destination host and argument hash;
- URL preflight permits canonical public HTTPS on port 443 only;
- userinfo, fragments, encoded/non-ASCII hosts, integer hosts, localhost/local names, private/link-local/multicast/metadata and IPv4-mapped IPv6 destinations are denied;
- arguments shown for approval are length-bounded and redact token/secret/password/cookie/authorization/API-key fields;
- resource links returned by MCP are recorded as links and are not fetched automatically.

## Why production remains disabled

Client preflight cannot prove the remote Fetch server’s egress behavior. Production suitability also requires server-side validation after every DNS resolution and redirect, binding the connected IP to the validated address, redirect limits/loop detection, header stripping, decompression and MIME/body/parser limits, response isolation, timeouts, and tests demonstrating those controls. The referenced third-party server has not been attested here.

Accordingly, `application.yml` keeps `fetch.enabled=false` and an empty allowlist. Enabling it is only suitable for a controlled compatibility profile whose egress is enforced by a verified proxy/firewall or by a separately reviewed `web-fetch-mcp-server`.

## Parity with built-in `web_fetch`

| Area | Existing `web_fetch` | Fetch MCP |
| --- | --- | --- |
| Default owner | Yes | No |
| Runtime | In-process DeerFlow provider | External MCP connection |
| Local policy/approval | Existing DeerFlow policy | MCP policy plus mandatory approval |
| URL/egress proof | Controlled by existing provider implementation | Unknown behind third-party server |
| Result semantics | Native source/evidence flow | Local `WEB_FETCH` semantic mapping required |
| Resource link | Provider result | Candidate only; never auto-fetched |
| Production status | Existing behavior retained | Disabled |

No existing provider has been deleted or rewritten. A future parity gate must add controlled fake-MCP redirect/rebinding/content fixtures and prove server-side egress before changing this conclusion.
