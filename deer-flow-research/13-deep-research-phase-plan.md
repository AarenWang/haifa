# Java DeerFlow 深度研究能力迭代 Phase Plan

## 1. 目标

本文将 `12-deep-research-gap-plan.md` 中规划的深度研究能力拆分为可迭代交付的 Phase plan，并将 `prompts/02-migration-prompt-tool-skill.md` 中的 Prompt、Tools、Skills 迁移任务融合到 Phase 3 开始的主线开发中。

拆分原则：

- 每个 Phase 都要形成可验证的增量，而不是只交付内部重构。
- 优先打通端到端闭环，再逐步增强研究质量、长任务能力和并行能力。
- Backend / Frontend / Observability / Tests 同步推进，避免只完成后端能力但用户无法感知。
- `prompts/02-migration-prompt-tool-skill.md` 继续保留，作为迁移输入和验收参考，但不再作为一条与 deep research phase 平行推进的独立开发线。
- 不把 Phase 5 生产 Sandbox、Auth / JWT / OIDC、IM Channel、Redis StreamBridge、完整 PostgreSQL / Flyway 大改造纳入本文范围。

目标闭环：

```text
Research Mode
  -> Iterative Tool Calling
  -> Prompt / Tool / Skill Contract
  -> Source Registry
  -> Evidence Extraction
  -> Research Plan
  -> Quality Gate
  -> Cited Report
  -> Artifact Delivery
  -> Resume / Continue
  -> Parallel Subagents
```

## 2. 与 `02-migration` 的关系

`prompts/02-migration-prompt-tool-skill.md` 中的任务不再单独排期，而是按下面方式吸收到 Phase 3 之后：

| `02-migration` 任务 | 融入 Phase |
| --- | --- |
| Lead Agent / Research Prompt Template 迁移 | Phase 3 |
| Deep Research Skill 默认激活与方法论迁移 | Phase 3 |
| Tool 三层建模：built-in / configured / MCP | Phase 3 |
| Tool catalog / ToolSearchTool / 安全边界 | Phase 3 |
| Public skills 迁移、SkillParser/Storage/Renderer 增强 | Phase 3 |
| ResearchOptions 注入 prompt | Phase 3 |
| `ask_clarification` 与 clarification gate 对齐 | Phase 5 |
| `present_files` 与 artifact 报告交付对齐 | Phase 6 |
| `task` 工具与 subagent prompt section 对齐 | Phase 8 |
| MCP 动态工具展示与 prompt / catalog 对齐 | Phase 3 起步，Phase 8 前补完 |

结论：

- `02-migration` 的基础设施部分是 Phase 3 的前半部分。
- `02-migration` 的交互深化部分分别落入 Phase 5、Phase 6、Phase 8。

## 3. Phase 总览

| Phase | 名称 | 核心产出 | 用户可见价值 |
| --- | --- | --- | --- |
| Phase 0 | Baseline 固化 | 当前 chat / tool / SSE 基线、日志字段、回归测试 | 避免后续改动破坏现有能力 |
| Phase 0.5 | Thread / Run 基础模型 | `ThreadRecord`、`RunRecord`、`MessageRecord`、threads API | 后续研究计划、来源、证据、artifact 都有稳定归属 |
| Phase 1 | Research Mode API 与事件语义 | `mode=research`、`researchOptions`、研究事件流 | 用户能显式选择深度研究模式并看到研究状态 |
| Phase 2 | 模型驱动 Tool Calling Loop | 多轮模型调用、工具调用、停止条件 | Agent 能自己搜索、fetch、再搜索，而不是一次性工具预匹配 |
| Phase 3 | Prompt / Tool / Skill 迁移底座 | DeerFlow 风格 Prompt Template、Tool 三层建模、Skill System 迁移 | 研究模式不再只是“长一点的 prompt”，而有正式运行时契约 |
| Phase 4 | Source / Evidence / Citation Core | 来源注册、证据存储、引用管理、网页清洗 | 研究结论开始可追溯，重复来源被去重 |
| Phase 5 | Research Plan / Progress / Quality Gate | 研究计划、Todo 状态、clarification gate、质量门禁 | 深度研究不再“搜一次就总结”，而是按维度推进 |
| Phase 6 | Report / Artifact Delivery | Markdown 报告、artifact API、预览和下载、`present_files` | 用户拿到可交付报告，而不是超长聊天文本 |
| Phase 7 | Context Compression / Tool Budget / Thread Memory | 长上下文摘要、证据压缩、工具输出预算、thread continuity | 支撑更多来源、更长任务并复用历史研究成果 |
| Phase 8 | Subagent 并行研究 | `task` 工具、subagent runtime、并发限制、主 agent 汇总 | 复杂主题可按维度并行研究，由主 agent 统一综合 |

