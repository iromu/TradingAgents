# Invoking Embabel Agents

While many examples show Embabel agents being invoked via `UserInput` through the shell, they can also be invoked programmatically with strong typing. This is the most deterministic approach — code, rather than LLM assessment of user input, determines which agent is invoked and how.

## Creating an AgentProcess Programmatically

Create and execute agent processes directly using `AgentPlatform`:

```java
AgentProcess agentProcess = agentPlatform.createAgentProcess(
    myAgent, new ProcessOptions(), Map.of("input", userRequest)
);

// Start async and wait
Object result = agentPlatform.start(agentProcess).get();

// Or run synchronously
AgentProcess completedProcess = agentProcess.run();
MyResultType result = completedProcess.last(MyResultType.class);
```

```kotlin
val agentProcess = agentPlatform.createAgentProcess(
    agent = myAgent, processOptions = ProcessOptions(),
    bindings = mapOf("input" to userRequest)
)

val result = agentPlatform.start(agentProcess).get()

val completedProcess = agentProcess.run()
val result = completedProcess.last<MyResultType>()
```

Varargs creation (useful in web controllers):

```java
AgentProcess agentProcess = agentPlatform.createAgentProcessFrom(
    travelAgent, new ProcessOptions(), travelRequest, userPreferences
);
```

```kotlin
val agentProcess = agentPlatform.createAgentProcessFrom(
    agent = travelAgent, processOptions = ProcessOptions(),
    travelRequest, userPreferences
)
```

## Using AgentInvocation

`AgentInvocation` provides a higher-level, type-safe API. It automatically finds the appropriate agent by searching all registered agents for one whose goals produce the requested result type.

### Basic usage

```java
var invocation = AgentInvocation.create(agentPlatform, TravelPlan.class);
TravelPlan plan = invocation.invoke(travelRequest);
```

```kotlin
val invocation: AgentInvocation<TravelPlan> = AgentInvocation.create(agentPlatform)
val plan = invocation.invoke(travelRequest)
```

### Asynchronous invocation

```java
CompletableFuture<TravelPlan> future = invocation.invokeAsync(travelRequest);
future.thenAccept(plan -> logger.info("Plan generated: {}", plan));
TravelPlan plan = future.get();  // or block
```

```kotlin
val future: CompletableFuture<TravelPlan> = invocation.invokeAsync(travelRequest)
future.thenAccept { plan -> logger.info("Plan generated: {}", plan) }
val plan = future.get()
```

## Invocation with Named Inputs

```java
Map<String, Object> inputs = Map.of(
    "request", travelRequest, "preferences", userPreferences
);
TravelPlan plan = invocation.invoke(inputs);
```

```kotlin
val inputs = mapOf("request" to travelRequest, "preferences" to userPreferences)
val plan = invocation.invoke(inputs)
```

## Custom Process Options

### Verbosity

```java
var processOptions = new ProcessOptions()
    .withVerbosity(new Verbosity()
        .withShowPrompts(true)
        .withShowLlmResponses(true)
        .withDebug(true));

var invocation = AgentInvocation.builder(agentPlatform)
    .options(processOptions).build(TravelPlan.class);
TravelPlan plan = invocation.invoke(travelRequest);
```

```kotlin
val processOptions = ProcessOptions(
    verbosity = Verbosity(showPrompts = true, showLlmResponses = true, debug = true)
)
val invocation: AgentInvocation<TravelPlan> = AgentInvocation.builder(agentPlatform)
    .options(processOptions).build()
val plan = invocation.invoke(travelRequest)
```

> **NOTE:** Verbosity on `ProcessOptions` is inherited by `Subagent` invocations — set it once at the top level and it flows through the call tree. `GoalTool` and `AgentTool` default to `showPrompts = true` regardless of parent context.

### Budget

```java
var processOptions = new ProcessOptions(
    new Verbosity().withShowPrompts(true),
    new Budget(1000, 5000)  // min, max token budget
);
```

