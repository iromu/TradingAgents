Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.33. Integrations

#### 4.33.1. Model Context Protocol (MCP)

##### Publishing

###### Overview
Embabel Agent can expose your agents as MCP servers, making them available to external MCP clients such as Claude Desktop, VS Code extensions, or other MCP-compatible applications.
The framework provides automatic publishing of agent goals as tools and prompts without requiring manual configuration.
###### Server Configuration
Configure MCP server functionality in your `application.yml`.
The server type determines the execution mode:
###### Server Types
Embabel Agent supports two MCP server execution modes controlled by the `spring.ai.mcp.server.type` property:**SYNC Mode** (Default)
- Blocking operations wrapped in reactive streams
- Simpler to develop and debug
- Suitable for most use cases
- Better error handling and logging**ASYNC Mode**
- True non-blocking reactive operations
- Higher throughput for concurrent requests
- More complex error handling
- Suitable for high-performance scenarios
###### Transport Protocol
Embabel Agent uses **SSE (Server-Sent Events) transport**, exposing your MCP server at `localhost:8080/sse`.
This is compatible with Claude Desktop, MCP Inspector, Cursor, and most desktop MCP clients.**Clients requiring Streamable HTTP**Some clients (e.g., OpenWebUI) require Streamable HTTP transport instead of SSE.
Use the `mcpo` proxy to bridge your SSE server:Then connect your client to `localhost:8000`.
###### Automatic Publishing
**Tools**Agent goals are automatically published as MCP tools when annotated with `@Export(remote = true)`.
The `PerGoalMcpToolExportCallbackPublisher` automatically discovers and exposes these goals without any additional configuration.**Prompts**Prompts are automatically generated for each goal’s starting input types through the `PerGoalStartingInputTypesPromptPublisher`.
This provides ready-to-use prompt templates based on your agent definitions.
###### Exposing Agent Goals as Tools
Agent goals become MCP tools automatically when annotated with `@Export`:JavaKotlin
###### Exposing Embabel `ToolObject` and `LlmReference` types as tools
A common requirement is to expose existing Embabel functionality via MCP.
For example, an `LlmReference` might be added to a `PromptRunner` but might also be used as an external tool via MCP.To do this, use `McpToolExport` to create a bean of type `McpToolExportCallbackPublisher`.For example, to expose a `ToolishRag` LLM reference as an MCP tool, define a Spring configuration class as follows:JavaKotlin
| **1** | Your bean should be of type `McpToolExport` || **2** | Use `McpToolExport.fromLlmReference` to return the instance |

###### Naming Strategies
When exporting tools, you can control how tool names are transformed using a naming strategy.
This is useful for namespacing tools when exporting from multiple sources to avoid naming conflicts.**Using ToolObject with a naming strategy:**JavaKotlin
| **1** | All tool names will be prefixed with `myservice_` |
Common naming strategies include:
- **Prefix**: `{ "namespace_$it" }` - adds a prefix to avoid conflicts
- **Uppercase**: `{ it.uppercase() }` - converts to uppercase
- **Identity**: `StringTransformer.IDENTITY` - preserves original names (default)**LlmReference naming:**When using `fromLlmReference`, the reference’s built-in naming strategy is applied automatically.
This prefixes tool names with the lowercased, normalized reference name.
For example, an `LlmReference` named "MyAPI" will prefix all tools with `myapi_`.JavaKotlin**Exporting multiple sources with different prefixes:**JavaKotlin
###### Filtering Tools
You can filter which tools are exported using the `filter` property on `ToolObject`:JavaKotlin
| **1** | Only tools whose names start with `public_` will be exported |
You can combine naming strategies and filters:JavaKotlin
| **1** | The filter is applied to the original tool name before the naming strategy transforms it |