## 4. Phase 0：Baseline 固化

### 目标

在正式引入深度研究链路前，先固定当前系统边界，避免后续重构影响已有普通问答、工具调用和前端工作区能力。

### 范围

- 梳理当前 `haifa-ai-deerflow` 的 chat run、SSE event、tool registry、`web_search`、`web_fetch` 行为。
- 固化基础配置项和日志字段：`runId`、`threadId`、tool name、tool status、duration、error。
- 增加或补齐最小回归测试：
  - 普通 chat 不触发 research path。
  - `web_search` / `web_fetch` 仍可独立运行。
  - SSE 基础事件不破坏前端解析。
- 文档记录当前能力边界和后续 feature flag。

### 不做

- 不引入新的 agent loop。
- 不改变现有前端交互主路径。
- 不做持久化大改造。

### 验收标准

- 当前普通 chat 和基础网页工具行为有自动化测试或可重复手工验收脚本。
- 后续 research 能力可以通过配置或请求参数关闭。
- 日志中可以稳定定位一次 run 的工具执行过程。

## 5. Phase 0.5：Thread / Run 基础模型

### 目标

在 Research Mode、Source Registry、Artifact 和 Thread Memory 之前，先把当前“字符串 threadId”升级为轻量 Thread 概念。第一版只做内存模型和 API，不引入完整数据库迁移。

### 为什么前置

深度研究不是一次性回答，而是一组围绕同一主题持续发生的 run：

- 第一次 run 生成 research plan。
- 后续 run 修改计划、补充来源或生成报告。
- Artifact 需要同时关联 `runId` 和 `threadId`。
- Source / Evidence 后续需要在同一 thread 内复用。
- Subagent 子 run 需要继承主 run 的 thread 上下文。

如果没有正式 Thread 模型，Phase 3、Phase 5、Phase 7 会反复返工。

### Backend

- 新增模型：

```text
ThreadRecord
  threadId
  title
  status
  metadata
  createdAt
  updatedAt

MessageRecord
  messageId
  threadId
  runId
  role: user | assistant | tool | system
  content
  metadata
  createdAt
```

- 补齐 `RunRecord` 语义：
  - `runId` 表示一次执行。
  - `threadId` 表示该 run 归属的对话/研究工作区。
  - 后续可扩展 `mode=chat | research`。
- 新增内存服务：
  - `ThreadManager`
  - `MessageStore`
- 新增 API：
  - `POST /api/deerflow/threads`
  - `GET /api/deerflow/threads`
  - `GET /api/deerflow/threads/{threadId}`
  - `PATCH /api/deerflow/threads/{threadId}`
  - `GET /api/deerflow/threads/{threadId}/runs`
  - `GET /api/deerflow/threads/{threadId}/messages`
- Run 创建规则：
  - 请求带 `threadId` 时，后端校验或 upsert thread。
  - 请求不带 `threadId` 时，后端创建 thread 后再创建 run。
  - SSE 第一条 `RUN_STARTED` 必须包含最终 `runId` 和 `threadId`。
  - 用户消息、模型最终回答、工具观察按 `MessageRecord` 记录。

### Frontend

- 前端不再只把 thread 当作本地随机字符串。
- 初始化时可创建或复用当前 thread。
- Run history、uploads、后续 artifacts 都使用同一个 canonical `threadId`。
- Advanced Options 中仍允许手动指定 `threadId`，便于调试和复现。

### Tests

- 不带 `threadId` 创建 run 时，后端能生成 thread。
- 带 `threadId` 创建 run 时，后端能复用或 upsert thread。
- `GET /threads/{threadId}/runs` 能返回该 thread 下的 run。
- `GET /threads/{threadId}/messages` 能返回 user / assistant 消息。
- 上传文件仍按 thread 隔离。

### 验收标准

- Thread 有独立模型和查询 API。
- 一次 run 的 user message 和 assistant answer 能追溯到同一 thread。
- 后续 Source / Evidence / Artifact 可以直接挂载到 `threadId`，无需再重构基础关系。

