# Spring AI MCP Example

This directory contains a small but complete Spring AI MCP sample based on the
official Boot Starter documentation.

- `spring-ai-mcp-server` shows how to build an MCP Server that exposes tools,
  resources, and prompts.
- `spring-ai-mcp-client` shows how to build an MCP Client that connects to the
  server and invokes `callTool`, `readResource`, and `getPrompt`.

Reference docs:

- Spring AI MCP Server Boot Starter:
  `https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-server-boot-starter-docs.html`
- Spring AI MCP Client Boot Starter:
  `https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-client-boot-starter-docs.html`
- Spring AI MCP Java SDK:
  `https://docs.spring.io/spring-ai-mcp/reference/mcp.html`

## Run Order

1. Start the server:

   `mvn -pl haifa-ai/haifa-ai-spring/spring-ai-mcp/spring-ai-mcp-server -am spring-boot:run`

2. Start the client:

   `mvn -pl haifa-ai/haifa-ai-spring/spring-ai-mcp/spring-ai-mcp-client -am spring-boot:run`

3. Call the client endpoints:

- `GET http://localhost:8092/api/mcp/overview`
- `GET http://localhost:8092/api/mcp/tools`
- `POST http://localhost:8092/api/mcp/tools/search-catalog`
- `GET http://localhost:8092/api/mcp/resources/featured`
- `GET http://localhost:8092/api/mcp/prompts/sales-playbook?audience=new-user&goal=first-order-conversion`

## What The Example Covers

- The server keeps an in-memory product catalog and exposes it through MCP.
- The client discovers server capabilities with Spring AI MCP Client.
- The client shows two consumption styles:
  - direct use of `McpSyncClient`
  - MCP tool bridging through Spring AI `ToolCallbackProvider`