###### Exposing Tools on Spring Components in Spring AI style
It is also possible to expose tools on Spring components as with regular Spring AI.For example:JavaKotlinOf course, you can inject the Embabel `Ai` interface to help do the work of the tools if you wish, or invoke other agents from within the tool methods.For further information, see the Spring AI MCP Annotations Reference.
###### Server Architecture
The MCP server implementation uses several design patterns:**Template Method Pattern**
- `AbstractMcpServerConfiguration` provides common initialization logic
- Concrete implementations (`McpSyncServerConfiguration`, `McpAsyncServerConfiguration`) handle mode-specific details**Strategy Pattern**
- Server strategies abstract sync vs async operations
- Mode-specific implementations handle tool, resource, and prompt management**Publisher Pattern**
- Tools, resources, and prompts are discovered through publisher interfaces
- Automatic registration and lifecycle management
- Event-driven initialization ensures proper timing
###### Built-in Tools
Every MCP server includes a built-in `helloBanner` tool that displays server information:
##### Security
Embabel MCP servers support two complementary layers of security that work together.
Think of them like a building with a reception desk and locked office doors: the HTTP filter
chain is the reception desk that turns away anyone without a badge, and `@SecureAgentTool`
is the locked door on each individual office that checks what the badge actually permits.
###### Layer 1 — HTTP transport (filter chain)
All requests to MCP endpoints (`/sse/`, `/mcp/`, `/message/**`) must carry a valid JWT
Bearer token or they are rejected with `401 Unauthorized` before the GOAP planner is invoked.Configure a `SecurityFilterChain` and a JWT resource server in your Spring Security setup:Kotlin
| **1** | Empty prefix means JWT claim values like `news:read` map directly to Spring Security
authorities, so `hasAuthority('news:read')` in a `@SecureAgentTool` expression works without
any `SCOPE_` prefix. |
Configure JWT validation in `application.yml`:
###### Layer 2 — Method-level (`@SecureAgentTool`)
Enforces per-action authorization inside the GOAP execution pipeline, after the HTTP layer
has validated the token.
Place `@SecureAgentTool` on the `@Agent` class to protect every `@Action` in that agent:KotlinJava
| **1** | Class-level annotation applies to every `@Action` in this agent. || **2** | Both `extractTopic` (the intermediate step) and `produceDigest` (the goal action) require
`news:read` — without class-level security, intermediate actions run freely before the goal
action’s check fires, potentially burning LLM tokens on an unauthorised request. |
See `@SecureAgentTool` for the full annotation
reference including supported SpEL expressions and method-level override behaviour.
###### Dependency
The starter auto-configures `SecureAgentToolAspect` and wires the Spring Security
`MethodSecurityExpressionHandler`. No additional `@EnableMethodSecurity` is required.
##### Consuming
Embabel Agent can consume external MCP servers as tool sources, automatically organizing them into Tool Groups that agents can use.
###### Docker Tools Integration

###### Configuration Approaches
**Docker MCP Gateway** (Recommended)Uses Docker Desktop’s MCP Toolkit extension as a single gateway to multiple tools:**Individual Containers**Run each MCP server as a separate Docker container:
###### Available Tool Groups
Tool Groups are conditionally created based on configured MCP connections using `@ConditionalOnMcpConnection`:
| Tool Group | Required Connections | Capabilities || Web Tools | `brave-search-mcp`, `fetch-mcp`, `wikipedia-mcp`, or `docker-mcp` | Web search, URL fetching, Wikipedia queries || Maps | `google-maps-mcp` or `docker-mcp` | Geocoding, directions, place search || Browser Automation | `puppeteer-mcp` or `docker-mcp` | Page navigation, screenshots, form interaction || GitHub | `github-mcp` or `docker-mcp` | Issues, pull requests, comments |

###### How It Works
The `@ConditionalOnMcpConnection` annotation checks for configured connections at startup:JavaKotlin
| **1** | Bean created if **any** listed connection is configured || **2** | Filter selects which MCP tools belong to this group |

###### Custom Tool Groups
Define custom groups via configuration properties:
#### 4.33.2. A2A