## 6. Phase 1：Research Mode API 与事件语义

### 目标

先建立产品和协议边界，让普通聊天与深度研究在 API、配置、前端入口和事件语义上区分开。

### Backend

- 扩展 run request：

```json
{
  "message": "...",
  "mode": "chat | research",
  "researchOptions": {
    "depth": "quick | standard | deep",
    "timeWindow": "latest | last_30_days | last_year | all_time",
    "maxSources": 12,
    "requireCitations": true,
    "outputFormat": "answer | report"
  }
}
```

- 增加配置项：
  - `maxResearchSteps`
  - `maxResearchSources`
  - `maxFetchesPerRun`
  - `researchTimeout`
  - `defaultResearchDepth`
- 定义研究事件类型：
  - `RESEARCH_PLAN_CREATED`
  - `RESEARCH_DIMENSION_STARTED`
  - `RESEARCH_DIMENSION_COMPLETED`
  - `SOURCE_FOUND`
  - `SOURCE_FETCHED`
  - `EVIDENCE_EXTRACTED`
  - `QUALITY_GATE_STARTED`
  - `QUALITY_GATE_PASSED`
  - `QUALITY_GATE_FAILED`
  - `REPORT_STARTED`
  - `REPORT_COMPLETED`
  - `ARTIFACT_CREATED`
  - `SUBAGENT_STARTED`
  - `SUBAGENT_COMPLETED`

### Frontend

- 增加 Research mode toggle。
- 增加 depth selector、source count selector、output as report checkbox。
- 在 workspace 中预留 research progress 区域。
- 前端能容忍新增事件类型，即使部分事件在后续 Phase 才真正发出。

### Tests

- API 兼容性测试：旧请求不带 `mode` 时仍默认为 `chat`。
- Research options 参数校验测试。
- SSE event JSON schema 或解析测试。

### 验收标准

- 用户能明确选择普通回答或深度研究。
- `mode=research` 会进入独立配置和事件路径。
- 前端能展示“研究模式已启动”的状态，不再把深度研究伪装成普通 chat。

## 7. Phase 2：模型驱动 Tool Calling Loop

### 目标

让 agent 具备迭代使用工具的能力：搜索、选择来源、fetch、基于新内容继续搜索，直到满足停止条件。

### Backend

- 新增包建议：

```text
org.wrj.haifa.ai.deerflow.agent.loop
  AgentLoop
  ToolCall
  ToolCallResult
  ModelStep
```

- 实现 `AgentLoop`：
  - 多轮 model step。
  - tool call request parsing。
  - tool execution dispatch。
  - tool result 回写消息历史。
  - final answer detection。
  - max steps / max tool calls / timeout。
- 标准化 `ToolCall` / `ToolCallResult`：
  - id。
  - tool name。
  - arguments。
  - status。
  - result。
  - error。
  - duration。
  - metadata。
- 为 `web_search` / `web_fetch` 接入 loop。
- 发送中间事件：
  - `MODEL_DELTA`
  - `TOOL_CALL_REQUESTED`
  - `TOOL_STARTED`
  - `TOOL_COMPLETED`
  - `RESEARCH_STEP_COMPLETED`

### Frontend

- 在 trace / activity 区域展示工具调用链。
- 对长时间 run 展示当前 step、已调用工具数量、超时状态。

### Tests

- 用 mock model 验证：先 `web_search`，再基于结果调用 `web_fetch`。
- 验证 step limit 生效，避免无限循环。
- 验证工具失败后能继续、重试或优雅结束。

### 验收标准

- 单个 research run 至少支持 10 到 30 个 tool/model step。
- 模型能从搜索结果中选择 URL 并调用 fetch。
- 模型能基于 fetch 内容继续发起新查询。
- 普通 chat 不被迫进入多轮工具 loop。

## 8. Phase 3：Prompt / Tool / Skill 迁移底座

### 目标

将 `prompts/02-migration-prompt-tool-skill.md` 的基础迁移任务落成正式运行时契约，让 deep research 不再依赖零散 YAML 和轻量提示词拼接，而是拥有 DeerFlow 风格的 Prompt、Tool、Skill 三位一体基础设施。

### 为什么放在 Phase 3

