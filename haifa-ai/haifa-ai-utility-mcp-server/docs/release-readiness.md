# Release readiness

Decision: **READY_WITH_LIMITATIONS**

The utility service and DeerFlow connection have automated offline protocol/contract coverage. The public contract contains exactly 16 tools; production has fail-fast issuer/audience/Origin requirements and rate limiting. Provider base URLs are trusted configuration, HTTPS-only in production, with redirects disabled and bounded response handling.

## Verified release evidence

- `mvn -pl haifa-ai/haifa-ai-utility-mcp-server -am verify`: PASS (30 tests, 0 failures/errors/skips).
- `mvn -pl haifa-ai/haifa-ai-deerflow -am verify`: PASS; the reactor also verifies utility, and DeerFlow ran 478 tests with 0 failures, 0 errors and 4 pre-existing skips.
- The versioned contract snapshot contains exactly 16 tools and is checked without automatic snapshot rewriting.
- DeerFlow starts a real local utility Streamable HTTP server in its integration test and performs initialize, list and call through the production client path.
- `git diff --check`: PASS. Research documents 44 and 45 are unchanged; Fetch remains disabled in the production topology.

Limitations before a general production declaration:

- Codex, Claude Code, VS Code and Cursor real-client smoke entries remain `NOT_RUN`.
- OAuth integration needs a deployment-environment issuer fixture and sanitized evidence.
- Provider fixture coverage proves retry/cache/MIME/size plus service mappings, but does not yet emulate every upstream enum and connection failure.
- Prometheus scraping requires adding/configuring a registry in the deployment.
- Fetch MCP is not part of utility v1 and remains disabled in DeerFlow production because third-party server egress controls are not proven.

Rollback: disable `haifa.ai.deerflow.mcp.enabled`, remove the utility connection from the active profile, and roll back the utility artifact. Do not silently modify an existing tool Schema; publish a reviewed contract version and retain the previous artifact during rollback.