```kotlin
val processOptions = ProcessOptions(
    verbosity = Verbosity(showPrompts = true),
    budget = Budget(1000, 5000)
)
```

## Passing Tool Call Context at Invocation Time

Use `ProcessOptions.withToolCallContext()` to attach out-of-band metadata that flows through the entire agent run to every tool — including remote MCP tools (becomes MCP `_meta` on the wire).

```java
var processOptions = new ProcessOptions()
    .withToolCallContext(Map.of(
        "authToken",     request.getHeader("Authorization"),
        "tenantId",      request.getHeader("X-Tenant-Id"),
        "correlationId", UUID.randomUUID().toString()
    ));

var invocation = AgentInvocation.builder(agentPlatform)
    .options(processOptions).build(CustomerReport.class);
CustomerReport report = invocation.invoke(customerQuery);
```

```kotlin
val processOptions = ProcessOptions()
    .withToolCallContext(ToolCallContext.of(
        "authToken"     to request.getHeader("Authorization"),
        "tenantId"      to request.getHeader("X-Tenant-Id"),
        "correlationId" to UUID.randomUUID().toString(),
    ))

val invocation = AgentInvocation.builder(agentPlatform)
    .options(processOptions).build<CustomerReport>()
val report = invocation.invoke(customerQuery)
```

Context set here can be read by any `@LlmTool` method that declares a `ToolCallContext` parameter. It can also be supplemented per-interaction inside `@Action` methods using `PromptRunner.withToolCallContext()` — interaction-level values win on conflict.

## Confidence Thresholds

`Autonomy` uses configurable confidence thresholds. If no match exceeds the threshold, a `NoAgentFound` or `NoGoalFound` exception is thrown.

### Platform-level configuration

```properties
embabel.agent.platform.autonomy.agent-confidence-cut-off=0.6
embabel.agent.platform.autonomy.goal-confidence-cut-off=0.6
```

### Per-request override

```java
GoalSelectionOptions options = new GoalSelectionOptions(
    0.5,   // goalConfidenceCutOff — override platform default
    null,  // agentConfidenceCutOff — use platform default
    false  // multiGoal
);
```

```kotlin
val options = GoalSelectionOptions(
    goalConfidenceCutOff = 0.5,
    agentConfidenceCutOff = null,
    multiGoal = false
)
```

## Autonomy Modes

`Autonomy` uses an LLM to rank available agents or goals against user input and select the best match.

### Closed Mode (`chooseAndRunAgent`)

The LLM selects the most appropriate agent. The selected agent runs in isolation using only its own actions and goals.

```java
return autonomy.chooseAndRunAgent(userIntent, ProcessOptions.DEFAULT);
```

```kotlin
return autonomy.chooseAndRunAgent(userIntent, ProcessOptions.DEFAULT)
```

### Open Mode (`chooseAndAccomplishGoal`)

The LLM selects the most appropriate goal from all available goals, then assembles a dynamic agent from any available actions to achieve it.

```java
return autonomy.chooseAndAccomplishGoal(
    ProcessOptions.DEFAULT,
    GoalChoiceApprover.APPROVE_ALL,
    agentPlatform,
    Map.of("userInput", new UserInput(userIntent)),
    new GoalSelectionOptions()
);
```

```kotlin
return autonomy.chooseAndAccomplishGoal(
    ProcessOptions.DEFAULT,
    GoalChoiceApprover.APPROVE_ALL,
    agentPlatform,
    mapOf("userInput" to UserInput(userIntent)),
    GoalSelectionOptions()
)
```

### Goal Choice Approval

```java
// Approve only high-confidence matches
GoalChoiceApprover approver = GoalChoiceApprover.approveWithScoreOver(0.8);

// Custom approval logic
GoalChoiceApprover customApprover = request -> {
    if (request.getGoal().getName().contains("dangerous")) {
        return new GoalChoiceNotApproved("Dangerous goals require manual approval");
    }
    return GoalChoiceApproved.INSTANCE;
};
```

