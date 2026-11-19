# Java DeerFlow 运行时观测防幻觉与命令策略加固开发提示词

## 0. 任务目标

目标仓库：`D:\workspace\haifa`

目标模块：`haifa-ai/haifa-ai-deerflow`

本任务同时治理两类相互放大的问题：

1. 沙箱命令策略使用普通子串匹配，导致 `secureboot` 被 `reboot` 拦截、`NoTypeInformation` 被 `format` 拦截；
2. 本地数据采集失败后，模型仍生成进程名、指标、排行榜和图表，并宣称来自实时观测。

最终目标是建立以下不可绕过的不变量：

```text
FAILED / DENIED Tool result = 零观测数据
本地实时数字声明 => 当前 run 内必须存在成功、非渲染型、包含数据载荷的 Tool 结果
图表生成成功 != 图表数据真实
Skill 名称 != Tool 名称
```

## 1. 运行时事实约束

### 1.1 禁止无证据补全

模型不得编造或补全：

- 当前进程名、应用名、服务名；
- CPU、内存、I/O、电量、功耗、续航、温度等本地指标；
- 排名、Top N、百分比、误差范围、采样时间；
- 文件、图表或报告的数据内容。

示例值、应用名映射、历史 Assistant 文本和绘图脚本中的常量均不属于观测证据。

### 1.2 Tool 状态语义

- `SUCCESS` 且输出含非空数据载荷：可作为候选证据；
- `FAILED`、`DENIED`、`SKIPPED`、`NOT_FOUND`、超时、非零退出码：不得产生任何事实声明；
- 仅输出“文件已保存”“图表已生成”的渲染 Tool：只能证明交付成功，不能证明源数据真实；
- 当前 run 没有测量证据时，必须重新采集或明确说明无法获得数据。

### 1.3 最终答案证据门禁

`ChatFinalAnswerGateNode` 在每次准备输出最终答案时都执行，但不得通过答案关键词、工具名称猜测或载荷文本形状决定是否放行。门禁只校验本次 run 已登记的结构化 `CompletionRequirement`，没有相关 Requirement 的普通知识问答可以直接通过。

必须支持以下 Requirement：

- `LOCAL_OBSERVATION`：本机、设备、进程、CPU、内存、电量、功耗等当前观测；
- `DERIVED_DATA`：基于观测数据进行统计、排名、聚合或预测；
- `ARTIFACT_DELIVERY`：生成并交付图表、报告或其他文件；
- `WEB_CITATION`：基于网页搜索结果作出现状声明；
- `FILE_MUTATION`：声明已经修改代码或配置；
- `COMMAND_EXECUTION`：声明某条命令已经成功执行；
- `USER_PROVIDED_DATA`：仅基于用户明确提供的数据分析，不得冒充本机实时观测。

Requirement 采用双层触发：

1. 请求入口或规划阶段根据结构化意图登记 Requirement；
2. 实际工具行为自动追加 Requirement，避免规划遗漏。例如本机采集工具追加 `LOCAL_OBSERVATION`，绘图行为追加 `ARTIFACT_DELIVERY`。

规划阶段通过 `declare_completion_requirements` Tool 登记结构化 Requirement。该 Tool 只能增加约束，不得生成任何 Evidence。受信任 Tool 通过代码级 `ToolCompletionContract` 声明其 Requirement 与成功 Evidence 类型；失败调用仍登记 Requirement，但不生成 Evidence。

通用 `run_script` 和 `bash` 成功最多生成 `COMMAND_RESULT`，不能因为 stdout 看起来像 JSON 或包含数字就升级为 `MEASUREMENT`。仅允许受信任的领域适配器根据已验证的命令级规则追加动态 contract。例如直接执行并在 stdout 返回系统状态的 `powercfg /getactivescheme`、`/query`、`/requests` 等查询可生成 `MEASUREMENT`；仅生成报告文件的 `/batteryreport`、`/energy`、`/srumutil` 不得仅凭命令成功生成测量 Evidence。

Tool 执行成功后由受信任运行时生成 `EvidenceRecord`，模型不得自行声明证据类型。至少记录：

```text
evidenceId、evidenceType、sourceToolCallId、payloadRef/payloadHash、observedAt、attributes
```

派生数据必须记录父 Evidence ID 与计算血缘；图表必须绑定其数据集 Evidence ID。`FAILED`、`DENIED`、超时、非零退出码不得产生 Evidence。`ARTIFACT` Evidence 不能满足 `LOCAL_OBSERVATION`。

