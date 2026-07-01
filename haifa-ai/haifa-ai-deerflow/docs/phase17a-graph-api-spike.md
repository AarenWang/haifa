# Phase 17A - Spring AI Alibaba Graph API Spike

This note records the Spring AI Alibaba Graph Core API verified for `haifa-ai-deerflow`.

## Dependency

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-graph-core</artifactId>
    <version>${spring.ai.alibaba.version}</version>
</dependency>
```

Current parent property:

```xml
<spring.ai.alibaba.version>1.1.2.3</spring.ai.alibaba.version>
```

`com.alibaba.cloud.ai:spring-ai-alibaba-graph-core:1.1.2.3` resolves from Maven Central.

The module dependency tree resolves Graph Core's Spring AI transitive dependencies through the existing Spring AI BOM, so observed Spring AI artifacts are `1.1.8`:

- `spring-ai-commons`
- `spring-ai-rag`
- `spring-ai-retry`
- `spring-ai-autoconfigure-retry`
- `spring-ai-deepseek`
- `spring-ai-zhipuai`

## Verified Core Types

The smoke test uses these package names and method shapes from version `1.1.2.3`:

| Concern | Verified API |
| --- | --- |
| Graph definition | `com.alibaba.cloud.ai.graph.StateGraph` |
| Graph compilation | `StateGraph.compile(CompileConfig)` |
| Compiled graph | `com.alibaba.cloud.ai.graph.CompiledGraph` |
| State container | `com.alibaba.cloud.ai.graph.OverAllState` |
| Key strategy | `com.alibaba.cloud.ai.graph.KeyStrategy` and `KeyStrategy.builder()` |
| Node action | `com.alibaba.cloud.ai.graph.action.AsyncNodeAction` |
| Node update type | `CompletableFuture<Map<String, Object>>` |
| Invoke | `CompiledGraph.invoke(Map<String, Object>, RunnableConfig)` |
| Stream | `CompiledGraph.stream(Map<String, Object>, RunnableConfig)` |
| Stream output | `com.alibaba.cloud.ai.graph.NodeOutput` |
| Run config | `com.alibaba.cloud.ai.graph.RunnableConfig` |
| Compile config | `com.alibaba.cloud.ai.graph.CompileConfig` |
| Checkpoint saver SPI | `com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver` |
| In-memory saver | `com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver` |
| Saver config | `com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig` |

## Checkpoint API

`CompileConfig.builder().saverConfig(...)` accepts a `SaverConfig`.

`BaseCheckpointSaver` exposes:

```java
Collection<Checkpoint> list(RunnableConfig config);
Optional<Checkpoint> get(RunnableConfig config);
RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception;
BaseCheckpointSaver.Tag release(RunnableConfig config) throws Exception;
```

For Phase 17A, only `MemorySaver` is used. SQLite/JDBC checkpoint persistence should not be implemented until Phase 17E confirms the production saver strategy.

## Smoke Test

`GraphCoreSmokeTest` verifies:

- `StateGraph` can be constructed with a named graph and `KeyStrategyFactory`.
- Two `AsyncNodeAction` nodes can update `OverAllState`.
- `KeyStrategy.APPEND` appends list state across nodes.
- `CompiledGraph.invoke(...)` returns the final state.
- `CompiledGraph.stream(...)` emits `NodeOutput` entries.
- `MemorySaver` records checkpoint history for a `RunnableConfig.threadId`.

## Compatibility Notes

- `haifa-ai-deerflow` still uses the legacy runtime by default.
- Phase 17A does not connect Graph to `SimpleAgentRuntime`, SSE, tools, research, or subagents.
- Graph runtime configuration is present under `haifa.ai.deerflow.graph.*` but defaults to disabled.
- `spring-ai-alibaba-graph-core` has its own Spring AI transitive dependencies. The module test suite must remain the compatibility gate before any active runtime integration.
