# DeerFlow Studio

A minimal, developer-friendly web interface for the DeerFlow agent runtime.

## Quick Start

### 1. Start the backend

From the repository root:

```bash
mvn -pl haifa-ai/haifa-ai-deerflow -am -Popenai clean install -DskipTests
mvn -pl haifa-ai/haifa-ai-deerflow -Popenai spring-boot:run
```

The backend runs at `http://localhost:8095`.

### 2. Start the frontend

```bash
cd haifa-ai/haifa-ai-deerflow-web
npm install
npm run dev
```

The frontend dev server opens at `http://localhost:5173` and proxies `/api` to the backend.

### 3. Open in browser

Navigate to `http://localhost:5173`.

## Features

- **Task Composer** — Submit agent tasks with optional threadId and model overrides.
- **Answer Workspace** — Watch run phases in real time and view the final answer.
- **Activity Trace** — Inspect the event timeline with human-readable summaries.
- **Raw Event Inspector** — Expand any event to view its formatted JSON payload.
- **Stop / Re-run / Clear** — Control the session without losing your input.
- **Responsive layout** — Stacks vertically on mobile, side-by-side on desktop.

## Architecture

- React 19 + TypeScript + Vite
- Custom `fetch` + `ReadableStream` SSE parser (no EventSource)
- `useReducer` for centralized state
- Pure CSS — no heavy UI framework
- `lucide-react` for icons

## API Proxy

`vite.config.ts` forwards `/api` to `http://127.0.0.1:8095` so the frontend uses relative paths.

## Build

```bash
npm run build
```

Output goes to `dist/`.