- Phase 4 的 Source / Evidence 依赖稳定的工具命名、工具分类和 prompt 约束。
- Phase 5 的 research plan / quality gate 依赖 deep-research 方法论和 clarification 规则。
- 如果此时不统一 Prompt / Tool / Skill 合同，后续 Phase 4 到 Phase 8 会在名称、能力边界和激活方式上反复返工。

### Backend

- 新增 Java Prompt Builder：
  - `LeadAgentPromptTemplate`
  - `ResearchPromptTemplate`
- 迁移 DeerFlow Lead Agent 核心结构：
  - role
  - user input boundary
  - confidentiality
  - thinking style
  - clarification system
  - skill system
  - working directory
  - citations
  - critical reminders
- 将 `deer-flow/skills/public/deep-research/SKILL.md` 方法论迁移为 research mode 默认约束：
  - Broad Exploration
  - Deep Dive
  - Diversity and Validation
  - Synthesis Check
  - Temporal Awareness
  - citation 要求
- 将 `ResearchOptions` 注入 research prompt：
  - `depth`
  - `timeWindow`
  - `maxSources`
  - `requireCitations`
  - `outputFormat`
- 保留 `application.yml` 覆盖能力：
  - 无配置时使用 Java 默认模板。
  - 有配置时支持覆盖或追加。

### Tools 迁移

- 按 DeerFlow 三层来源重建工具模型：
  - Built-in Tools
  - Configured Tools
  - MCP Tools
- 在 `ToolRegistry` / `DeferredToolCatalog` / `ToolSearchTool` 中显式体现来源与类别。
- 迁移并规范 provider-visible tool names：
  - built-in：`present_files`、`ask_clarification`、`view_image`
  - configured：`web_search`、`web_fetch`、`image_search`、`ls`、`read_file`、`glob`、`grep`、`write_file`、`str_replace`、`bash`
  - delegation：`task`
  - dynamic：`mcp__*`
- 将现有 `MockSearchTool` / `MockFetchTool` 作为 `web_search` / `web_fetch` 的兼容层或过渡实现。
- 对危险工具施加安全降级：
  - `bash`
  - `write_file`
  - `str_replace`
- 安全要求：
  - 默认受配置开关控制。
  - 默认受 `workspaceRoot` / `uploadsRoot` / `outputsRoot` 约束。
  - 不允许越界访问。
- MCP 当前可继续使用 `DisabledMcpClientAdapter`，但 catalog 和 prompt 必须能显示 `mcp__*` 是动态工具来源。

### Skills 迁移

- 将 `deer-flow/skills/public` 下 public skills 迁移到：
  - `src/main/resources/skills/public`
  - 并保留 `HAIFA_DEERFLOW_SKILLS` 覆盖能力
- 增强 `SkillParser` / `FileSystemSkillStorage` / `SlashSkillResolver`：
  - 支持 YAML front matter 的 `name`
  - `description`
  - `allowed-tools`
  - `references`
  - `templates`
  - `scripts`
  - `assets`
- 将 `SkillPromptRenderer` 升级为 DeerFlow 风格 `<skill_system>`：
  - 输出 `available_skills`
  - 展示 `name`、`description`、`source`、`location`
  - 说明 Progressive Loading Pattern
  - 说明 `/skill-name` 显式激活
  - 对 `custom/public` 标记可编辑性
- 显式 slash 激活时：
  - 本轮 prompt 注入完整 `SKILL.md`
- 普通场景：
  - 只注入 skill metadata，避免系统 prompt 过长
- `deep-research` 在 research mode 下自动激活。
- 用户显式 `/deep-research` 时也可激活。

### Frontend

- 确保 research 模式把 `researchOptions` 稳定发送到后端。
- `deep` depth 与后端 Deep Research prompt 严格对齐。
- 如有必要，增加轻量 Skills / Tools 可见性入口，但不做大规模 UI 重构。

### Tests

- Prompt builder 包含：
  - deep-research 关键段落
  - current date / temporal awareness
  - citations 规则
- Skill renderer 覆盖：
  - `available_skills`
  - slash activation
  - full content injection vs metadata-only
- Tool catalog 覆盖：
  - built-in / configured / MCP 三类
  - `glob`
  - `grep`
  - `image_search`
  - `task`
  - `mcp__*`
- research request 会把 `ResearchOptions` 带入 AgentLoop prompt。

### 验收标准

