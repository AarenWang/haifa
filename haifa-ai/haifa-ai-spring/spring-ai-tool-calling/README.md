# spring-ai-tool-calling

This sample demonstrates how to wrap geography search capabilities as callable tools that can be
invoked from a `ChatClient`. Instead of talking to a remote LLM, the module provides a custom
`GeoChatClient` implementation that illustrates the flow end-to-end and keeps the sample runnable
without external credentials.

## Getting started

1. Export the credentials for the geography service and (optionally) the LLM provider:

   ```bash
   export GEO_KNOWLEDGE_BASE_URL="https://example.com/api"
   export GEO_KNOWLEDGE_API_KEY="your-api-key"
   export SPRING_AI_OPENAI_API_KEY="your-openai-token"
   ```

2. Build and run the module from the repository root:

   ```bash
   mvn -pl haifa-ai/haifa-ai-spring/spring-ai-tool-calling spring-boot:run
   ```

3. Interact with the REST endpoint:

   ```bash
   curl -X POST http://localhost:8080/api/geo/chat \
        -H "Content-Type: application/json" \
        -d '{"message": "Tell me about Beijing"}'
   ```

   The response contains the synthesized answer backed by the geography knowledge tool.

## Testing

The module includes an integration test (`GeoChatControllerIntegrationTest`) that spins up a
`MockWebServer` to emulate the external API and verifies the tool calling pipeline end-to-end:

```bash
mvn -pl haifa-ai/haifa-ai-spring/spring-ai-tool-calling test
```

## Configuration reference

- `haifa.ai.tool-calling.model` – default model identifier added to the generated answer.
- `haifa.ai.tool-calling.response-prefix` – prefix appended to the response for additional context.
- `haifa.ai.tool-calling.geo.base-url` – base URL of the geography knowledge API.
- `haifa.ai.tool-calling.geo.path` – relative path used for lookups.
- `haifa.ai.tool-calling.geo.api-key` – optional API key sent as `X-API-Key` header.
- `haifa.ai.tool-calling.geo.timeout` – timeout applied to external calls (e.g. `5s`).

These properties can be set through `application.yml`, environment variables, or command-line
arguments when starting the application.
