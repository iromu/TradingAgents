# Annotation Model Reference

Detailed annotation patterns for Embabel agents. See SKILL.md for the core workflow.

## @Agent Annotation

```java
@Agent(description = "Writes and reviews stories")
public class WriteAndReviewAgent { ... }
```

Must provide `description` — used by the LLM in agent selection.

## @Agentic Annotation

```java
@Agentic(description = "Generates reports from data")
public class ReportAgent { ... }
```

Auto-registered via Spring component scanning (enabled by default via `embabel.agent.platform.scanning.annotation`).

## @EmbabelComponent — Action Container

```java
@EmbabelComponent
public class IssueActions {
    @Action(cost = 0.1, value = 0.8)
    public GHIssue saveNewIssue(GHIssue ghIssue, OperationContext context) { ... }
}
```

Useful with Utility AI planner that selects the most valuable next action among all available actions.

## @Action — Full Attribute Reference

```java
@Action(
    description = "Search for flights",
    pre = {"spel:destination != null"},
    post = {"flightInfo != null"},
    canRerun = false,
    readOnly = true,
    clearBlackboard = false,
    cost = 0.1,
    value = 0.5
)
public FlightInfo searchFlights(Destination dest, Ai ai) { ... }
```

### clearBlackboard

Useful in two scenarios:
1. **Multi-step workflows** where you want to reset the processing context
2. **Looping states** where an action returns to a previously-visited state type

> **Warning:** Avoid using `clearBlackboard` on goal-achieving actions (those with `@AchievesGoal`). Clearing the blackboard removes `hasRun` tracking conditions, which may interfere with goal satisfaction.

### Dynamic Cost Computation with @Cost

```java
@Cost(name = "computeCost")
public double computeCost(@Nullable GHIssue issue, Blackboard bb) {
    return issue != null ? 0.8 : 0.2;
}

@Action(costMethod = "computeCost")
public GHIssue saveIssue(GHIssue input, OperationContext context) { ... }
```

Key differences from `@Condition` methods:
- All domain object parameters in `@Cost` methods must be nullable
- When a domain object is not available on the blackboard, `null` is passed
- The method must return a `double` between 0.0 and 1.0
- The `Blackboard` can be passed as a parameter for direct access

Dynamic cost is especially useful with **Utility planning** (`PlannerType.UTILITY`), where cost/value tradeoffs are a core concept.

## @Condition Annotation

```java
@Condition
public boolean hasCriticalTicket(Ticket ticket) {
    return ticket.isCritical();
}
```

Conditions should not have side effects — they may be called multiple times.

### Dynamic SpEL Conditions

```java
@Action(pre = {
    "spel:urgency > 0.5",
    "spel:newEntity.newEntities.?[#this instanceof T(com.example.Issue)].size() > 0"
})
public void handleIssue(GHIssue issue, OperationContext context) { ... }
```

### Expression Syntax

SpEL expressions reference blackboard objects by their binding names (typically the camelCase form of the class name).

| Pattern | Description |
|---------|-------------|
| `spel:obj.property > value` | Simple property comparison |
| `spel:obj instanceof T(com.example.Type)` | Type checking |
| `spel:collection.size() > 0` | Check collection is not empty |
| `spel:collection.?[condition].size() > 0` | Check filtered collection |
| `spel:obj.property != null` | Null checking |
| `spel:condition1 && condition2` | Combine with AND |

> Use SpEL conditions for simple property checks. For complex logic or reusable conditions, prefer `@Condition` methods.

## @SecureAgentTool — Security

```java
@Agent
@SecureAgentTool(expression = "hasAuthority('news:read')")
public class NewsAgent { ... }
```

Method-level annotations override class-level. Requires `embabel-agent-mcp-security` starter.

## @Provided — Inject Platform Beans

```java
@Action
public TicketCategory triage(
        Ticket ticket,
        @Provided TicketFlow flow) {  // injected from Spring context
    return flow.process(ticket);
}
```

Use `@Provided` for services, configuration, or enclosing component references (especially in `@State` classes).

### When to Use @Provided

- Accessing the enclosing `@EmbabelComponent` or `@Agent` from a `@State` action
- Services that are infrastructure concerns, not domain objects
- Configuration or environment values

### Do NOT Use @Provided For

- Domain objects that should drive planning (use regular parameters)
- Objects that need to be tracked on the blackboard

Since `@State` classes must be static nested classes or top-level classes, `@Provided` is the recommended way to access the enclosing component's services.

## SomeOf — Union Return Types

```java
public record Classification(Intent intent) implements SomeOf { ... }
// Can return BillingIntent, SupportIntent, etc. — all are valid postconditions
```

Enables routing scenarios where an action returns one of several possible types. Multiple fields of the `SomeOf` instance may be non-null — this enables the most appropriate routing.

## @RequireNameMatch — Binding by Name

```java
@Action
public void process(@RequireNameMatch Thing thingOne) { ... }
// Requires a Thing named "thingOne" on the blackboard
```

## Reactive Triggers with trigger

The `trigger` field enables reactive behavior where an action only fires when a specific type is the **most recently added** value to the blackboard:

```java
@Action(trigger = UserMessage.class)
public void handleMessage(UserMessage msg, Conversation conv) { ... }
```

Without `trigger`, an action fires as soon as all its parameters are available. With `trigger`, the specified type must additionally be the most recent value added.

Useful when:
- You have multiple actions that could handle different event types
- You want to distinguish between "data available" and "event just occurred"
- You're building event-driven or reactive workflows

## Parameter Types

Action methods must have at least one parameter. Parameters fall in two categories:

### Domain Objects
Backed by the blackboard. Nullable parameters are populated if non-null on the blackboard.

### Infrastructure Parameters
`OperationContext`, `ProcessContext`, `Ai` — access blackboard and invoke LLMs.

### @Provided
Marked with `@Provided` — injected from Spring context rather than resolved from blackboard.

### Inheritance
Both Action and Condition methods may be inherited from superclasses.

## Action Method Implementation

Embabel makes it easy to seamlessly integrate LLM invocation and application code, using common types. An `@Action` method is a normal method, and can use any libraries or frameworks you like.

The only special thing about it is its ability to use the `OperationContext` parameter to access the blackboard and invoke LLMs.

## @AchievesGoal

The `@AchievesGoal` annotation can be added to an `@Action` method to indicate that the completion of the action achieves a specific goal.

Every agent needs at least one action marked with `@AchievesGoal` to define what constitutes completion.

## Key Points

- `@Agent` and `@Agentic` both register beans; `@Agentic` is auto-discovered
- `@EmbabelComponent` exposes actions without being an agent itself
- `clearBlackboard` is useful for looping states and context resets
- `@Cost` enables dynamic cost computation for Utility AI planning
- `@SecureAgentTool` provides MCP security with SpEL expressions
- `@Provided` injects from Spring context (essential for `@State` classes)
- `SomeOf` enables union return types for routing scenarios
- `trigger` enables reactive behavior (most-recently-added check)
- Method-level annotations override class-level for `@SecureAgentTool`
- Both Action and Condition methods can be inherited from superclasses