- research mode 使用正式 Java prompt template，而不是单纯拉长 YAML 文本。
- tool catalog 能明确区分 built-in / configured / MCP。
- public skills 可从资源目录加载，slash 激活与自动激活行为一致。
- deep-research 方法论进入运行时，而不是只保留在文档中。

## 9. Phase 4：Source / Evidence / Citation Core

### 目标

从“把网页内容塞进 prompt”升级为“来源可去重、证据可管理、结论可引用”。

### 前置依赖

依赖 Phase 3 完成的工具命名与 prompt 契约，尤其是：

- `web_search` / `web_fetch` 命名稳定
- research prompt 中已有 citation 和 temporal awareness 约束
- tool catalog 已能区分来源与能力边界

### Backend

- 新增包建议：

```text
org.wrj.haifa.ai.deerflow.research
  ResearchSource
  EvidenceItem
  SourceRegistry
  EvidenceStore
  CitationManager
  SourceQualityScorer

org.wrj.haifa.ai.deerflow.webcontent
  WebContentExtractor
  DedupService
  FetchPolicy
```

- 实现 `ResearchSource`：
  - `sourceId`
  - `title`
  - `url`
  - `domain`
  - `publishedAt`
  - `fetchedAt`
  - `sourceType`
  - `credibility`
  - `snippet`
  - `contentHash`
- 实现 `EvidenceItem`：
  - `evidenceId`
  - `sourceId`
  - `quoteOrParaphrase`
  - `claim`
  - `dimension`
  - `confidence`
  - `extractedAt`
- 第一版使用内存 store，保留后续数据库接口。
- `SourceRegistry`：
  - URL normalize
  - URL 去重
  - content hash 去重
  - domain / title / fetch time 记录
- `WebContentExtractor`：
  - title
  - main text
  - author / date
  - canonical URL
  - outgoing links
- `CitationManager`：
  - 生成 inline citation
  - 维护 Sources 列表
  - 只输出实际引用过的来源

### Frontend

- 增加 Source list panel 第一版：
  - title
  - domain
  - fetched status
  - citation count
- Evidence drawer 先做只读结构预览。

### Tests

- URL normalize / dedup 单元测试。
- 同一 URL 重复 fetch 不产生重复 source。
- citation 与 Sources 列表一致性测试。
- 长网页提取后不会把导航、脚本等噪声作为主内容。

### 验收标准

- 同一 URL 不会重复 fetch 多次。
- 每条关键 evidence 能追溯到 `sourceId`。
- 最终输出中的 Sources 只包含实际使用来源。
- 搜索结果里的重复转载不会占据多个 evidence slot。

## 10. Phase 5：Research Plan / Progress / Quality Gate

### 目标

把 deep-research skill 的方法论和 clarification 规则变成运行时约束：先计划、按维度推进、需要时澄清、最后做质量检查。

### Backend

- 新增或补齐：

```text
ResearchMode
ResearchOptions
ResearchPlan
ResearchTask
ResearchPlanMiddleware
ResearchQualityGate
ResearchCompletenessScore
AskClarificationTool
```

- `ResearchPlan` 字段：
  - topic
  - researchQuestions
  - dimensions
  - searchQueries
  - sourceCriteria
  - expectedDeliverable
- `ResearchTask` 字段：
  - id
  - title
  - dimension
  - status：`PENDING / IN_PROGRESS / COMPLETED / BLOCKED / FAILED`
  - evidenceIds
- Clarification gate：
  - 范围不清时先问问题
  - 时间窗口不清时使用 `researchOptions.timeWindow` 默认值
  - 输出格式不清时使用 `researchOptions.outputFormat`
  - 将 `ask_clarification` 从 catalog 能力变为可实际执行的用户澄清路径
- `ResearchQualityGate` 检查：
  - 是否覆盖至少 3 个角度
  - 是否读取足够数量的全文来源
  - 是否包含事实、数据、案例、观点、限制
  - 是否覆盖反方观点或挑战
  - 是否满足引用完整性

### Frontend

- 增加 `ResearchPlanView`。
- 展示每个维度的状态、来源数、证据数。
- 展示 quality gate pass / fail 和缺口说明。
- 当触发 `ask_clarification` 时，前端能用现有对话容器接住澄清问题并继续 run。

### Tests

