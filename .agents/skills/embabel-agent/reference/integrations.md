# Integrations Reference

MCP, A2A, observability, and security integrations for Embabel agents. See SKILL.md for the core workflow.

## MCP Server Publishing

### Server Configuration

Publish agents as MCP servers with SYNC or ASYNC transport:

```yaml
embabel:
  agent:
    platform:
      mcp:
        server:
          name: my-agent-server
          version: 1.0.0
          transport: SSE  # SYNC or SSE or Streamable-HTTP
```

### Automatic Tool Publishing

Goals are automatically published as MCP tools. Use `McpToolExport` to expose `LlmReference` types:

```java
@McpTool(name = "search", description = "Search the knowledge base")
public LlmReference searchTool() {
    return LlmReference.builder()
        .description("Search for documents")
        .build();
}
```

### Tool Filtering

Control which tools are exposed:

```java
@Bean
public ToolGroup mcpToolsGroup(McpSyncClient client) {
    return new McpToolGroup(
        "MCP Tools",
        "mcp-server",
        "Embabel",
        Set.of(ToolGroupPermission.INTERNET_ACCESS),
        List.of(client),
        callback -> {
            String name = callback.getToolDefinition().name();
            return !name.contains("internal");  // Exclude internal tools
        }
    );
}
```

### Spring AI @McpTool

Use Spring AI's `@McpTool` annotation for simpler MCP tool exposure:

```java
@McpTool(description = "Get the current weather")
public String getWeather(String location) {
    return weatherService.getWeather(location);
}
```

## MCP Security

### Layer 1: HTTP Filter Chain

Secure MCP endpoints with JWT authentication:

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(exchange -> exchange
            .pathMatchers("/mcp/**").authenticated()
            .anyExchange().permitAll()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
}
```

### Layer 2: @SecureAgentTool

Protect individual agents or methods with SpEL expressions:

```java
@Agent
@SecureAgentTool(expression = "hasAuthority('news:read')")
public class NewsAgent { ... }

// Method-level overrides class-level
@Action
@SecureAgentTool(expression = "hasAuthority('news:write')")
public void publishNews(News news) { ... }
```

Requires: `embabel-agent-mcp-security` starter.

## MCP Client / Consuming

### Docker Tools Integration

Use MCP tools from Docker containers:

```java
@Bean
@ConditionalOnMcpConnection
public ToolGroup dockerWebTools(McpSyncClient dockerClient) {
    return new McpToolGroup(
        "Web tools from Docker",
        "docker-web",
        "Docker",
        Set.of(ToolGroupPermission.INTERNET_ACCESS),
        List.of(dockerClient),
        callback -> {
            String name = callback.getToolDefinition().name();
            return name.contains("brave") || name.contains("fetch");
        }
    );
}
```

### Conditional MCP Connection

Only create MCP clients when the server is available:

```java
@Bean
@ConditionalOnMcpConnection
public McpSyncClient dockerMcpClient() {
    return McpClient.sync(...)
        .serverUrl("http://localhost:3001")
        .buildSyncClient();
}
```

### Tool Groups

Configure tool groups for MCP tools:

| Group | Description | Typical Tools |
|-------|-------------|---------------|
| `WEB` | Web search and browsing | Brave, Google, DuckDuckGo |
| `MAPS` | Location-based tools | Google Maps, OpenStreetMap |
| `BROWSER_AUTOMATION` | Browser control | Playwright, Puppeteer |
| `GITHUB` | GitHub operations | Issues, PRs, repos |

## Observability

### Setup

Add the observability starter and configure exporters:

```yaml
embabel:
  agent:
    platform:
      observability:
        enabled: true
        exporter: langfuse  # langfuse, langsmith, zipkin
        endpoint: https://app.langfuse.com
        public-key: ${LANGFUSE_PUBLIC_KEY}
        secret-key: ${LANGFUSE_SECRET_KEY}
```

### Supported Exporters

| Exporter | Dependency | Use Case |
|----------|-----------|----------|
| Langfuse | `embabel-agent-observability-langfuse` | Production monitoring |
| LangSmith | `embabel-agent-observability-langsmith` | Debugging & testing |
| Zipkin | `embabel-agent-observability-zipkin` | Distributed tracing |

### Migration Notes

Older versions used different property names. Check the migration guide in `migrating.md` for upgrade instructions.

## A2A (Agent-to-Agent)

A2A enables agents to communicate with each other across process boundaries. Configure A2A cards and task definitions for inter-agent workflows.

## Key Points

- MCP servers publish goals as tools automatically
- Use `McpToolExport` for `LlmReference` exposure
- Two-layer security: HTTP filter chain + `@SecureAgentTool`
- MCP clients are conditionally created with `@ConditionalOnMcpConnection`
- Observability supports Langfuse, LangSmith, and Zipkin
- Tool groups provide abstraction between intent and tool selection