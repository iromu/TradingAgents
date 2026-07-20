# Tools Reference

## Overview

Embabel provides two categories of tools for exposing functionality to LLMs:

- **In-process tools** — JVM methods annotated with `@LlmTool` or Spring AI's `@Tool`
- **Remote tools** — MCP (Model Context Protocol) servers whose tools are discovered and wrapped

---

## @LlmTool: Expose JVM Methods

Annotate methods on any class (stateful or stateless, static or instance, any visibility):

```java
public class MathTools {
    @LlmTool(description = "add two numbers")
    public double add(double a, double b) { return a + b; }
}
```

- Each annotated method becomes a distinct tool exposed to the LLM
- Return type must be serializable
- Not supported: `Optional`, async types, reactive types, functional types
- Tool methods can bind to `AgentProcess` to publish objects to the blackboard

---

## @Tool (Spring AI)

The Spring AI `@Tool` annotation is also valid. Use it when you want IDE support or are already using Spring AI tool calling.

---

## ToolCallContext: Inject Out-of-Band Metadata

`ToolCallContext` is an immutable key-value bag that flows through the tool pipeline without the LLM ever seeing it — like HTTP headers on a request.

### Injecting into @LlmTool Methods

Declare a `ToolCallContext` parameter — the framework injects it and excludes it from the JSON schema:

```java
@LlmTool(description = "Look up customer by ID")
public String lookupCustomer(
        @LlmTool.Param(description = "Customer ID") long customerId,
        ToolCallContext context) {
    String tenantId = context.get("tenantId");
    return customerService.lookup(customerId, tenantId);
}
```

### Setting Context

**At process boundary** (cross-cutting infrastructure):

```java
var processOptions = new ProcessOptions()
    .withToolCallContext(Map.of("authToken", token, "tenantId", "acme"));
```

**Per-interaction** (domain-specific metadata):

```java
return ai.withDefaultLlm()
    .withToolCallContext(Map.of("entityId", "123"))
    .createObject(prompt, Result.class);
```

### Context Merge

Interaction-level values win on conflict. `ProcessOptions` is for cross-cutting concerns; `PromptRunner.withToolCallContext()` is for per-interaction concerns.

### MCP Meta Export

`ToolCallContext` entries are forwarded as MCP `_meta` on the wire. Control what crosses the boundary:

```java
@Bean
public ToolCallContextMcpMetaConverter toolCallContextMcpMetaConverter() {
    return ToolCallContextMcpMetaConverter.allowKeys("tenantId", "correlationId");
}
```

Factory methods: `passThrough()` (default), `noOp()`, `allowKeys(...)`, `denyKeys(...)`, or a custom lambda.

---

## OneShotPerLoopTool

Prevent repeated tool calls within a single planning loop iteration:

```java
Tool gated = new OneShotPerLoopTool(underlyingTool,
    "The body was returned earlier — read it from your conversation history.");
```

Loop scoping uses `LoopMemo` which reads `ToolCallContext.loopId()` — stamp a fresh UUID per turn:

```kotlin
val loopId = UUID.randomUUID().toString()
context.ai()
    .withToolCallContext(mapOf(ToolCallContext.LOOP_ID_KEY to loopId))
    .withTools(gatedTools)
    .respond(messages)
```

---

## @Cost / costMethod: Dynamic Tool Costs

Specify a cost estimate so budget-aware agents can check remaining budget before invoking:

```java
@LlmTool(description = "Expensive web search", costMethod = "estimateSearchCost")
public String search(String query) { ... }

@Cost
public int estimateSearchCost(String query) {
    return query.length() > 50 ? 100 : 50;
}
```

---

## Tool Groups

Tool groups provide indirection between user intent and tool selection — ask for "web" tools, not a specific search engine.

### @Configuration

```java
@Bean
ToolGroup mcpWebToolsGroup() {
    return new McpToolGroup(
        CoreToolGroups.WEB_DESCRIPTION, "docker-web", "Docker",
        Set.of(ToolGroupPermission.INTERNET_ACCESS),
        mcpSyncClients,
        cb -> cb.getToolDefinition().name().contains("brave")
    );
}
```

### PromptRunner Methods

| Method | Description |
|---|---|
| `withToolGroup(String)` | Add by name |
| `withToolGroup(ToolGroup)` | Add instance |
| `withToolObject(Any)` | Add domain object with `@LlmTool`/`@Tool` methods |
| `withTool(Tool)` | Add a framework-agnostic Tool |
| `withTools(List<Tool>)` | Add multiple tools |

---

## Domain Tools

Stateful tools on domain objects that encapsulate state never exposed to the LLM:

```java
context.ai()
    .withDefaultLlm()
    .withToolObject(customer)
    .creating(Order.class)
    .fromPrompt("Create an order");
```

---

## Tool Chaining

Expose `@LlmTool` methods on returned objects — the LLM navigates through well-defined operations:

```java
var userManager = new SimpleAgenticTool("userManager", "Manage users")
    .withTools(searchUserTool, getUserTool)
    .withToolChainingFrom(User.class);
// Flow: getUserTool returns a User -> updateEmail() becomes available
```

- **Predicate filtering**: `.withToolChainingFrom(User.class, (user, ctx) -> user.isAdmin())`
- **Auto-discovery**: `.withToolChainingFromAny()` — discover on any returned object
- **Last-wins**: only the most recent artifact of a type is active
- **On PromptRunner**: works on any `PromptRunner`, not just agentic tools

---

## Framework-Agnostic Tool Interface

The `Tool` interface is not tied to any LLM framework:

### Creating Tools