典型放行链如下：

```text
LOCAL_OBSERVATION -> MEASUREMENT Evidence
DERIVED_DATA      -> DERIVED_DATASET Evidence（包含父证据）
ARTIFACT_DELIVERY -> ARTIFACT Evidence（绑定数据集证据）
```

门禁最终应退化为通用集合判定：所有 Requirement 均能由当前 run 的 EvidenceLedger 满足才放行。未满足时向模型返回缺失的 Requirement；达到最大步数仍无证据时，只能返回明确的未完成说明，不得保留无证据数字。

禁止继续向 `ChatFinalAnswerGateNode` 增加以下 case-by-case 规则：

- 本地观测关键词数组；
- 绘图或变更工具名称黑名单；
- 通过 JSON 开头、换行、数字等文本特征猜测数据载荷；
- 针对单一业务或命令的特殊判断。

`ChatFinalAnswerGateNode` 只负责调用 `CompletionPolicyEngine` 并处理“放行、继续、终止”结果；Requirement 生成、Evidence 生成和血缘校验分别属于独立组件。

## 2. 命令策略

### 2.1 全局禁止管道符

命令行和脚本正文中出现任意 `|` 字符都必须拒绝，包括引号、注释、正则表达式和字符串中的 `|`。错误信息应明确指出 `pipe operator is disabled`。

### 2.2 黑名单必须按命令或 Token 序列匹配

禁止使用 `String.contains` 扫描普通子串。最低要求：

- 对脚本去除字符串和注释后执行词法 Token 化；
- 单 Token 规则只匹配完整 Token；
- 多 Token 规则按连续 Token 序列匹配；
- 命令名大小写不敏感；
- `secureboot` 不得匹配 `reboot`；
- `NoTypeInformation`、`Format-Table` 不得匹配 `format`；
- `rm -rf`、`del /s`、`Remove-Item -Recurse` 仍必须被拦截。

PowerShell AST 可作为后续增强；当前 Java 侧必须至少实现命令级/Token 边界匹配，并 fail closed。

### 2.3 白名单加固

- 白名单只接受规范化后的裸命令名；
- 拒绝 `./tool`、`.\\tool.exe`、绝对路径等路径限定可执行文件，避免同名恶意程序绕过；
- Windows 可执行扩展名 `.exe`、`.cmd`、`.bat`、`.com` 统一规范化；
- 黑名单优先于白名单；
- 生产配置不得依赖空白名单表示“全部允许”；
- 配置值必须去空格、去空项、大小写归一化并在启动或测试阶段验证。

## 3. Skill 与图表数据血缘

- `chart-visualization` 是 Skill，不得作为 Tool 调用；
- Skill 必须先激活并加载其 `SKILL.md`，再调用其中声明的通用 `run_script`/`bash`；
- 绘图输入不得由模型重新手写数字；
- 后续完整实现应让图表输入绑定 `sourceToolCallId` 和数据摘要哈希；
- 当前阶段至少要求同一 run 内存在成功数据观测，且渲染结果不能充当观测结果。

## 4. 指标命名

- `Get-Process` 的累计 CPU 时间、内存和 I/O 代理值不得称为真实电量、瓦特或焦耳；
- 未接入 SRUM、ETW/E3 或硬件能量计量时，只能称为“资源活动指数”；
- 不得声称未经验证的误差范围或“业界公认权重”；
- 代理指标必须声明采样窗口、计算公式和限制。

## 5. 必须补充的测试

1. `secureboot` 不触发 `reboot`；
2. `NoTypeInformation`、`Format-Table` 不触发 `format`；
3. `rm -rf`、`del /s`、`Remove-Item -Recurse` 继续被拒绝；
4. 命令或脚本任意位置出现 `|` 都被拒绝；
5. 路径限定的白名单可执行文件被拒绝；
6. 采集 Tool 失败后，包含本地数字的最终答案被拒绝；
7. 只有 `present_files` 或绘图脚本成功时，仍不得宣称实时数据；
8. 成功的非渲染型结构化采集结果允许通过。

## 6. 验收标准

```text
安全策略不再因普通子串产生上述误报；
本地实时数字无法在没有当前 run 成功观测的情况下进入最终答案；
渲染成功与数据真实性在状态和门禁中明确分离；
所有相关单元测试通过；
git diff --check 通过。
```