- research run 没有 plan 时不能直接生成长报告。
- quality gate 不满足时会继续搜索或在结果中明确说明限制。
- clarification 触发后，用户回答能继续原 research thread。
- 用户修改研究计划后能更新任务状态。

### 验收标准

- 前端能看到研究计划、进度、已完成维度。
- Agent 至少执行 3 个不同角度的搜索。
- Agent 至少 fetch 5 个来源全文，除非用户选择 quick depth 或来源不足。
- 模型不能只搜索一次就生成“深度研究报告”。

## 11. Phase 6：Report / Artifact Delivery

### 目标

让深度研究输出成为可交付、可预览、可下载、可追踪的 artifact，并把 `present_files` 能力接到真正的研究产物交付链路中。

### Backend

- 新增包建议：

```text
org.wrj.haifa.ai.deerflow.artifact
  ArtifactRecord
  ArtifactService
  ArtifactController
  PresentFilesTool
  ReportWriterService
```

- 实现 `ReportWriterService`：
  - 输入 research plan、evidence store、citation manager、synthesis result
  - 输出 Markdown 报告
  - 保存到 outputs，例如 `research-<topic>-<YYYYMMDD>.md`
- 报告结构：
  - executive summary
  - methodology
  - key findings
  - dimensions
  - evidence table
  - limitations
  - sources
- 实现 `ArtifactRecord`：
  - artifactId
  - runId
  - threadId
  - path
  - filename
  - mimeType
  - size
  - createdAt
- API：
  - `GET /api/deerflow/artifacts`
  - `GET /api/deerflow/artifacts/{artifactId}`
  - `GET /api/deerflow/artifacts/{artifactId}/download`
- `present_files` 等价能力：
  - 将 outputs 下的报告登记为 artifact
  - 在模型可见层以 `present_files` 形式暴露研究交付物
  - 发送 `ARTIFACT_CREATED` 事件

### Frontend

- 增加 `ArtifactPanel`。
- 增加报告预览。
- 增加下载按钮。
- 最终聊天回答只展示摘要、限制和 artifact 链接。
- 如需要，在这里补上轻量 Tools / Skills 可见性入口，因为此时用户已经能感知研究产物。

### Tests

- 报告文件生成测试。
- artifact metadata 与文件一致性测试。
- 下载 API 测试。
- inline citations 与 Sources 一致性测试。
- `present_files` 只暴露已登记 artifact，不允许任意路径穿越。

### 验收标准

- 完整报告保存为 artifact。
- 前端可预览和下载报告。
- 最终聊天区不倾倒超长报告正文。
- 报告中的 inline citations 与 Sources 列表一致。
- `present_files` 与 artifact 目录一致，而不是独立悬空能力。

## 12. Phase 7：Context Compression / Tool Budget / Thread Memory

### 目标

在 MVP 闭环完成后，提升长任务稳定性与连续研究体验，支撑更多来源、更长上下文和跨 run 复用。

### 为什么合并

原来的 Phase 6 与 Phase 7 都属于 MVP 之后的增强阶段，并且都依赖：

- 研究报告与 artifact 已经存在
- source / evidence / citation 已稳定
- thread / run / message 关系已稳定

因此在排期上合并为一个阶段更利于控制迭代成本。

### Backend

- 实现 `SummarizationMiddleware`：
  - 按消息数、字符数、估算 token 数触发
  - 保留最近 N 条消息
  - 将旧消息压缩为 summary message
- 实现 `ResearchEvidenceCompressor`：
  - 将 fetched full text 压缩为 facts / claims / quotes / source metadata
  - 不把全文长期塞进上下文
- 实现 `ToolOutputBudgetMiddleware`：
  - 限制单次 tool output 大小
  - 对超长 fetch 内容先摘要再进入模型上下文
- 实现 `SourceAwareSummary`：
  - 摘要保留 `sourceId` / URL / citation anchor
- 实现 thread-level research memory：
  - previous research plans
  - source registry
  - evidence store summary
  - artifact list
  - user preferences
- Follow-up handling：
  - “继续扩展上一份报告”复用已有 plan / sources
  - “只补充某个章节”定位到对应 dimension
  - “用上次来源生成摘要 / 表格 / 新报告”复用 artifact 和 evidence
- Auto title：
  - 根据研究主题生成 thread title
- Follow-up suggestions：
  - 基于 quality gate 缺口生成后续问题

### Frontend