```java
Tool greetTool = Tool.of("greet", "Greets the user") { _ ->
    Tool.Result.text("Hello!")
};

Tool addTool = Tool.of("add", "Adds two numbers",
    Tool.InputSchema.of(
        Tool.Parameter.integer("a", "First number"),
        Tool.Parameter.integer("b", "Second number")
    )
) { input -> Tool.Result.text("42") };
```

### Strongly Typed Tools

```java
Tool addTool = Tool.fromFunction("add", "Adds two numbers",
    AddRequest.class, AddResult.class,
    input -> new AddResult(input.a() + input.b())
);
```

### From Annotated Methods

```java
List<Tool> tools = Tool.fromInstance(new MathService());
```

---

## MCP Integration

### McpToolFactory

```java
@Bean
public McpToolFactory mcpToolFactory(List<McpSyncClient> clients) {
    return new SpringAiMcpToolFactory(clients);
}
```

```java
// Single tool
Tool braveSearch = mcpToolFactory.toolByName("brave_web_search");

// Group behind UnfoldingTool facade
UnfoldingTool wikiTool = mcpToolFactory.unfoldingByName(
    "wikipedia", "Search Wikipedia",
    Set.of("search_wikipedia", "get_article", "get_summary")
);
```

### Lazy MCP Initialization

For OAuth-authenticated MCP servers, defer the handshake:

```yaml
spring:
  ai:
    mcp:
      client:
        initialized: false
        toolcallback:
          enabled: false
embabel:
  agent:
    platform:
      tools:
        lazy-init: true
```

---

## MCP Docker Tools

Embabel supports consuming tools from Docker containers via the MCP Docker Gateway. Tools are conditionally created based on available images.

### CoreToolGroups

Embabel provides predefined tool groups:

| Group | Description |
|-------|-------------|
| `CoreToolGroups.WEB` | Web search, browsing tools |
| `CoreToolGroups.BROWSER_AUTOMATION` | Browser automation tools |
| `CoreToolGroups.MAPS` | Maps and location tools |
| `CoreToolGroups.GITHUB` | GitHub operations tools |

### Docker MCP Gateway Configuration

```yaml
spring:
  ai:
    mcp:
      docker:
        enabled: true
        gateway-url: http://mcp-gateway:3100
        tool-groups:
          - WEB
          - BROWSER_AUTOMATION
```

### Conditional Tool Creation

Tools are created only when the required Docker image is available:

```java
@Bean
public McpToolFactory mcpDockerTools(McpSyncClient dockerClient) {
    return new SpringAiMcpToolFactory(List.of(dockerClient));
}
```

### Custom Tool Groups

Create custom tool groups from Docker containers:

```java
@Bean
public UnfoldingTool dockerWebTools(McpToolFactory factory) {
    return factory.unfoldingByName("docker-web", "Docker Web Tools",
        Set.of(ToolGroupPermission.INTERNET_ACCESS));
}
```

---

## Subagent: Agent Handoffs as Tools

A Subagent delegates to another Embabel agent, sharing the parent's blackboard:

```java
var subagent = Subagent.ofClass(PerformanceFinder.class)
    .consuming(WorksToFind.class);

context.ai()
    .withDefaultLlm()
    .withTool(subagent)
    .creating(Concert.class)
    .fromPrompt("Assemble a concert");
```

Reference by name, instance, or annotated instance.

---

## Agentic Tools

An agentic tool uses an LLM to orchestrate sub-tools:

### SimpleAgenticTool

```java
var orchestrator = new SimpleAgenticTool("math-orchestrator", "Orchestrates math")
    .withTools(addTool, multiplyTool)
    .withParameter(Tool.Parameter.string("expression", "Math expression"))
    .withLlm(LlmOptions.withModel("gpt-4"));
```

### PlaybookTool: Conditional Unlocking

```java
var researcher = new PlaybookTool("researcher", "Research topics")
    .withTools(searchTool, fetchTool)
    .withTool(analyzeTool).unlockedBy(searchTool)
    .withTool(summarizeTool).unlockedBy(analyzeTool);
```

All three support tool chaining via `withToolChainingFrom()`.

---

## Progressive Tools (UnfoldingTool)

Progressive disclosure — present a single facade, reveal inner tools on invocation:

```java
var databaseTool = UnfoldingTool.of(
    "database_operations",
    "Work with the database. Invoke to see specific operations.",
    List.of(queryTool, insertTool, deleteTool)
);
```

### Category-Based

```java
var fileTool = UnfoldingTool.byCategory(
    "file_operations",
    "File operations. Pass category: 'read' or 'write'.",
    Map.of("read", List.of(readFile, listDir), "write", List.of(writeFile, deleteFile))
);
```
### Annotation-Based

```java
@UnfoldingTools(name = "database_operations", description = "Database operations")
public class DatabaseTools {
    @LlmTool(description = "Execute a SQL query")
    public QueryResult query(String sql) { ... }
}
var tool = UnfoldingTool.fromInstance(new DatabaseTools());
```
- **ToolCallContext is invisible to the LLM** — never put sensitive data in `@LlmTool` parameters; use `ToolCallContext` for infrastructure metadata
- **OneShotPerLoopTool requires a loop ID** — without stamping `ToolCallContext.LOOP_ID_KEY`, the wrapper degrades to passthrough
- **MCP meta converter defaults to pass-through** — for production with third-party MCP servers, always set an allowlist or denylist
- **Tool chaining uses last-wins semantics** — only the most recent artifact of a type is active
- **Async/reactive types are not supported** as tool parameters or return types
- **Lazy MCP init requires three properties** — both Spring AI flags and `embabel.agent.platform.tools.lazy-init=true`
---

*Source: Embabel Agent v1.0.0 documentation*
