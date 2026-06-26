# Task for Another Agent: Phase 3 Middleware Chain

You are working in `D:\workspace\haifa` on the `haifa-ai/haifa-ai-deerflow` module.

## Goal

Extend the current Phase 1 + Phase 2 prototype with a small DeerFlow-style
middleware chain while keeping the existing SSE run endpoint working.

## Context

Current module:

- `SimpleAgentRuntime` creates runs, executes safe local tools, calls the
  `AgentModelClient`, and emits SSE `AgentEvent`s.
- `ToolRegistry` owns deterministic safe tool planning.
- `RunManager` is in-memory.
- `SpringAiAgentModelClient` is the Spring AI boundary with fallback behavior
  when no provider is configured.

Reference plan:

- `deer-flow-research/11-java-deerflow-implementation-plan.md`
- Python reference: `deer-flow/backend/packages/harness/deerflow/agents/lead_agent/agent.py`

## Required Work

1. Add a `middleware` package under `org.wrj.haifa.ai.deerflow`.
2. Define:
   - `AgentMiddleware`
   - `MiddlewareChain`
   - `MiddlewareOrder`
   - `AgentRuntimeContext`
3. Implement at least these middlewares:
   - `DynamicContextMiddleware`: inject current date/time and workspace root into model prompt context.
   - `TokenBudgetMiddleware`: reject or final-answer early when request + tool observations exceed a configurable character budget.
   - `ToolErrorHandlingMiddleware`: convert tool exceptions into tool observation text instead of failing the whole run.
4. Refactor `SimpleAgentRuntime` to pass model prompt assembly through the middleware chain.
5. Add unit tests for middleware ordering and token budget behavior.
6. Keep `mvn -pl haifa-ai/haifa-ai-deerflow -am test` green.

## Constraints

- Do not introduce database, Redis, Docker, or MCP in this task.
- Keep the module runnable without external model credentials.
- Do not remove the existing safe local tools or SSE endpoint.
- Prefer small interfaces and Java records over large mutable maps.

## Acceptance Criteria

- `/api/deerflow/runs/stream` still streams run/tool/model events.
- The model prompt includes middleware-injected dynamic context.
- A configured small token/character budget produces a controlled event rather
  than an uncaught exception.
- Tests cover middleware order and at least one budget stop case.
