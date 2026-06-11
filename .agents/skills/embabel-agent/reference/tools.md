# Tools Reference

## @LlmTool: Expose JVM Methods to LLMs

```java
public class MathTools {
    @LlmTool(description = "add two numbers")
    public double add(double a, double b) {
        return a + b;
    }
}
```

- Tool methods can have any visibility, static or instance scope
- Return type must be serializable
- Not supported: Optional, async types, reactive types, functional types
- Tools can be stateful — often encapsulate domain objects with private state

## Tool Groups

Tool groups provide indirection between user intent and tool selection:

```yaml
embabel:
  agent:
    platform:
      tools:
        includes:
          weather:
            description: Get weather for location
            provider: Docker
            tools:
              - weather
```

Configure in `@Configuration`:

```java
@Configuration
public class ToolConfig {
    @Bean
    ToolGroup weatherTools(List<McpSyncClient> clients) {
        return new McpToolGroup("weather", "Docker", "Get weather for location",
            Set.of("INTERNET_ACCESS"),
            client -> client.getTools(),
            tool -> tool.getName().equals("get_weather")
        );
    }
}
```

## ToolCallContext

For infrastructure metadata (auth tokens, tenant IDs) that the LLM should never see:

```java
@LlmTool(description = "Look up customer")
public String lookupCustomer(
        @LlmTool.Param(description = "Customer ID") long customerId,
        ToolCallContext context) {
    String tenantId = context.get("tenantId");
    String authToken = context.get("authToken");
    return customerService.lookup(customerId, tenantId, authToken);
}
```

Set at invocation:

```java
ProcessOptions.withToolCallContext(Map.of("tenantId", "acme", "authToken", "xyz"))
```

Per-interaction:

```java
context.ai().withDefaultLlm().withToolCallContext(Map.of("entityId", "123"))
```

Context merge: interaction-level values win on conflict.

## OneShotPerLoopTool

For tools meant to fire at most once per agentic loop iteration:

```java
var tool = new OneShotPerLoopTool(
    underlyingTool,
    "The body was returned earlier — read it from your conversation history."
);
```

## Subagent: Agent Handoffs as Tools

```java
var subagentTool = Subagent.ofClass(PerformanceFinder.class)
    .consuming(WorksToFind.class);

context.ai().withDefaultLlm()
    .withTool(subagentTool)
    .creating(Concert.class)
    .fromPrompt("Find performances and assemble a concert");
```

The LLM can now invoke another agent as a tool. The subagent shares the parent's blackboard context.

## Agentic Tools

### SimpleAgenticTool: Flat Tool Orchestration

```java
var agenticTool = SimpleAgenticTool.builder()
    .withLlm(LlmOptions.withModel("gpt-4o"))
    .withTools(new Calculator(), new Formatter())
    .withSystemPrompt("You are a math assistant. Use the calculator and formatter tools.")
    .build();
```

### PlaybookTool: Conditional Tool Unlocking

```java
var playbook = PlaybookTool.builder()
    .withTools(
        new SearchTool(),
        new AnalyzeTool().withPrerequisites(Set.of("search")),
        new SummarizeTool().withArtifacts(Set.of("analysis"))
    )
    .build();
```

### StateMachineTool: State-Based Availability

```java
var smTool = StateMachineTool.builder(OrderState.class)
    .withTools(
        new CreateOrderTool().availableIn(OrderState.DRAFT),
        new ConfirmOrderTool().availableIn(OrderState.DRAFT),
        new ShipOrderTool().availableIn(OrderState.CONFIRMED),
        new DeliverOrderTool().availableIn(OrderState.SHIPPED)
    )
    .build();
```

## Domain Tools

Tools from `@LlmTool` methods on domain objects:

```java
public class Customer {
    @Tool
    public double getLoyaltyDiscount() { ... }
}

// Add to prompt runner
context.ai().withDefaultLlm()
    .withToolObject(customer)
    .creating(Order.class)
    .fromPrompt("Create an order for this customer");
```

The LLM can call `customer.getLoyaltyDiscount()` as a tool.

## Tool Chaining

Expose `@LlmTool` methods on returned objects:

```java
context.ai().withDefaultLlm()
    .withToolChainingFrom(Customer.class)
    .creating(Customer.class)
    .fromPrompt("Find the customer");
// After customer is returned, customer.getLoyaltyDiscount() becomes available as a tool
```

## Framework-Agnostic Tool Interface

```java
var tool = Tool.create("add", "Add two numbers",
    List.of(
        Tool.Parameter.integer("a", "First number"),
        Tool.Parameter.integer("b", "Second number")
    ),
    args -> (long) args.get("a") + (long) args.get("b")
);
```

Or from annotated methods:

```java
var tool = LlmToolMethodTool.from(MathTools.class, "add");
```

## Using Tools in Action Methods

```java
@Action
public RelevantNews findNews(StarPerson person, OperationContext context) {
    return context.ai().withDefaultLlm()
        .withToolGroup(CoreToolGroups.WEB)
        .creating(RelevantNews.class)
        .fromPrompt("Find news about " + person.name());
}
```

Chaining multiple tool groups:

```java
context.ai().withDefaultLlm()
    .withToolGroup("weather")
    .withToolGroup("news")
    .creating(Report.class)
    .fromPrompt("Create a weather and news report");
```

## Key Points

- `@LlmTool` methods can be on any class — stateful domain objects are common
- Tool groups provide indirection; configure in YAML or `@Configuration`
- `ToolCallContext` passes infrastructure metadata invisible to the LLM
- Subagents let the LLM invoke other agents as tools
- Agentic tools let an LLM orchestrate sub-tools within a single tool call
- Domain tools expose `@Tool` methods on objects the LLM works with
- Tool chaining dynamically exposes tools from returned artifacts