```kotlin
val approver = GoalChoiceApprover.approveWithScoreOver(0.8)

val customApprover = GoalChoiceApprover { request ->
    if (request.goal.name.contains("dangerous")) {
        GoalChoiceNotApproved("Dangerous goals require manual approval")
    } else {
        GoalChoiceApproved
    }
}
```

### Handling Selection Failures

```java
try {
    return autonomy.chooseAndRunAgent(userIntent, ProcessOptions.DEFAULT);
} catch (NoAgentFound e) {
    logger.info("No matching agent. Rankings: {}", e.getAgentRankings());
    return fallbackResponse();
} catch (NoGoalFound e) {
    logger.info("No matching goal. Rankings: {}", e.getGoalRankings());
    return fallbackResponse();
} catch (GoalNotApproved e) {
    logger.info("Goal not approved: {}", e.getReason());
    return requiresApprovalResponse();
}
```

```kotlin
try {
    return autonomy.chooseAndRunAgent(userIntent, ProcessOptions.DEFAULT)
} catch (e: NoAgentFound) {
    logger.info("No matching agent. Rankings: {}", e.agentRankings)
    return fallbackResponse()
} catch (e: NoGoalFound) {
    logger.info("No matching goal. Rankings: {}", e.goalRankings)
    return fallbackResponse()
} catch (e: GoalNotApproved) {
    logger.info("Goal not approved: {}", e.reason)
    return requiresApprovalResponse()
}
```

## When to Use Each Approach

| Approach | Best For |
|----------|----------|
| `AgentInvocation.invokeAsync()` | When you need a `CompletableFuture` for programmatic handling, chaining, or reactive integration |
| Direct `AgentProcess` creation | Webhooks, form submissions, or UI flows where you poll for status via REST/SSE |

## REST Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/process/{processId}` | GET | Process status, result, URLs |
| `/api/v1/process/{processId}` | DELETE | Terminate a running process |
| `/events/process/{processId}` | GET | SSE stream of process events |

Each endpoint can be individually disabled via configuration (set to `false` to return HTTP 404):

```properties
embabel.agent.platform.rest.process-status-enabled=false
embabel.agent.platform.rest.process-kill-enabled=false
embabel.agent.platform.rest.process-events-enabled=false
```

## ProcessOptions — Full Reference

`ProcessOptions` controls how an agent process runs. Key options:

| Option | Description |
|--------|-------------|
| `contextId` | Existing context for session state (pre-populates blackboard) |
| `blackboard` | Custom blackboard (start from particular state) |
| `test` | Test mode flag |
| `verbosity` | Control logging of prompts, LLM responses, planning details |
| `control` | Early termination (action count or budget limit) |
| `ephemeral` | Skip persistence, no child processes |
| `toolCallContext` | Out-of-band metadata for tool invocations |
| `plannerType` | Override planner type for this process |

```kotlin
val processOptions = ProcessOptions()
    .withContextId("session-123")
    .withEphemeral(true)
    .withVerbosity(Verbosity(showPrompts = true, showLlmResponses = true, debug = true))
    .withToolCallContext(ToolCallContext.of(
        "authToken" to bearerToken,
        "tenantId" to tenantId,
    ))
```

> **Verbosity inheritance:** Verbosity set on `ProcessOptions` is inherited by subagent invocations — set once at the top level and it flows through the call tree.

## ConcurrentAgentProcess

When `process-type: CONCURRENT` is configured, the platform uses `ConcurrentAgentProcess` which runs all achievable actions in parallel per tick using virtual threads.

### Behavior Differences from SimpleAgentProcess

- **ReplanRequestedException**: Only the first replan request is honored; subsequent ones are dropped
- **Blacklisted actions**: An action that is blacklisted is prevented from immediate re-execution in the next tick
- **Concurrency safety**: Actions must be safe to run concurrently — avoid shared mutable state

---

*Source: Embabel Agent v1.5.0 documentation*
