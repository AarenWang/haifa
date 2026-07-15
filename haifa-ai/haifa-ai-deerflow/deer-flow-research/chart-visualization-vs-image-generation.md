# chart-visualization 与 image-generation Skill 研发分析

> 调研日期：2026-07-15。本文分析当前 Haifa DeerFlow 实现；Skill 名称是 `chart-visualization`，不是 `chat-visualization`。

## 结论

两个 Skill 都不应注册同名专用 Tool。Skill 是“给 Agent 的能力说明、执行流程和可复用资源”，实际执行复用 DeerFlow 的通用 `bash` / `run_script` 能力。只有必须由宿主统一治理、鉴权、审计、限流或交互确认的独立外部能力，才值得提升为专用 Tool；不能因为有一个 Skill，就机械地创建一个 Tool 实现类。

`chart-visualization` 解决“数值和文字必须准确”的确定性渲染问题；`image-generation` 解决“视觉语义和风格优先”的生成式图片问题。图表、表格、流程数据和中文标签不能交给图片模型碰运气；创意人物、场景、产品图和装饰背景也不适合用图表渲染器实现。

## 功能与实现对比

| 维度 | chart-visualization | image-generation |
| --- | --- | --- |
| 核心目标 | 精确呈现数据、标签、图例、坐标轴和 CJK 文本 | 生成角色、场景、产品、插画等创意位图 |
| 确定性 | 高；相同输入应得到语义一致的图表 | 低；同一提示词允许产生不同视觉结果 |
| 输入 | 图表操作名与结构化 `args`，或本地 dashboard JSON | 结构化提示词 JSON、可选参考图片、宽高比 |
| 执行入口 | `scripts/generate.js` 或 `scripts/render_bar_dashboard.py` | `scripts/generate.py` |
| 外部依赖 | AntV HTTP 渲染端点；本地 dashboard 只依赖 Pillow 与 CJK 字体 | Gemini 或 MiniMax HTTP API；Python `requests`，参考图校验依赖 Pillow |
| 输出 | 远端图表 URL 下载为本地图片，或 Pillow 直接写 PNG | Provider 返回 base64，脚本解码后直接写本地图片 |
| Provider 选择 | `VIS_REQUEST_SERVER` 切换兼容 AntV 协议端点 | `IMAGE_GENERATION_PROVIDER=gemini|minimax`，也可按凭据自动选择 |
| 中文策略 | 必须显式选用并验证 Microsoft YaHei、SimHei、Noto Sans CJK 等字体 | 不承诺模型能准确生成中文；需要准确文本时二次确定性叠字 |
| 失败语义 | JSON、操作名、HTTP、API、下载、空文件任一步失败都必须非零退出 | 凭据、Provider、API、prompt 长度、base64、文件校验失败都非零退出 |
| DeerFlow 交付 | 输出必须落到 `/mnt/user-data/outputs/`，验证后调用 `present_files` | 同左 |

## chart-visualization 调用链

1. Agent 根据 `SKILL.md` 和 `references/` 选择图表类型并构造 JSON。
2. Agent 通过通用 `bash` 调用 `generate.js`，JSON 中的 `tool` 只是脚本内部操作标识，不是 DeerFlow Tool。
3. `generate.js` 将操作标识映射为 AntV `type`，POST 到 `VIS_REQUEST_SERVER`。
4. 脚本校验 `success/resultObj`，从结果中提取图片 URL，再下载到 `--output-file`。
5. 只有本地文件存在且非空，Agent 才调用 `present_files` 并回复成功。

对多面板柱状对比图，Agent 可改用 `render_bar_dashboard.py`。这个路径完全本地化，使用 Pillow 绘制，并检测 CJK 字体是否真的包含中文 glyph；它避免 Matplotlib/Seaborn 缺包，也避免“安装了绘图库但没有中文字形”造成乱码。

## image-generation 调用链

1. Agent 将创意需求整理成英文结构化提示词 JSON。
2. Agent 通过通用 `bash` 调用 `generate.py`，传入 prompt、参考图、输出路径和宽高比。
3. 脚本根据 `IMAGE_GENERATION_PROVIDER` 和凭据选择 Gemini 或 MiniMax。
4. Gemini 接收完整结构化 JSON；MiniMax `image-01` 只使用合并后的 `prompt` 字段，并执行 1500 字符上限校验。
5. Provider 返回 base64 图片，脚本解码、写入并验证非空文件；失败时非零退出。