- 在 trace 中展示 compression / budget 事件。
- 当内容被压缩时，前端仍能查看 source metadata 和 artifact。
- 在 workspace 中展示当前 thread 的历史研究 artifact。
- 展示可复用 sources 和 previous plan。
- 支持用户从 artifact 或 source 发起 follow-up。

### Tests

- 10 个以上来源的 research run 不因上下文过长失败。
- 压缩后仍能正确引用来源。
- 超长网页不会让单次 tool result 超出预算。
- 同一 thread 的第二次 research 可复用已有 source，不重复 fetch。
- 用户要求补充某个章节时，不重跑全部维度。

### 验收标准

- `standard` depth 能稳定处理 10 个以上来源。
- `deep` depth 能在配置限制内继续运行并保留引用准确性。
- 用户能基于已有 evidence 继续工作，而不需要重复搜索已读取来源。
- Follow-up 结果能引用上一轮来源并生成新的 artifact 或答案。

## 13. Phase 8：Subagent 并行研究

### 目标

在 source / evidence / report / quality / thread memory 基础设施稳定后，引入并行研究能力，并将 `02-migration` 中的 `task` 工具、subagent prompt section 迁移完整接入。

### Backend

- 实现 `TaskTool`：
  - description
  - prompt
  - subagentType
  - allowed tools
  - timeout
  - max turns
- 将 DeerFlow `_build_subagent_section` 迁移为 Java prompt template 的 subagent section。
- 实现 `SubagentRuntime`：
  - 继承当前 thread / run 上下文
  - 可限制工具范围
  - 可限制模型、timeout、max turns
  - 输出结构化 `SubagentResult`
- 实现 `SubagentLimitMiddleware`：
  - 每轮最多 N 个并发 subagent
  - 超出则拒绝或排队
  - 记录 token usage 和失败原因
- `SubagentResult` 字段：
  - status
  - summary
  - evidenceIds
  - sourceIds
  - token usage
  - error
- 主 agent 综合：
  - 合并各 subagent evidence
  - 去重 sources
  - 统一通过 quality gate
  - 生成单一报告，而不是拼接多个子报告
- MCP 动态工具在此阶段前完成与 subagent 的兼容检查：
  - catalog 可见
  - prompt 可见
  - policy 可限制

### Frontend

- 展示 subagent started / completed / failed。
- 按 dimension 展示每个 subagent 的状态和来源数。
- 错误时展示降级结果：串行继续、跳过某维度或在限制中说明。

### Tests

- 一个 research run 可同时启动 2 到 3 个子任务。
- 并发限制生效。
- 子任务失败不会导致整个 run 崩溃。
- 主 agent 能综合子任务结果，并保留 citation 一致性。
- `task` 工具只暴露允许的子能力，不绕过主流程安全边界。

### 验收标准

- 复杂 research run 能并行执行多个独立维度。
- 主 agent 输出统一结论、统一 Sources、统一 artifact。
- Subagent 产生的 evidence 能被主流程质量门禁和报告写入使用。

## 14. MVP 定义

建议把 Phase 0.5 到 Phase 6 作为 Java DeerFlow Deep Research MVP。

MVP 必须满足：

- 用户可以选择 `research` 模式。
- 系统具备正式 thread / run / message 关系。
- research mode 使用正式 prompt template，而不是零散 research prompt 拼接。
- public skills 与 deep-research 默认技能可加载、可激活。
- tool catalog 能区分 built-in / configured / MCP。
- Agent 能自动生成研究计划。
- Agent 能执行至少 3 个不同角度的搜索。
- Agent 能 fetch 至少 5 个来源全文。
- 系统记录 source registry。
- 最终答案包含 inline citations 和 Sources。
- 完整报告保存为 artifact。
- 前端显示 research plan、source count、report artifact。
- 失败时日志能看到 runId、研究阶段、工具、来源 URL、异常堆栈。

Phase 7 到 Phase 8 属于增强阶段：

- Phase 7 解决规模、稳定性和连续研究体验。
- Phase 8 解决复杂主题的并行效率。

## 15. 依赖关系

```text
Phase 0 Baseline
  -> Phase 0.5 Thread / Run / Message
  -> Phase 1 Research Mode API / Events
  -> Phase 2 AgentLoop
  -> Phase 3 Prompt / Tool / Skill Migration Foundation
  -> Phase 4 Source / Evidence / Citation
  -> Phase 5 Plan / Progress / QualityGate
  -> Phase 6 Report / Artifact
  -> Phase 7 Compression / Budget / Thread Memory
  -> Phase 8 Subagent Parallelism
```

