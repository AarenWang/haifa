# 开发提示词：解耦 Lead Agent 与统一运行时入口

本提示词用以指导未来 Haifa-AI-DeerFlow 运行时架构的重塑开发，消除目前硬编码的 `streamResearch` 与 `streamChat` 双分支，走向**通用 Lead Agent + 中间件拦截链 + 动态技能**的通用解耦架构。

---

## 1. 核心目标与重构范围

### 1.1 消除硬编码分支
- 废弃 `SimpleAgentRuntime.java` 中通过 `request.isResearchMode()` 硬编码切分 `streamResearch` 和 `streamChat` 的逻辑。
- 统一 `stream(AgentRequest request)` 接口入口，所有请求路径共享完全相同的通用 Agent 执行循环。

### 1.2 核心业务逻辑下沉与中间件解耦
- **澄清机制 (Clarification Gate)**：从初始化硬编码调用，改写为通用的 `ClarificationMiddleware`。如果 LLM 决定发起澄清提问（通过调用新增的 `ask_clarification` 工具），中间件会拦截调用结果，将执行流挂起（Suspended），并流式发布 `CLARIFICATION_REQUIRED` 事件。
- **任务规划 (Research Plan)**：废弃静态的 `ResearchPlanner`，转由 `TodoMiddleware` 与 `write_todos` 工具替代。研究计划作为通用 TodoList 维护在运行时上下文中，计划的创建和状态更新均由 LLM 自行使用工具更改。
- **质量门禁 (Quality Gate)**：移除静态统计证据数量的硬编码门禁类。如果 TodoList 中仍有未完成的探索维度/探索任务，`TodoMiddleware` 会在模型尝试输出最终答案或提前退出的步骤进行拦截，并在交互历史中注入系统警告，强迫其继续执行任务。

---

## 2. 核心技术架构设计

### 2.1 统一的执行时序

```mermaid
sequenceDiagram
    autonumber
    participant Client as 客户端 (Web)
    participant Runtime as SimpleAgentRuntime
    participant Chain as MiddlewareChain
    participant Loop as AgentLoop
    participant Model as ModelClient

    Client->>Runtime: POST /api/run/chat (或 research 请求)
    Runtime->>Chain: execute(AgentRuntimeContext)
    Note over Chain: 加载人设、激活 Skills (如 deep-research)、读取历史
    Chain-->>Runtime: ModelPrompt (包含组装好的 System & User Prompts)
    Runtime->>Loop: run(prompt)
    
    loop 多轮 Tool-calling 循环
        Loop->>Model: generate(prompt)
        Model-->>Loop: ModelResponse
        alt LLM 调用了 ask_clarification 工具
            Loop->>Runtime: 触发 ClarificationMiddleware 拦截
            Runtime-->>Client: 推送 CLARIFICATION_REQUIRED 事件并挂起
        else LLM 尝试在 Todo 未完成时输出最终回复
            Loop->>Runtime: 触发 TodoMiddleware 拦截
            Note over Runtime: 强行插入错误 Prompt 驳回并继续循环
        end
    end
```

---

## 3. 关键代码骨架实现

### 3.1 统一的 `SimpleAgentRuntime.stream`

```java
@Override
public Flux<AgentEvent> stream(AgentRequest request) {
    return Flux.defer(() -> {
        long runStartTime = System.currentTimeMillis();
        String threadId = StringUtils.hasText(request.threadId()) ? request.threadId() : UUID.randomUUID().toString();
        
        // 1. 统一构建 Run 记录，模式改为通用运行
        String modelName = StringUtils.hasText(request.model()) ? request.model() : this.properties.getModel();
        Map<String, Object> runMetadata = new HashMap<>();
        runMetadata.put("source", "sse");
        runMetadata.put("userId", request.userId());
        runMetadata.put("isResearch", request.isResearchMode()); // 作为元数据，不由代码逻辑控制分支
        
        RunRecord run = this.runManager.create(threadId, modelName, runMetadata);
        this.runManager.markRunning(run.runId());
        
        AgentRunConfig config = new AgentRunConfig(
                threadId, run.runId(), modelName, true, false,
                this.properties.getMaxIterations(), 
                Path.of(this.properties.getWorkspaceRoot()), 
                RunMode.CHAT,
                ResearchOptions.defaults(), 
                runMetadata
        );
        
        AtomicInteger seq = new AtomicInteger();
        List<Skill> activeSkills = resolveActiveSkills(request);
        List<ToolResult> toolResults = new ArrayList<>();
        
        // 2. 依次构建 Runtime 上下文并流经中间件
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, toolResults, this.properties, activeSkills);
        Mono<ModelPrompt> promptMono = new MiddlewareChain(this.middlewares).next(context);
        
        return promptMono.flatMapMany(prompt -> {
            LoopConfig loopConfig = new LoopConfig(
                    config.maxIterations(),
                    30, // 统一最大步数限制
                    this.properties.getResearchTimeout(),
                    ResearchOptions.defaults()
            );
            
            // 3. 统一拉起通用的多轮 Tool-calling 循环
            return this.agentLoop.run(
                    loopConfig,
                    config,
                    prompt.systemPrompt(),
                    prompt.userPrompt(),
                    seq,
                    this.toolPolicyService,
                    activeSkills,
                    request.uploadedFileIds()
            );
        });
    });
}
```

### 3.2 澄清拦截器 `ClarificationMiddleware`
用于拦截模型调用 `ask_clarification` 工具的动作，并流式暂停运行状态：

```java
@Component
@MiddlewareOrder(20) // 在基本人设和记忆注入之后运行
public class ClarificationMiddleware implements AgentMiddleware {
    private final ResearchClarificationStore clarificationStore;

    public ClarificationMiddleware(ResearchClarificationStore clarificationStore) {
        this.clarificationStore = clarificationStore;
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain chain) {
        // 如果当前线程有待处理的澄清提问，挂起当前运行并流式等待用户输入
        String threadId = context.config().threadId();
        var pendingClarification = clarificationStore.findPending(threadId);
        
        if (pendingClarification.isPresent()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.PRECONDITION_REQUIRED, 
                    "Clarification required: " + pendingClarification.get().getQuestion()
            ));
        }
        
        return chain.next(context);
    }
}
```

---

## 4. 交付与验证标准

### 4.1 核心代码编译通过
- 移除所有 `SimpleAgentRuntime.java` 中的 `streamResearch()`、`streamChat()` 分支。
- 确保整个 `haifa-ai-deerflow` 无编译错误并顺利打包。

### 4.2 单元测试覆盖
- 编写 `UnifiedStreamRuntimeTest` 验证 `stream` 方法。
- 模拟用户发起 Deep Research 请求，模型在 step 1 调用 `write_todos`，在 step 2 遭遇未完成计划强制拦截，直至计划完成才返回最终报告的完整多轮测试路径。
