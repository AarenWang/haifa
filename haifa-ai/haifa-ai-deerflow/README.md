# haifa-ai-deerflow

Minimal Java DeerFlow prototype for Phase 1 + Phase 2.

It provides:

- a Spring Boot WebFlux gateway,
- an in-memory run manager,
- an agent runtime that emits SSE events,
- three safe local tools,
- a Spring AI model adapter with a deterministic fallback when no provider is configured.

## Run

```bash
mvn -f haifa-ai/haifa-ai-deerflow/pom.xml org.springframework.boot:spring-boot-maven-plugin:3.5.7:run
```

With OpenAI-compatible Spring AI configuration:

```bash
set SPRING_AI_OPENAI_API_KEY=your-key
set HAIFA_DEERFLOW_MODEL=gpt-4o-mini
mvn -Popenai -f haifa-ai/haifa-ai-deerflow/pom.xml org.springframework.boot:spring-boot-maven-plugin:3.5.7:run
```

## Stream a Run

```bash
curl -N -X POST http://localhost:8095/api/deerflow/runs/stream ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"List the workspace files, then explain what you saw\"}"
```

The runtime currently executes safe local tools deterministically before the
model call. Later phases should move tool selection into model tool-calling and
add middleware, skills, sandbox providers, and durable persistence.
