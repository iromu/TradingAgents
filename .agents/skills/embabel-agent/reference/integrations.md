# Integrations Reference

MCP, A2A, security, and observability integrations for Embabel agents. See SKILL.md for the core workflow.

---

## Model Context Protocol (MCP)

Embabel Agent exposes agents as MCP servers for external clients (Claude Desktop, Cursor, MCP Inspector). Goals are auto-published as tools and prompts.

### Server Configuration

```yaml
spring:
  ai:
    mcp:
      server:
        type: SYNC  # SYNC (default) or ASYNC
```

| **SYNC** (default) | Blocking ops in reactive streams. Simple, good error handling. |
| **ASYNC** | True non-blocking reactive. Higher throughput, more complex error handling. |

### Transport Protocol

Uses **SSE transport** at `http://localhost:8080/sse`. For clients requiring Streamable HTTP (e.g., OpenWebUI), use the `mcpo` proxy:

```bash
uvx mcpo --port 8000 --server-type sse -- http://localhost:8080/sse
```

Then connect to `http://localhost:8000`.

---

## Automatic Publishing

Goals annotated with `@Export(remote = true)` are auto-discovered via `PerGoalMcpToolExportCallbackPublisher`. Prompts are generated for each goal's starting input types via `PerGoalStartingInputTypesPromptPublisher`.

---

## Exposing Agent Goals as Tools

Annotate goal methods with `@Export(remote = true)`:

```java
@Agent(goal = "Provide weather information", backstory = "Weather service agent")
public class WeatherAgent {

    @Goal
    @Export(remote = true)  // Becomes MCP tool
    public String getWeather(@Param("location") String location,
                             @Param("units") String units) {
        return "Weather for " + location + " in " + units;
    }

    @Goal
    public String internalMethod() {
        // Not exposed to MCP
        return "Internal use only";
    }
}
```

```kotlin
@Agent(goal = "Provide weather information", backstory = "Weather service agent")
class WeatherAgent {

    @Goal
    @Export(remote = true)  // Becomes MCP tool
    fun getWeather(
        @Param("location") location: String,
        @Param("units") units: String
    ): String = "Weather for $location in $units"

    @Goal
    fun internalMethod(): String = "Internal use only"  // Not exposed
}
```

---

## Exposing LlmReference as MCP Tools

Use `McpToolExport` to expose `LlmReference` or `ToolObject` types:

```java
@Configuration
public class RagMcpTools {
    @Bean
    McpToolExport ragTools(SearchOperations searchOperations) {
        var toolishRag = new ToolishRag("docs", "Embabel docs", searchOperations);
        return McpToolExport.fromLlmReference(toolishRag);
    }
}
```

```kotlin
@Configuration
class RagMcpTools {
    @Bean
    fun ragTools(searchOperations: SearchOperations): McpToolExport {
        val toolishRag = ToolishRag("docs", "Embabel docs", searchOperations)
        return McpToolExport.fromLlmReference(toolishRag)
    }
}
```

### Naming Strategies

Control tool name transformation to avoid conflicts:

```java
// ToolObject with prefix
@Bean
public McpToolExport prefixedTools() {
    return McpToolExport.fromToolObject(
        new ToolObject(List.of(myTool), name -> "myservice_" + name));
}
```

```kotlin
@Bean
fun prefixedTools(): McpToolExport {
    return McpToolExport.fromToolObject(
        ToolObject(objects = listOf(myTool), namingStrategy = { "myservice_$it" }))
}
```

**LlmReference naming**: `fromLlmReference` prefixes tools with the lowercased reference name (e.g., "WeatherService" → `weatherservice_getWeather`).

### Filtering

```java
@Bean
public McpToolExport filteredTools() {
    return McpToolExport.fromToolObject(
        new ToolObject(List.of(myTool), StringTransformer.IDENTITY,
            name -> name.startsWith("public_")));  // Only public tools
}
```

The filter applies to the **original** name before the naming strategy transforms it.

### Spring AI @McpTool on Components

```java
@Component
public class CalculatorTools {
    @McpTool(name = "add", description = "Add two numbers")
    public int add(@McpToolParam(description = "First", required = true) int a,
                   @McpToolParam(description = "Second", required = true) int b) {
        return a + b;
    }
}
```

---

## Security

Two complementary layers: HTTP filter chain (reception desk) + `@SecureAgentTool` (locked office door).

### Layer 1 — HTTP Transport (Filter Chain)

All requests to `/sse/**`, `/mcp/**`, `/message/**` require a JWT Bearer token.

```kotlin
@Configuration
@EnableWebSecurity
class McpSecurityConfiguration {
    @Bean
    fun mcpFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.securityMatcher("/sse/**", "/mcp/**", "/message/**")
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt { jwt ->
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) }}
            .csrf { it.disable() }
        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val conv = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("authorities"); setAuthorityPrefix("") }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(conv) }
    }
}
```

**JWT config:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          public-key-location: classpath:keys/public.pem
          jws-algorithms: RS256
```

### Layer 2 — Method-level (`@SecureAgentTool`)

Protects every `@Action` in an agent class. Kotlin:

```kotlin
@Agent(description = "Curated news digest agent")
@SecureAgentTool("hasAuthority('news:read')")
class NewsDigestAgent {
    @Action
    fun extractTopic(userInput: UserInput, context: OperationContext): NewsTopic { ... }

    @Goal(description = "Produce news digest",
                  export = Export(remote = true, name = "newsDigest",
                                  startingInputTypes = [UserInput::class]))
    @Action
    fun produceDigest(topic: NewsTopic, context: OperationContext): NewsDigest { ... }
}
```

Java:

```java
@Agent(description = "Curated news digest agent")
@SecureAgentTool("hasAuthority('news:read')")
public class NewsDigestAgent {
    @Action
    public NewsTopic extractTopic(UserInput userInput, OperationContext context) { ... }