这也是“Skill 不等于 Tool”的直接例子：Provider 适配、参数校验和文件落盘是 Skill 自带脚本的确定性实现，DeerFlow 只需提供受控的通用脚本执行能力。若未来需要平台级密钥托管、逐租户计费、人工确认或统一内容安全审核，可以新增一个通用的“媒体生成 Provider Tool”，但仍不应为每个 Skill 创建同名 Tool。

## chart-visualization 发现的缺口与修复

本次修复前存在以下问题：

- `generate.js` 已存在，但 `SKILL.md` 反而要求 Agent 不要声称它存在，执行指导与代码相互矛盾。
- 单项缺少 `tool`、操作名未知或远端 API 失败时，脚本只写 stderr 后继续，最终可能以退出码 0 结束，导致上层把 `TOOL_FAILED` 误报成成功。
- 远端仅返回 URL，没有下载到 DeerFlow 输出目录，不能形成可验证、可交付的 Artifact。
- 映射表遗漏官方文档已列出的 `spreadsheet`。
- 请求仍使用旧的 `chart-visualization-creator` source 标记，与当前上游文档的 `chart-visualization-skills` 不一致。
- 缺少请求超时、非 JSON 响应、非图片响应和空文件校验。

修复后的边界是：一次 `--output-file` 调用只生成一个图；所有错误快速失败并返回非零退出码；远端图片必须下载落盘并校验；两份 Skill 资源副本保持一致；本地 dashboard、CJK 字体规则和生成式图片边界继续保留。

## AntV 默认服务与替代方案

AntV 当前官方 Skill 仍公开写明 `POST https://antv-studio.alipay.com/api/gpt-vis`，请求包含 `type`、`source: "chart-visualization-skills"` 和图表参数，响应为 `success/resultObj`；其支持列表也包含 `spreadsheet`。来源：[AntV chart-visualization-skills](https://github.com/antvis/chart-visualization-skills/blob/master/skills/chart-visualization/SKILL.md)。2026-07-15 的最小柱状图探测返回 HTTP 200 和可下载的 `resultObj` URL，因此它当前可用于研发测试。

但 AntV 的公开 Skill 没有同时给出该端点的生产 SLA、限流、鉴权、数据保留和兼容性承诺。由此应把它定位为“默认便利服务”，而不是无条件的生产基础设施。敏感数据不应在没有数据处理协议的情况下发送；生产调用还应有超时、失败告警、熔断与备用渲染路径。

公开可替代服务中，QuickChart 提供 `https://quickchart.io/chart` 的 GET/POST 图片渲染接口，官方文档说明它接收 Chart.js 配置并直接返回 PNG、SVG 或 WebP：[QuickChart POST API](https://quickchart.io/documentation/usage/post-endpoint/)。其开源服务也支持 Docker 自建：[QuickChart GitHub](https://github.com/typpo/quickchart#docker)。但它不是 AntV `gpt-vis` 协议的 drop-in URL：输入 schema、图表类型和返回格式都不同，必须新增 adapter；地图、组织图、鱼骨图、思维导图和 spreadsheet 等能力也不能假设完全等价。

因此不一定必须自己搭建：

- 本地开发、演示和低风险任务：可以继续使用 AntV 默认端点，并保留本地落盘校验。
- 能接受 Chart.js 能力范围：可以接入 QuickChart 托管服务，但要实现明确的协议转换 adapter，不能只替换 `VIS_REQUEST_SERVER`。
- 需要敏感数据、稳定 SLA、离线部署或精确保持当前 AntV schema：应自建兼容渲染服务，或把核心图表迁移为本地 AntV G2/其他确定性渲染器；这时 `VIS_REQUEST_SERVER` 指向自有 adapter。

从第一性原理看，关键不是“有没有公开 URL”，而是谁对渲染正确性、可用性、数据边界和交付文件负责。Skill 负责告诉 Agent 如何调用；脚本负责适配和验证；服务负责渲染；DeerFlow Tool 层只负责通用执行与平台治理。
