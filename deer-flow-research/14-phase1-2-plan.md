# DeerFlow Phase 1 + 2 开发计划

## 当前状态

Phase 0.5 已完成：Thread / Run / Message 基础模型、Threads API、前端按 thread 展示对话流。

## 目标

实现 Phase 1（Research Mode API 与事件语义）和 Phase 2（模型驱动 Tool Calling Loop），保持现有 chat 行为兼容。

---

## Stage 1: Phase 1 后端 — 核心模型与配置扩展

### 1.1 新建文件

| 文件 | 说明 |
|------|------|
| `agent/RunMode.java` | enum `CHAT`, `RESEARCH` |
| `agent/ResearchOptions.java` | record: depth, timeWindow, maxSources, requireCitations, outputFormat |
| `agent/ResearchDepth.java` | enum `QUICK`, `STANDARD`, `DEEP` |
| `agent/ResearchTimeWindow.java` | enum `LATEST`, `LAST_30_DAYS`, `LAST_YEAR`, `ALL_TIME` |
| `agent/ResearchOutputFormat.java` | enum `ANSWER`, `REPORT` |
| `agent/ResearchEventType.java` | 研究专用事件类型（可选，如果直接扩展 AgentEventType） |

### 1.2 修改文件

| 文件 | 修改内容 |
|------|----------|
| `web/RunCreateRequest.java` | 增加 `mode`, `researchOptions` 字段；缺省 mode=chat |
| `agent/AgentRequest.java` | 增加 `mode`, `researchOptions` 字段 |
| `config/DeerFlowProperties.java` | 增加 `maxResearchSteps`, `maxResearchSources`, `maxFetchesPerRun`, `researchTimeout`, `defaultResearchDepth` |
| `agent/AgentRunConfig.java` | 增加 `mode`, `researchOptions` 字段 |
| `agent/AgentEventType.java` | 增加研究事件类型 |
| `web/RunController.java` | 将 mode/researchOptions 传递给 AgentRequest |
| `agent/SimpleAgentRuntime.java` | 根据 mode 选择路径（chat 保持现有逻辑，research 走新路径） |
| `run/RunManager.java` | `create` 方法增加接收 mode/researchOptions metadata |
| `run/RunRecord.java` | 不需要修改，metadata 已支持 |
| `resources/application.yml` | 增加 research 配置默认值 |

---

## Stage 2: Phase 1 前端 — Research Mode UI

### 2.1 修改文件

| 文件 | 修改内容 |
|------|----------|
| `types.ts` | 扩展 `RunRequest` 增加 mode/researchOptions；扩展 `DeerFlowEventType` 增加研究事件；扩展 `AppState` 增加 researchMode 等 |
| `api/deerflowClient.ts` | `RunRequest` 类型同步 |
| `components/TaskComposer.tsx` | 增加 Research mode toggle、depth selector、source count selector、output as report checkbox |
| `components/EventCard.tsx` | 增加新研究事件的标题、图标、颜色映射，保持向后兼容 |
| `state/deerflowReducer.ts` | 处理新增事件类型，增加 research phase 映射 |
| `App.tsx` | 传递 research 状态到 TaskComposer |

---

## Stage 3: Phase 2 后端 — AgentLoop 与工具调用循环

### 3.1 新建包 `org.wrj.haifa.ai.deerflow.agent.loop`

| 文件 | 说明 |
|------|------|
| `LoopConfig.java` | maxSteps, maxToolCalls, timeoutMs, researchOptions |
| `ModelStep.java` | 记录一次模型调用：prompt, response, toolCalls, timestamp |
| `ToolCall.java` | id, toolName, arguments, status |
| `ToolCallResult.java` | id, toolName, arguments, status, result, error, duration, metadata |
| `LoopStepResult.java` | stepIndex, modelStep, toolResults, stopReason |
| `AgentLoop.java` | 核心循环：多轮 model step → parse tool calls → dispatch → write results → detect final answer |
| `ToolCallParser.java` | 从模型响应中解析工具调用请求（支持 JSON 格式） |
| `MockSearchTool.java` | mock web_search 工具，返回硬编码搜索结果 |
| `MockFetchTool.java` | mock web_fetch 工具，返回硬编码网页内容 |
| `ResearchAgentRuntime.java` | research 模式运行时，使用 AgentLoop |

### 3.2 修改文件

| 文件 | 修改内容 |
|------|----------|
| `agent/AgentEventType.java` | 增加 `MODEL_DELTA`, `TOOL_CALL_REQUESTED`, `TOOL_STARTED`, `TOOL_COMPLETED`, `RESEARCH_STEP_COMPLETED` |
| `agent/SimpleAgentRuntime.java` | 提取公共方法；research mode 委托给 ResearchAgentRuntime |
| ` DeerFlowApplication.java` | 确保 ResearchAgentRuntime 被注册为 Bean |

---

## Stage 4: 测试

### 4.1 后端测试

| 文件 | 测试内容 |
|------|----------|
| `AgentLoopTest.java` | mock model 先请求 search 再请求 fetch；step limit 生效；工具失败后继续 |
| `ResearchOptionsValidationTest.java` | 缺省值和校验 |
| `SimpleAgentRuntimeTest.java` | 旧请求不带 mode 仍按 chat 执行；SSE 事件兼容 |
| `RunControllerTest.java` | 新 API 参数传递正确 |

### 4.2 前端测试

- `npm run build` 成功
- 旧 UI 行为不变
- Research mode 切换正确

---

## 验收标准

- [ ] 旧请求不带 mode 时仍按 chat 执行
- [ ] Research mode 有独立 UI 入口和配置
- [ ] AgentLoop 支持多轮 tool/model step
- [ ] mock model 能 search → fetch → follow-up
- [ ] step limit 生效
- [ ] 工具失败能记录并继续
- [ ] 前端能展示 research 事件
- [ ] `mvn -pl haifa-ai/haifa-ai-deerflow -am test` 通过
- [ ] `npm run build` 通过