关键依赖说明：

- Phase 3 依赖 Phase 2 的标准 tool loop，否则 prompt / tool / skill 迁移无法落到实际运行链路。
- Phase 4 依赖 Phase 3 的稳定工具命名、citation 契约和 deep-research 方法论。
- Phase 5 依赖 Phase 4 的 evidence / citation，否则 quality gate 无法可靠评分。
- Phase 6 依赖 Phase 4 和 Phase 5，否则报告缺少可追溯证据和研究过程说明。
- Phase 7 依赖 Phase 6 的 artifact 闭环，否则 thread memory 和 follow-up 缺少稳定复用对象。
- Phase 8 必须放在 source / evidence / quality / report / memory 稳定后，否则并行只会放大无结构文本问题。

## 16. 推荐落地节奏

### Iteration A：Research 入口可用

覆盖 Phase 0 到 Phase 1，其中 Phase 0.5 作为 Phase 1 前置基础。

交付结果：

- Thread / run / message 基础模型
- Threads API
- Research mode toggle
- Research options
- Research event schema
- 当前 chat / tool 行为回归基线

### Iteration B：可迭代搜索

覆盖 Phase 2。

交付结果：

- Agent loop
- `web_search -> web_fetch -> follow-up search`
- 工具调用 trace

### Iteration C：迁移底座落地

覆盖 Phase 3。

交付结果：

- Java prompt template
- deep-research 默认激活
- Tool 三层建模
- Skill system 迁移
- `ResearchOptions` 入 prompt

### Iteration D：证据可追踪

覆盖 Phase 4。

交付结果：

- Source registry
- Evidence store
- Citation manager
- Source list panel

### Iteration E：研究过程可控

覆盖 Phase 5。

交付结果：

- Research plan
- Todo / progress
- clarification gate
- quality gate

### Iteration F：报告可交付

覆盖 Phase 6。

交付结果：

- Markdown report
- Artifact API
- Preview / download
- `present_files`
- Deep Research MVP 完整闭环

### Iteration G：长任务与连续研究

覆盖 Phase 7。

交付结果：

- Summarization
- Evidence compression
- Tool output budget
- Thread research memory
- Follow-up reuse

### Iteration H：并行研究效率

覆盖 Phase 8。

交付结果：

- `TaskTool`
- Subagent runtime
- 并发限制
- 主 agent 综合报告

## 17. 风险与控制点

| 风险 | 影响 | 控制方式 |
| --- | --- | --- |
| Prompt / Tool / Skill 迁移过晚 | Phase 4 到 Phase 8 反复返工 | 将 `02-migration` 基础部分整体前置到 Phase 3 |
| Tool 命名与 DeerFlow 不一致 | prompt、catalog、tests、frontend 表达混乱 | 在 Phase 3 统一 provider-visible tool names |
| Skill 全量注入导致 prompt 爆炸 | 上下文溢出、模型不稳定 | 只在 slash 激活时注入全文，默认只注入 metadata |
| 引用与来源不一致 | 报告可信度下降 | CitationManager 统一生成 inline citation 和 Sources |
| 网页内容过长或噪声大 | 上下文污染、模型遗漏重点 | WebContentExtractor、ToolOutputBudget、EvidenceCompressor |
| Quality gate 过严 | 研究任务无法结束 | depth 分级、允许带限制说明交付 |
| 过早引入 subagent | 并行产生无结构文本 | Phase 8 后置，依赖 evidence / report / quality / memory 基础设施 |
| Artifact 路径安全 | 文件泄露或路径穿越 | ArtifactService 只暴露登记文件，下载接口做路径校验 |

## 18. 完成后的能力边界

完成 Phase 1 到 Phase 6 后，Java DeerFlow 应从“搜索增强聊天”升级为“最小可用深度研究 agent”，并完成 Prompt、Tool、Skill 三大体系的核心迁移。

完成 Phase 7 到 Phase 8 后，Java DeerFlow 应具备更接近 DeerFlow Python 的长任务研究能力：

- 能处理更多来源。
- 能跨 run 复用研究记忆。
- 能按维度并行研究。
- 能输出可追踪、可下载、可继续扩展的研究产物。
