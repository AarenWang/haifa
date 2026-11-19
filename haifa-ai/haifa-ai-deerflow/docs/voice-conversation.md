# 语音对话配置与运行

当前语音链路采用浏览器推流和服务端编排：浏览器采集麦克风并重采样为
`PCM S16LE / 16 kHz / 单声道`，后端完成流式 ASR、Agent 文本生成和流式 TTS，
再把 `PCM S16LE` 音频按顺序推回浏览器播放。ASR 与 TTS 可选择不同厂商。

## 开发模式

默认 `VOICE_ASR_PROVIDER=fake`、`VOICE_TTS_PROVIDER=fake`，用于本地协议和界面联调，
不会调用云服务，也不会产生可听的真实合成语音。后端 API Key 只允许通过环境变量或
密钥管理服务注入，不能写入前端代码或提交到仓库。

## 阿里云百炼（推荐起步配置）

在百炼控制台创建与地域一致的 API Key，取得业务空间 Workspace ID。以下 PowerShell
示例使用 Qwen 实时识别和 CosyVoice 流式合成：

```powershell
$env:VOICE_ASR_PROVIDER = "dashscope"
$env:VOICE_TTS_PROVIDER = "dashscope"
$env:DASHSCOPE_ASR_ENABLED = "true"
$env:DASHSCOPE_TTS_ENABLED = "true"
$env:DASHSCOPE_API_KEY = "sk-your-api-key"
$env:DASHSCOPE_WORKSPACE_ID = "your-workspace-id"
$env:DASHSCOPE_REGION = "cn-beijing"
$env:DASHSCOPE_ASR_MODEL = "qwen3-asr-flash-realtime"
$env:DASHSCOPE_TTS_MODEL = "cosyvoice-v3-flash"
$env:DASHSCOPE_TTS_VOICE = "longanyang"
$env:DASHSCOPE_TTS_SAMPLE_RATE = "24000"
```

`DASHSCOPE_REGION` 支持 `cn-beijing` 和 `ap-southeast-1`。API Key、Workspace ID、模型
和音色必须属于同一地域；音色必须被所选 TTS 模型支持。也可以用
`DASHSCOPE_ASR_ENDPOINT` / `DASHSCOPE_TTS_ENDPOINT` 覆盖自动生成的业务空间专属地址。
TTS 适配器使用 Qwen-Audio-TTS/CosyVoice WebSocket 协议，因此也可换成控制台中与该
协议、地域和音色兼容的模型 ID。

## 火山引擎

### ASR

当前 ASR 适配器连接边缘大模型网关 Realtime API。需要先在边缘大模型网关开通预置
Doubao ASR 或部署自己的 ASR 模型，并取得网关 API Key：

```powershell
$env:VOICE_ASR_PROVIDER = "volcengine"
$env:VOLCENGINE_ASR_ENABLED = "true"
$env:VOLCENGINE_VOICE_API_KEY = "your-edge-gateway-api-key"
$env:VOLCENGINE_ASR_ENDPOINT = "wss://ai-gateway.vei.volces.com/v1/realtime"
$env:VOLCENGINE_ASR_MODEL = "bigmodel"
# 仅当网关实例要求时配置：
$env:VOLCENGINE_ASR_RESOURCE_ID = "your-resource-id"
```

`bigmodel` 是平台预置 Doubao ASR 的查询模型名；自部署模型应改为实际部署名称。

### TTS

当前 TTS 适配器使用火山语音合成 V1 WebSocket 二进制协议，并把 Agent 的文本分块按
顺序合成。需从语音技术控制台取得 App ID、Access Token 和音色 `voice_type`：

```powershell
$env:VOICE_TTS_PROVIDER = "volcengine"
$env:VOLCENGINE_TTS_ENABLED = "true"
$env:VOLCENGINE_TTS_APP_ID = "your-app-id"
$env:VOLCENGINE_TTS_ACCESS_TOKEN = "your-access-token"
$env:VOLCENGINE_TTS_CLUSTER = "volcano_tts"
$env:VOLCENGINE_TTS_VOICE = "your-voice-type"
$env:VOLCENGINE_TTS_SAMPLE_RATE = "24000"
```

火山已提供更新的语音合成接口；本项目当前保留 V1 兼容适配器。若新账号只开通了 V3
资源或需要双向流式输入、复刻音色等新能力，应新增 V3 provider，不要把 V3 endpoint
直接填入 `VOLCENGINE_TTS_ENDPOINT`。

## 混合厂商

两个 provider 独立选择。例如“火山 ASR + 百炼 TTS”只需设置：

```powershell
$env:VOICE_ASR_PROVIDER = "volcengine"
$env:VOICE_TTS_PROVIDER = "dashscope"
```

并同时配置对应两组凭证。若指定 provider 未启用或凭证不完整，后端会明确返回
`ASR_UNAVAILABLE` / `TTS_UNAVAILABLE`，不会静默退回 fake provider。

## 启动与验证

```powershell
mvn -pl haifa-ai/haifa-ai-deerflow -am spring-boot:run
```

另一个终端启动前端：

```powershell
Set-Location haifa-ai/haifa-ai-deerflow-web
npm run dev
```

浏览器必须运行在 `localhost` 或 HTTPS 安全上下文中才能使用麦克风。开发代理已经开启
WebSocket 转发。生产部署时还应由可信认证层设置用户身份，不应把查询参数 `userId`
当作强身份认证。

## 常见问题

- 连接后一直没有 `asr.ready`：检查 API Key、Workspace/网关模型、地域和 endpoint。
- TTS 返回音色错误：模型与音色不兼容，去对应控制台复制该模型的音色 ID。
- 播放速度或音高异常：确认 provider 实际输出采样率与 `*_TTS_SAMPLE_RATE` 一致。
- 浏览器没有麦克风权限：检查安全上下文、系统权限和浏览器站点权限。
- 生产环境断连：反向代理必须允许 WebSocket Upgrade，并将空闲超时设为大于 90 秒。
