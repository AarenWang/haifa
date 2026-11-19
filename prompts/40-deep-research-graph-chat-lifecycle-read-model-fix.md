# Deep Research Graph Chat Lifecycle Read Model Fix Prompt

## Problem Description

In graph-first or active-research mode, Deep Research requests can execute `web_search` and `write_file`, produce an apparent final answer, and create report artifacts, but the Deep Research Monitor remains mostly empty:

- `sources = 0`
- `evidence = 0`
- `claims = 0`
- `citations = 0`
- quality stays at the initial placeholder
- `Identified Gaps = ["initial_exploration"]`

Observed case: `threadId=3a78b1be-b820-45eb-aeb2-93e743d6328d` completed with a `web_search` and report artifact, but did not persist structured research sources/evidence/claims/quality updates.

`initial_exploration` is acceptable only immediately after research initialization. It is not a valid completed research state.

## Analysis Conclusion

The backend has two execution paths:

1. Legacy `AgentLoop`
   - Constructs a `CompositeAgentLoopObserver`.
   - Includes `ResearchLoopObserver`.
   - Calls lifecycle hooks such as:
     - `beforeToolExecute`
     - `onToolCompleted`
     - `shouldContinue`
     - `onFinalAnswerProposed`
     - `onFinalAnswerAccepted`
   - `ResearchLoopObserver` persists research read models and quality state.

2. Active `GraphChatRuntime`
   - Used by research requests when graph mode is `GRAPH_FIRST` or `ACTIVE_RESEARCH`.
   - Its nodes directly call model/tools/finalize logic.
   - `ChatExecuteToolsNode` executes tools directly and publishes events, but does not call `AgentLoopObserver`.
   - `ChatFinalizeNode` completes the run without `shouldContinue`, final-answer gating, or accepted-answer postprocessing.

Therefore the active chat graph bypasses the domain lifecycle that Deep Research depends on. `web_search` results are visible as tool events, but they do not flow through `ResearchLoopObserver.onToolCompleted`, so they are not registered in `sources/evidence/claims/citations`. The initial `QualityAssessment` row is never overwritten because final-answer quality hooks are not called.

## Files Implicated

- `SimpleAgentRuntime.shouldUseActiveChatGraph(...)`
- `GraphChatRuntime`
- `GraphChatRuntimeRequest`
- `ChatExecuteToolsNode`
- `ChatParseModelOutputNode` or a new final-answer gate node
- `ChatFinalizeNode`
- `AgentLoop`
- `AgentLoopObserver`
- `ResearchLoopObserver`

## Fix Direction

Do not patch the UI or prompt only. Fix the graph runtime lifecycle boundary.

### Required Behavior

When `GraphChatRuntime` is active, graph nodes must honor the same domain lifecycle used by `AgentLoop`:

1. Tool execution lifecycle:
   - Convert pending graph tool calls into `ToolCall`.
   - Call `observer.beforeToolExecute(runConfig, toolCall)` before actual tool execution.
   - Persist raw `ToolCallResult`.
   - Call `observer.onToolCompleted(runConfig, toolCall, rawToolResult, events, seq, history)` after tool execution.
   - Publish any observer-generated research events.
   - Append observer observation into the next model message window.

2. Final answer lifecycle:
   - If the model returns no pending tools, do not immediately finalize.
   - Call `observer.shouldContinue(...)` first.
   - Call `observer.onFinalAnswerProposed(...)` before accepting final answer.
   - If rejected, append retry instruction as a system message and continue to `CALL_MODEL`.
   - On actual finalization, call `observer.onFinalAnswerAccepted(...)` and persist/use the returned final answer and metadata.

3. Report artifact lifecycle:
   - Avoid double report artifact creation when a research run already generated a model-authored report artifact via `write_file`.
   - Prefer a single runtime-owned final report path, or suppress auto-report creation if a visible markdown report artifact already exists for the run.

## Regression Test Cases

### Test 1: Graph-first research persists sources

Setup:
- Enable graph mode `GRAPH_FIRST`.
- Run a research request.
- Mock model sequence:
  1. assistant calls `web_search`
  2. assistant attempts premature final answer
- Mock search provider returns at least one URL.

Expected:
- At least one `SOURCE_FOUND` or persisted source exists for run.
- `deerflow_sources` / source API returns `sources.size() > 0`.
- Quality assessment no longer only contains `initial_exploration` after completion.

### Test 2: Graph-first research quality gate rejects premature final answer

Setup:
- Same as Test 1, but no fetched evidence exists.

Expected:
- Final answer is not accepted until quality gate decides it can complete or max-step/budget constraints are reached.
- A system retry/continuation instruction is appended to the model context.
- Quality assessment includes actual gaps such as missing evidence/citations, not only `initial_exploration`.

### Test 3: No duplicate report artifacts

Setup:
- Research run calls `write_file` to create a markdown artifact, then completes.

Expected:
- Only one visible final report artifact is associated with the run, or the runtime explicitly marks generated drafts vs final reports.

## Acceptance Criteria

- Deep Research Monitor shows populated sources after graph-first research uses `web_search`.
- Completed research runs do not leave quality at only `initial_exploration`.
- Graph chat runtime and legacy AgentLoop share the same research lifecycle semantics.
- Existing chat behavior remains unchanged for normal chat mode.
- Backend tests pass with focused Maven verification.