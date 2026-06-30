# Haifa-AI-Deerflow 运行体验指南

## 环境准备

- **JDK 17+**（建议 21 或 25）
- **Maven 3.9+**
- **可选**：OpenAI API Key（`sk-...`）用于真实模型调用；无 Key 时可使用 `MockAgentModelClient` 测试本地流程

## 编译

在仓库根目录执行：

```bash
# 1. 编译并安装 parent 及依赖模块
cd /d/workspace/haifa
mvn -pl haifa-ai/haifa-ai-deerflow -am -Popenai clean install -DskipTests

# 2. 运行测试（首次建议加上，确认全部通过）
mvn -pl haifa-ai/haifa-ai-deerflow -am -Popenai test
```

> 说明：`-Popenai` 激活 `spring-ai-starter-model-openai` 依赖，否则 Spring AI `ChatClient` 无法注入。

## 创建技能目录（体验 Phase 4 核心）

默认 skills root 是当前工作目录下的 `skills/`。

在启动服务前，先创建技能文件：

```bash
# Windows CMD / PowerShell
mkdir "%CD%\skills\public\research"
mkdir "%CD%\skills\public\writer"
echo # Research Assistant > "%CD%\skills\public\research\SKILL.md"
echo. >> "%CD%\skills\public\research\SKILL.md"
echo Help the user perform research tasks. >> "%CD%\skills\public\research\SKILL.md"
echo. >> "%CD%\skills\public\research\SKILL.md"
echo ## Allowed tools >> "%CD%\skills\public\research\SKILL.md"
echo - web_search >> "%CD%\skills\public\research\SKILL.md"
echo - web_fetch >> "%CD%\skills\public\research\SKILL.md"
echo - summarize >> "%CD%\skills\public\research\SKILL.md"

echo # Writer Assistant > "%CD%\skills\public\writer\SKILL.md"
echo. >> "%CD%\skills\public\writer\SKILL.md"
echo Help the user write and edit text. >> "%CD%\skills\public\writer\SKILL.md"
echo. >> "%CD%\skills\public\writer\SKILL.md"
echo - allowed-tools: proofread, expand_text >> "%CD%\skills\public\writer\SKILL.md"
```

**SKILL.md 格式说明**：
- 一级标题 `# Skill Name` → 会被解析为 description
- `## Allowed tools` 后面的 `- tool_name` 列表 → 被解析为 allowedTools
- 也可以内联写 `- allowed-tools: tool1, tool2`

## 启动服务

### 方式 A：使用真实 OpenAI 模型

```bash
cd /d/workspace/haifa
set OPENAI_API_KEY=sk-your-key
set OPENAI_BASE_URL=https://api.openai.com
mvn -pl haifa-ai/haifa-ai-deerflow -Popenai spring-boot:run
```

### 方式 B：无 API Key 测试本地流程（推荐初次体验）

如果暂时没有 OpenAI Key，可以临时用内置的 `MockAgentModelClient` 替换。在启动前添加环境变量：

```bash
set HAIFA_DEERFLOW_MODEL=gpt-4o-mini
mvn -pl haifa-ai/haifa-ai-deerflow -Popenai spring-boot:run
```

> 注意：如果没有配置 API Key，Spring AI 在注入 `ChatClient.Builder` 时会报错。建议先确保 `OPENAI_API_KEY` 已设置，或者自行添加一个 `MockAgentModelClient` Bean 并标记 `@Primary` 以覆盖默认的 Spring AI 客户端。

## 体验 API

服务启动后监听 `http://localhost:8095`。

### 1. 普通消息（不激活技能）

```bash
curl -N -X POST http://localhost:8095/api/deerflow/runs/stream \
  -H "Content-Type: application/json" \
  -d "{\"threadId\":\"t1\",\"message\":\"List workspace files\"}"
```

预期 SSE 事件流：
```
event: run_started
data: {"eventId":"1","runId":"...","type":"RUN_STARTED","content":"Run started"}

event: tool_started
data: {"eventId":"2","...","type":"TOOL_STARTED","content":"Executing list_workspace_files"}

event: tool_completed
data: {"eventId":"3","...","type":"TOOL_COMPLETED","content":"..."}

event: model_started
data: {"eventId":"4","...","type":"MODEL_STARTED","content":"Calling Spring AI model adapter"}

event: model_completed
data: {"eventId":"5","...","type":"MODEL_COMPLETED","content":"..."}

event: run_completed
data: {"eventId":"6","...","type":"RUN_COMPLETED","content":"Run completed"}
```

