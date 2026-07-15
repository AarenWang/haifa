# Skill/Tool runtime boundary

## Core rule

A Skill is a declarative instruction/resources package. It may contain `SKILL.md`, scripts, templates, and references, but it never creates or requires a per-skill Tool implementation. Tools are stable atomic capabilities with explicit schemas and execution status. Consequently, `tool_search` is backed only by the callable Tool catalog; skill metadata cannot synthesize deferred tools.

Media skills use the same pipeline as other executable skills:

`read_file -> write_file -> bash -> present_files`

The image-generation Skill is implemented by `skills/public/image-generation/scripts/generate.py`. There is deliberately no `ImageGenerationTool` or generic `SkillScriptTool`.

## Virtual filesystem contract

| Virtual root | Permission | Host source |
| --- | --- | --- |
| `/mnt/skills` | read/execute | `haifa.ai.deerflow.skills-root` |
| `/mnt/user-data/uploads` | read-only | `uploads-root` |
| `/mnt/user-data/workspace` | read/write/execute | `workspace-root` |
| `/mnt/user-data/outputs` | read/write | `outputs-root` |

Local execution rewrites these aliases to configured host roots and masks them in output. Docker binds the same roots with matching permissions. Host absolute paths are rejected by the command policy.

## Status and delivery evidence

`ToolResult.Status` is propagated through graph, legacy loop, events, persistence mapping, middleware, and model observations. Non-zero exits, timeouts, denials, missing tools, and process-start failures are not successes.

Artifact delivery is separate from file creation. `present_files` validates and registers an existing non-empty regular file under outputs for the current thread/run. The final-answer gate requires successful `present_files` evidence before accepting claims that a file was generated, saved, delivered, or is downloadable.

## Image providers and models

Gemini is selected automatically when `GEMINI_API_KEY` is present, or explicitly with `IMAGE_GENERATION_PROVIDER=gemini`:

```powershell
$env:GEMINI_API_KEY = "..."
$env:GEMINI_API_HOST = "https://generativelanguage.googleapis.com"
$env:GEMINI_IMAGE_MODEL = "gemini-3-pro-image-preview"
```

MiniMax can be selected explicitly:

```powershell
$env:IMAGE_GENERATION_PROVIDER = "minimax"
$env:MINIMAX_API_KEY = "..."
$env:MINIMAX_API_HOST = "https://api.minimaxi.com"
$env:MINIMAX_IMAGE_MODEL = "image-01"
```

These variables are an explicit sandbox environment allowlist in `application.yml`. Values are passed to the generic execution backend and are not included in Tool content or metadata. Network access must be enabled for real provider calls. Prefer the Docker backend for strong filesystem isolation; the local backend is a development mode.

The Gemini host/model and MiniMax host/model are configurable so deployments can follow provider model changes without adding a Tool or changing the Skill CLI.

## Python chart dependencies and CJK fonts

The deterministic comparison renderer under `chart-visualization/scripts/` requires Pillow plus a CJK-capable system font. A Python plotting package does not install Chinese glyphs. Local Windows execution auto-detects Microsoft YaHei, SimHei, SimSun, or DengXian; `DEERFLOW_CJK_FONT` can point to another font file.

For Docker, install the dependencies while building the generic sandbox image rather than from an Agent run:

```dockerfile
FROM python:3.11-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*
RUN python -m pip install --no-cache-dir Pillow seaborn
```

Set `DEERFLOW_CJK_FONT=/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc` when the distribution uses a non-standard font path. Chart rendering must fail instead of silently falling back to a font without the required glyphs.
