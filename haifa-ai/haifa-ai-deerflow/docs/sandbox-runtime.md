# Sandbox runtime 与 Local Trusted 开发模式

DeerFlow 的 Skill 仍然只是指令、脚本、引用和资源包。图片、图表等 Skill 统一使用 `read_file`、`write_file`、`run_script`、`bash` 和 `present_files`；本实现没有为任何 Skill 新增专用 Tool。

## 三种 backend

| backend | 环境与工具链 | 隔离边界 | 适用范围 |
| --- | --- | --- | --- |
| `local-restricted` | 清空子进程环境后重建 OS 必需变量、受控 PATH、UTF-8 和显式 `sandbox.environment` | 宿主进程，不是强隔离 | 受控的本地开发与兼容模式 |
| `local-trusted` | 继承宿主普通环境和 PATH，再过滤凭据；可选加载 profile snapshot | 宿主进程，不是安全 Sandbox | 可信、单用户开发环境 |
| `docker` | 由镜像提供解释器、字体和依赖，只显式注入环境 | 容器级进程和文件隔离 | 不可信输入和生产推荐模式 |

旧值 `local` 只兼容映射到 `local-restricted`。空值也映射到 `local-restricted`；其他未知值会启动失败，不再静默回退。

## Local Trusted 配置

`local-trusted` 必须同时满足 backend 选择和两个显式授权开关：

```yaml
haifa:
  ai:
    deerflow:
      sandbox:
        enabled: true
        backend: local-trusted
        allow-host-execution: true
        allowed-script-languages: python,python3,powershell,node,bash
        local-trusted:
          enabled: true
          inherit-environment: true
          inherit-path: true
          load-user-profile: false
          isolate-home: false
          passthrough-environment-names:
            - HTTP_PROXY
            - HTTPS_PROXY
```

缺少任一授权时配置校验会 fail closed。该模式会直接执行宿主命令，也可能读取用户级 npm、Git、Maven 等配置；denylist 不能替代容器隔离，因此不得用于多租户或不可信任务。

`load-user-profile` 默认为 `false`。启用后使用真实 Bash（Windows 为 Git Bash）获取一次 exported environment snapshot，并缓存到本次应用生命周期。`shell-init-files` 可显式列出要 source 的文件。Windows 不会把 `cmd.exe` 或 `System32\\bash.exe` 冒充 Bash。

## Runtime capability

`run_script` 按以下顺序解析 Node、Python、PowerShell 和 Bash：

1. `sandbox.executables.<language>` 的绝对路径；
2. 当前 backend 的最终有效 PATH；
3. Windows 的合法 `PATHEXT`，以及 Git for Windows 的常见 Bash 路径；
4. 文件、可执行权限和低风险 `--version` probe。

本地执行最终使用解析并缓存的绝对 executable，不再把裸 `node`、`python` 或 `bash` 留给嵌套 shell 偶然解析。例如：

```yaml
sandbox:
  executables:
    node: ${HAIFA_DEERFLOW_NODE_EXECUTABLE:}
    python: ${HAIFA_DEERFLOW_PYTHON_EXECUTABLE:}
    bash: ${HAIFA_DEERFLOW_BASH_EXECUTABLE:}
```

`GET /api/deerflow/health` 的 `sandbox` 字段显示 backend、总体状态和各 allowed language 的 `UP`/`DOWN`，但不暴露完整 PATH 或绝对宿主路径。缺少 runtime 时 `run_script` 快速返回 `FAILED`，并指出相应配置键。

## 环境过滤与脱敏

Local Trusted 从 `System.getenv()` 开始构造环境，然后：

1. 删除 OpenAI、Gemini、MiniMax、GitHub、云凭据、数据库密码等精确敏感变量；
2. 删除匹配 `*_API_KEY`、`*_TOKEN`、`*_SECRET`、`*_PASSWORD`、`*_PRIVATE_KEY`、`*_CREDENTIALS`、`*_ACCESS_KEY` 等模式的变量；
3. 根据 `passthrough-environment-names` 恢复运维显式授权的宿主变量；内部 JWT、session、加密和管理密钥仍不可普通 passthrough；
4. 叠加非空 `sandbox.environment`，这是给 Sandbox 显式注入 provider key 的受控通道；
5. 设置 UTF-8、HOME 策略、虚拟目录变量和 Runtime PATH。

显式注入的敏感值会在 stdout、stderr、异常和 ToolResult metadata 进入持久化/SSE 前替换为 `[REDACTED]`。Docker 的 `docker run` 参数只携带环境变量名，实际值通过 Docker CLI 子进程环境传递，避免密钥出现在进程命令行。

## 旧配置迁移

`run-script-local-unsafe-allowed=true` 暂时只为 `local`/`local-restricted` 的 `run_script` 保持兼容，并在启动时记录一次 deprecation warning。迁移为：

```yaml
sandbox:
  backend: local-restricted
  allow-host-execution: true
  run-script-local-unsafe-allowed: false
```

`bash` 和 `run_script` 现在共用 `allow-host-execution` 授权。生产环境应改用 `backend: docker`，并在镜像中显式安装 Node、Python 依赖与 `fonts-noto-cjk`。

## chart-visualization 调用链

AntV client 是 Skill 脚本，不是 Tool。使用 `run_script(language=node)` 时，应在该 Node 进程中直接 `require(.../scripts/generate.js)` 并调用导出的 `main(process.argv.slice(2))`；不要再通过 `execSync("node ...")` 启动第二个 Node。

`generate_bar_chart` 在联网前验证每个数据项的 `category` 字符串与有限数值 `value`，`label/value` 会直接失败。脚本只有在远端响应有效、下载得到非空图片且目标位于 outputs 后才算生成完成，随后仍必须调用 `present_files` 才能宣称已交付。