### 2. Slash Skill 激活（Phase 4 核心）

```bash
curl -N -X POST http://localhost:8095/api/deerflow/runs/stream \
  -H "Content-Type: application/json" \
  -d "{\"threadId\":\"t2\",\"message\":\"/research summarize this repo\"}"
```

如果查看最终模型 prompt（可以通过日志或断点），你会在 system prompt 中看到：
```
[Active skills]
- research: Research Assistant
  Allowed tools: web_search, web_fetch, summarize
```

### 3. 工具搜索（tool_search）

```bash
curl -N -X POST http://localhost:8095/api/deerflow/runs/stream \
  -H "Content-Type: application/json" \
  -d "{\"threadId\":\"t3\",\"message\":\"what tools do I have?\"}"
```

预期返回 `tool_search` 工具的结果，包含：
- `read_workspace_file`（builtin）
- `list_workspace_files`（builtin）
- `current_time`（builtin）
- `web_search`（skill:research，requires skill）
- `summarize`（skill:research，requires skill）
- `proofread`（skill:writer，requires skill）

### 4. 字符预算测试（TokenBudget）

修改 `application.yml`（或临时环境变量）：
```yaml
haifa:
  ai:
    deerflow:
      char-budget: 200
```

重启后发送长消息：
```bash
curl -N -X POST http://localhost:8095/api/deerflow/runs/stream \
  -H "Content-Type: application/json" \
  -d "{\"threadId\":\"t4\",\"message\":\"/research this is a very long message that will exceed the character budget when combined with tool observations and active skill sections\"}"
```

预期事件流：
```
event: run_started
event: model_completed   # "Budget exceeded..."
event: run_completed      # "Run completed with budget limit"
```
注意：**不会**看到 `model_started`，因为预算检查在真正调用模型前就已经拦截了。

### 5. 查询 Run 状态

```bash
curl http://localhost:8095/api/deerflow/runs/{runId}
```

## 配置开关体验

### 禁用 Skills
```bash
set HAIFA_DEERFLOW_SKILLS_ENABLED=false
mvn -pl haifa-ai/haifa-ai-deerflow -Popenai spring-boot:run
```
此时 `/research` 不会解析为 skill，prompt 中不会出现 `[Active skills]`。

### 禁用 Tool Search
```bash
set HAIFA_DEERFLOW_TOOLSEARCH_ENABLED=false
```
`tool_search` 工具不会响应任何查询。

### 禁用 MCP（默认已禁用）
```yaml
haifa:
  ai:
    deerflow:
      mcp-enabled: false
```
`McpToolProvider` 的 `isEnabled()` 返回 false，任何 MCP 调用都会抛 `IllegalStateException`。

## 调试技巧

1. **查看最终 Prompt**：在 `SimpleAgentRuntime.stream()` 中打断点，观察 `promptMono` 的值。
2. **查看技能解析**：`FileSystemSkillStorage` 会在启动时扫描目录，可在 `listSkillsIn` 方法中打断点。
3. **查看工具策略**：`executePlannedTools` 中 `toolPolicyService.isToolAllowed()` 的返回值决定是否执行工具。

## 常见问题

### Q: 启动报错 `ChatClient.Builder` not found
A: 确保使用了 `-Popenai` profile，且 `OPENAI_API_KEY` 已设置。如果没有 Key，需要自行写一个 `@Primary` 的 `AgentModelClient` Bean 替换 `SpringAiAgentModelClient`。

### Q: 技能目录不生效
A: 检查 `skills-root` 指向的绝对路径。默认是 `${user.dir}/skills`，在 IDE 中运行时需要确认 `user.dir` 是项目根目录。

### Q: 测试失败
A: 确保 JDK 17+，且 `mvn -pl haifa-ai/haifa-ai-deerflow -am -Popenai test` 在仓库根目录执行。

### Q: Budget 测试不触发
A: `char-budget` 统计的是最终 `systemPrompt + userPrompt` 的总长度（包含 skills section、tool XML、dynamic context）。如果设置 200 但注入 skills 后总长度不到 200，就不会触发。建议设置 50-100 做测试。