#### 4.33.3. Observability
Embabel Agent provides a unified observability module that automatically traces agent lifecycle, actions, LLM calls, tool invocations, and more — with zero code changes.
It integrates with any OpenTelemetry-compatible backend (Zipkin, Langfuse, Jaeger, Prometheus, etc.).
##### Setup
Add the observability starter to your `pom.xml`:Then add an exporter dependency. For example, Zipkin:Or Langfuse for LLM-focused observability:
| | You can use multiple exporters simultaneously (e.g., Langfuse for traces + Prometheus for metrics). |

##### Configuration
Enable observability and configure your exporter in `application.yml`:For Langfuse:
##### What Gets Traced
All tracing is automatic once the module is on the classpath.
The following events are captured as OpenTelemetry spans, organized in a parent-child hierarchy:
##### Tracing Configuration Properties
All tracing options are enabled by default and can be toggled individually:
| Property | Default | Description || `embabel.observability.enabled` | `true` | Master switch for observability || `embabel.observability.service-name` | `embabel-agent` | Service name in traces || `embabel.observability.trace-agent-events` | `true` | Agent lifecycle (creation, execution, completion, failures) || `embabel.observability.trace-tool-calls` | `true` | Tool invocations with input/output || `embabel.observability.trace-tool-loop` | `true` | Tool loop execution || `embabel.observability.trace-llm-calls` | `true` | LLM calls with token usage || `embabel.observability.trace-planning` | `true` | Planning and replanning iterations || `embabel.observability.trace-state-transitions` | `true` | Workflow state changes || `embabel.observability.trace-lifecycle-states` | `true` | WAITING, PAUSED, STUCK states || `embabel.observability.trace-rag` | `true` | RAG events (request, response, pipeline) || `embabel.observability.trace-ranking` | `true` | Ranking/selection events (agent routing) || `embabel.observability.trace-dynamic-agent-creation` | `true` | Dynamic agent creation events || `embabel.observability.trace-http-details` | `false` | HTTP request/response details (bodies, headers) || `embabel.observability.trace-tracked-operations` | `true` | `@Tracked` annotation aspect || `embabel.observability.mdc-propagation` | `true` | Propagate agent context into SLF4J MDC || `embabel.observability.metrics-enabled` | `true` | Micrometer business metrics (counters, gauges) || `embabel.observability.max-attribute-length` | `4000` | Max span attribute length before truncation |

##### Custom Operation Tracking with `@Tracked`
The `@Tracked` annotation lets you add observability spans to your own methods.
Inputs, outputs, duration, and errors are captured automatically.You can specify a type and description for richer traces:Available track types:
| Type | Description || `CUSTOM` | General-purpose (default) || `PROCESSING` | Data processing operation || `VALIDATION` | Validation or verification step || `TRANSFORMATION` | Data transformation || `EXTERNAL_CALL` | External service/API call || `COMPUTATION` | Computation or calculation |
When called within an agent execution, `@Tracked` spans are automatically nested under the current action:
| | `@Tracked` uses Spring AOP proxies.
Internal method calls within the same class are **not** intercepted.
Extract tracked methods into a separate `@Component` bean for the annotation to work. |

##### MDC Log Correlation
Agent context is automatically propagated into SLF4J MDC, enabling log filtering by agent run or action.MDC keys set automatically:
| MDC Key | Description | Set on | Removed on || `embabel.agent.run_id` | Agent process ID | Agent creation | Agent completed/failed/killed || `embabel.agent.name` | Agent name | Agent creation | Agent completed/failed/killed || `embabel.action.name` | Current action name | Action start | Action result |
Example Logback pattern:This produces logs like:To disable MDC propagation:
##### Supported Backends

| Backend | Type | Module || Langfuse | Traces | `opentelemetry-exporter-langfuse` || Zipkin | Traces | `opentelemetry-exporter-zipkin` || OTLP (Jaeger, Tempo) | Traces | `opentelemetry-exporter-otlp` || Prometheus | Metrics | `micrometer-registry-prometheus` |
For full details, see the Observability Module Documentation.