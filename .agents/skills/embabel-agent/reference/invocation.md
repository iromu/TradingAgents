# Invocation Reference

## AgentProcess Lifecycle

An `AgentProcess` manages the complete execution lifecycle:

**Process States:**

- `NOT_STARTED` → `RUNNING` → `COMPLETED` / `FAILED` / `TERMINATED` / `KILLED` / `STUCK` / `WAITING` / `PAUSED`

**Execution Methods:**

- `tick()` — perform the next single step
- `run()` — execute as far as possible

## Programmatic Invocation

### With AgentProcess

```java
AgentProcess process = agentPlatform.createAgentProcess(myAgent, options, bindings);
Object result = agentPlatform.start(process).get();
```

Create processes with varargs input:

```java
AgentProcess process = agentPlatform.createAgentProcess(
    myAgent, options, inputObject, anotherObject);
```

### With AgentInvocation (Type-Safe)

```java
var invocation = AgentInvocation.create(agentPlatform, ReviewedStory.class);
ReviewedStory result = invocation.invoke(new UserInput("Tell me a story"));
```

Named inputs:

```java
var result = invocation.invoke(Map.of(
    "input", new UserInput("Tell me a story"),
    "mode", "creative"
));
```

Custom process options:

```java
var options = ProcessOptions.builder()
    .withVerbose(true)
    .withMaxIterations(30)
    .build();
```

### Asynchronous Invocation

```java
var future = invocation.invokeAsync(new UserInput("Tell me a story"));
// ... do other work ...
ReviewedStory result = future.get();
```

### Passing Tool Call Context

```java
var options = ProcessOptions.builder()
    .withToolCallContext(Map.of("tenantId", "acme", "authToken", "xyz"))
    .build();
```

Context flows through the entire agent run to every tool invoked, including remote MCP tools.

## Autonomy: Dynamic Agent and Goal Selection

### Closed Mode (LLM picks agent)

```java
var autonomy = new Autonomy(modelProvider);
var result = autonomy.chooseAndRunAgent(
    "Find a horoscope for Alice who is a Scorpio",
    ProcessOptions.DEFAULT,
    agentPlatform,
    Map.of("input", userInput),
    GoalSelectionOptions.DEFAULT
);
```

### Open Mode (LLM picks goal, assembles agent)

```java
var result = autonomy.chooseAndAccomplishGoal(
    ProcessOptions.DEFAULT,
    approver,
    agentPlatform,
    bindings,
    GoalSelectionOptions.DEFAULT
);
```

### Goal Choice Approval

```java
var result = autonomy.chooseAndAccomplishGoal(
    ProcessOptions.DEFAULT,
    (goal, confidence) -> {
        if (confidence < 0.8) {
            // Require human approval for low-confidence goals
            return askForApproval(goal);
        }
        return goal;
    },
    agentPlatform,
    bindings,
    GoalSelectionOptions.DEFAULT
);
```

### Confidence Thresholds

Configure in `application.yml`:

```yaml
embabel:
  agent:
    platform:
      autonomy:
        agent-confidence-cut-off: 0.6
        goal-confidence-cut-off: 0.6
```

Or override per-request:

```java
var options = GoalSelectionOptions.builder()
    .withAgentConfidenceCutOff(0.8)
    .withGoalConfidenceCutOff(0.7)
    .build();
```

## Shell Usage

The Embabel Shell uses Autonomy for natural language commands:

```bash
# Closed mode (default) - select best agent
execute "Find a horoscope for Alice who is a Scorpio"

# Open mode - select best goal, use any actions
execute "Find a horoscope for Alice" -o

# Show goal rankings without executing
choose-goal "Find a horoscope for Alice"
```

## REST Endpoints

Embabel exposes REST endpoints out of the box:

| Endpoint                      | Method | Description                  |
|-------------------------------|--------|------------------------------|
| `/api/v1/process/{processId}` | GET    | Process status, result, URLs |
| `/api/v1/process/{processId}` | DELETE | Kill process                 |
| `/events/process/{processId}` | GET    | SSE stream of events         |

Each can be individually disabled via configuration:

```yaml
embabel:
  agent:
    platform:
      rest:
        process-status-enabled: true
        process-kill-enabled: true
        process-events-enabled: true
```

## Webhook Integration

For webhook-triggered workflows:

```java
@PostMapping("/webhook")
public ResponseEntity<String> handleWebhook(@RequestBody WebhookEvent event) {
    var process = agentPlatform.createAgentProcess(
        myAgent, ProcessOptions.DEFAULT, event);
    agentPlatform.start(process);
    return ResponseEntity.ok(process.getId());
}
```

Poll `/api/v1/process/{processId}` or subscribe to SSE at `/events/process/{processId}`.

## Web Application Example (htmx)

```java
public class TravelPlanningController {

    private final AgentPlatform agentPlatform;
    private final Map<String, CompletableFuture<TravelPlan>> activeJobs = new ConcurrentHashMap<>();

    @PostMapping("/plan")
    public String startPlanning(@RequestParam String request, Model model) {
        var invocation = AgentInvocation.create(agentPlatform, TravelPlan.class);
        var future = invocation.invokeAsync(new UserInput(request));
        var processId = getProcessId(future);
        activeJobs.put(processId, future);
        return "status";  // htmx partial update
    }

    @GetMapping("/status/{processId}")
    public String getStatus(@PathVariable String processId, Model model) {
        var future = activeJobs.get(processId);
        if (future.isDone()) {
            activeJobs.remove(processId);
            model.addAttribute("result", future.get());
            return "result";
        }
        model.addAttribute("status", "running");
        return "status";
    }
}
```

## Blackboard

The blackboard is the shared memory system:

```java
blackboard.add("key", value);        // add with explicit name
blackboard.add(value);               // add with default name ("it")
blackboard.get(MyClass.class);       // get most recent by type
blackboard.get("key", MyClass.class); // get by name and type
blackboard.hide(MyClass.class);      // hide from planning
```

Most of the time, user code doesn't interact with the blackboard directly — action inputs come from it and outputs are
automatically added.

## Context (Cross-Process State)

Embabel's `Context` persists state across multiple agent processes:

```java
var options = ProcessOptions.builder()
    .withContextId("user-session-123")
    .build();
```

Context is identified by `contextId` and populated into each process's blackboard. Implementation depends on
`ContextRepository` — the default is in-memory only.

## Key Points

- `AgentInvocation` provides a type-safe, higher-level API than raw `AgentProcess`
- `Autonomy` enables LLM-powered dynamic agent/goal selection
- Closed mode = LLM picks one agent; Open mode = LLM assembles an agent from all available actions
- REST endpoints are exposed out of the box for status polling and SSE streaming
- Tool call context flows from process boundary to every tool invocation
- Context persists state across multiple agent processes (in-memory by default)
