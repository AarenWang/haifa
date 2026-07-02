---
name: local-script-execution
description: Use when a task requires local observation, lightweight computation, file inspection, environment checks, or script-based verification.
allowed-tools: [run_script]
activation-hints: [local observation, lightweight computation, file inspection, environment checks, script-based verification, system resource, CPU, memory, 内存, 使用率, 当前电脑, 本机, 系统资源, 运行脚本, 执行脚本, 跑一下, 检查环境]
---

# Local Script Execution Skill

## Overview
This skill enables the Agent to generate and run scripts locally under a secure sandbox to satisfy tasks requiring:
1. Local observation (e.g. system resource utilization, memory limits, process status).
2. Lightweight mathematical computation or data manipulation.
3. Fast file-system checks (e.g. counting lines, scanning encoding, executing validation).
4. Environment diagnostics.

## Guidelines
- **Minimize Scripts**: Write the smallest necessary script.
- **Tool Arguments**: Call `run_script` with JSON shaped as `{"language":"python|powershell","code":"script source","args":[],"purpose":"short reason"}`. Do not use a `script` field.
- **Inspect Outcomes**: Read tool outputs (`stdout`, `stderr`, `exitCode`) to adapt your response.
- **Error Recovery / Robustness**: If a script fails (e.g. missing package like `psutil`), attempt a single alternate script or method (e.g. using PowerShell or a built-in shell utility) to accomplish the goal. Do not retry more than once.
- **No Harm**: Never execute scripts that modify critical system configurations or delete arbitrary directories outside the sandbox outputs.
- **Answer Dynamically**: Avoid hardcoded assumptions. Rely on the actual output retrieved from the script execution.