    @Goal(description = "Produce news digest",
                  export = @Export(remote = true, name = "newsDigest",
                                   startingInputTypes = {UserInput.class}))
    @Action
    public NewsDigest produceDigest(NewsTopic topic, OperationContext context) { ... }
}
```

Without class-level security, intermediate actions run freely before the goal action's check fires — potentially burning LLM tokens on an unauthorized request.

**Dependency:**

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-mcpserver-security</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
```

---

## Observability

Auto-traces agent lifecycle, actions, LLM calls, tool invocations — zero code changes. Integrates with any OpenTelemetry backend (Zipkin, Langfuse, LangSmith, Jaeger, Prometheus).

### Setup

Add the observability starter and an exporter. For Zipkin:

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-observability</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
```

Or the Embabel exporter (Langfuse + LangSmith in one):

```xml
<dependency>
    <groupId>com.quantpulsar</groupId>
    <artifactId>opentelemetry-exporter-embabel</artifactId>
    <version>0.6.0</version>
</dependency>
```

### Configuration

```yaml
embabel:
  agent:
    platform:
      observability:
        enabled: true
        service-name: my-agent-app

management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  langfuse:
    enabled: true
    endpoint: https://cloud.langfuse.com/api/public/otel
    public-key: pk-lf-...
    secret-key: sk-lf-...
```

Spans are organized in a parent-child hierarchy. Key span types: `embabel.agent` (one run turn), `embabel.action` (actions), `embabel.tool_loop` (tool loop), `embabel.llm` (LLM calls with token usage/cost), `embabel.tool` (tool invocations), `embabel.goal` / `embabel.lifecycle` (goal achievement and lifecycle states).

### Key Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `observability.enabled` | `true` | Master switch for traces + metrics |
| `observability.tracing-enabled` | `true` | Umbrella for all tracing |
| `observability.service-name` | `embabel-agent` | Service name in traces |
| `observability.trace-agent` | `true` | `embabel.agent` span (one run turn) |
| `observability.trace-action` | `true` | `embabel.action` span |
| `observability.trace-tool-calls` | `true` | Tool invocations |
| `observability.trace-tool-loop` | `true` | Tool loop execution |
| `observability.trace-llm-calls` | `true` | LLM calls with token usage/cost |
| `observability.capture-message-content` | `true` | Capture message bodies (opt-in for PII safety) |
| `observability.disabled-traces` | `[]` | Observation names to suppress |

### Custom Tracking with `@Tracked`

Add spans to your own methods:

```java
@Tracked("enrichCustomer")
public Customer enrich(Customer input) { ... }

@Tracked(value = "callPaymentApi", type = TrackType.EXTERNAL_CALL, description = "Payment gateway")
public PaymentResult processPayment(Order order) { ... }
```

Track types: `CUSTOM`, `PROCESSING`, `VALIDATION`, `TRANSFORMATION`, `EXTERNAL_CALL`, `COMPUTATION`.

> **Note:** `@Tracked` uses Spring AOP proxies. Internal method calls within the same class are *not* intercepted — extract tracked methods into a separate `@Component` bean.

### MDC Log Correlation

Agent context is propagated into SLF4J MDC automatically (`embabel.agent.run_id`, `embabel.agent.name`, `embabel.action.name`).

```xml
<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36}
  [runId=%X{embabel.agent.run_id} agent=%X{embabel.agent.name} action=%X{embabel.action.name}] - %msg%n</pattern>
```

---

## Observability Migration Notes

When upgrading from older Embabel versions, be aware of the following changes:

### Breaking Changes

- **Config prefix changed**: Observability config moved from `embabel.observability.*` to `embabel.agent.platform.observability.*`
- **`trace-http-details`**: Now defaults to `false` for privacy (was `true` in older versions)
- **Removed properties**: `embabel.observability.capture-tool-calls` was removed — use `embabel.agent.platform.observability.trace-tool-calls` instead

### What Gets Traced

| Span Type | Description |
|-----------|-------------|
| `embabel.agent` | One run turn (parent span) |
| `embabel.action` | Individual action execution |
| `embabel.tool_loop` | Tool loop execution |
| `embabel.llm` | LLM calls with token usage/cost |
| `embabel.tool` | Tool invocations |
| `embabel.goal` | Goal achievement |
| `embabel.lifecycle` | Lifecycle states |

### Custom Tracking with `@Tracked`

Add spans to your own methods:

```java
@Tracked("enrichCustomer")
public Customer enrich(Customer input) { ... }

@Tracked(value = "callPaymentApi", type = TrackType.EXTERNAL_CALL, description = "Payment gateway")
public PaymentResult processPayment(Order order) { ... }
```

Track types: `CUSTOM`, `PROCESSING`, `VALIDATION`, `TRANSFORMATION`, `EXTERNAL_CALL`, `COMPUTATION`.

> **Note:** `@Tracked` uses Spring AOP proxies. Internal method calls within the same class are *not* intercepted — extract tracked methods into a separate `@Component` bean.

---

## MCP Consuming

Embabel can consume external MCP servers as tool sources. Configure MCP clients and they are automatically available as tools:

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        tools:
          enabled: true
```

MCP tools are discovered at startup and registered as `Tool` beans. Use `McpToolFactory` to access them by name or group.

---

## Agent-to-Agent (A2A)

A2A enables agents to communicate across process boundaries. Configure A2A cards and task definitions for inter-agent workflows.
---

*Source: Embabel Agent v1.0.0 documentation*
