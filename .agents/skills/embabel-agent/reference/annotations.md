# Annotation Model Reference

Embabel's Spring-style annotation model for agents, actions, goals, and conditions (Java/Kotlin).

## @Agent Annotation

Spring stereotype annotation that registers a class as an agent. Auto-registered via component scanning.

```java
@Agent(description = "Writes and reviews stories")
public class WriteAndReviewAgent { ... }
```

**Must provide `description`** — used by the LLM in agent selection.

## @Agentic Annotation

Sibling of `@Agent` for marking classes that participate in the agent framework.
Auto-registered via Spring component scanning (enabled by default via `embabel.agent.platform.scanning.annotation`).

```java
@Agentic(description = "Generates reports from data")
public class ReportAgent { ... }
```

## @EmbabelComponent — Action Container

Exposes actions, goals, and conditions for use by agents without being an agent itself. Most useful with the **Utility AI planner**.

```java
@EmbabelComponent
public class IssueActions {
    @Action(cost = 0.1, value = 0.8)
    public GHIssue saveNewIssue(GHIssue ghIssue, OperationContext context) { ... }
}
```

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

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `description` | String | — | Human-readable description |
| `pre` | String[] | — | Preconditions (SpEL or `@Condition` method names) |
| `post` | String[] | — | Postconditions (SpEL expressions) |
| `canRerun` | boolean | `false` | Whether the action can be rerun if already executed |
| `readOnly` | boolean | `false` | No external side effects (data analysis only) |
| `clearBlackboard` | boolean | `false` | Clear blackboard after completion, keeping only output |
| `cost` | double | `0.0` | Relative cost (0–1) |
| `value` | double | `0.0` | Relative value (0–1) |
| `costMethod` | String | — | Name of a `@Cost` method for dynamic cost |
| `valueMethod` | String | — | Name of a `@Cost` method for dynamic value |
| `trigger` | Class<?> | — | Reactive trigger — fires only when this type is most recently added |

### clearBlackboard

Useful for multi-step workflows (reset context) and looping states (return to a previously-visited state type):

```java
@State
record ProcessingState(String data, int iteration) {
    @Action(clearBlackboard = true)  // enables returning to same state type
    LoopOutcome process() {
        if (iteration >= 3) return new DoneState(data);
        return new ProcessingState(data + "+", iteration + 1);
    }
}
```

> **Warning:** Avoid `clearBlackboard = true` on `@Goal` methods — it removes `hasRun` tracking conditions, interfering with goal satisfaction. Use on intermediate actions only.

### Dynamic Cost Computation with @Cost

```java
@Cost(name = "processingCost")
public double computeProcessingCost(@Nullable LargeDataSet data) {
    if (data != null && data.size() > 1000) return 0.9;
    return 0.1;
}

@Action(costMethod = "processingCost")
public ProcessedData process(RawData input) {
    return new ProcessedData(input.transform());
}
```

## @Goal Annotation (replaces @AchievesGoal)

Marks an `@Action` method as achieving a specific goal. In Embabel v1.0.0, `@AchievesGoal` was renamed to `@Goal`.
For MCP publishing, use `@Export(remote = true)` on the `@Goal` method.

```java
@Agent(description = "Issue triage agent")
public class IssueTriageAgent {

    @Action
    public IssueAssessment assess(GHIssue issue, Ai ai) {
        return ai.withDefaultLlm()
                 .creating(IssueAssessment.class)
                 .fromTemplate("issue_triage", Map.of("issue", issue));
    }

    @Goal(description = "Escalate urgent issues")
    @Action
    public void escalateUrgent(IssueAssessment assessment, GHIssue issue) { ... }
}
```

Every agent needs at least one `@Goal` action to define completion.

For MCP publishing:

```java
@Goal(description = "Produce a curated news digest")
@Action(export = @Export(remote = true, name = "newsDigest",
                         startingInputTypes = {UserInput.class}))
public NewsDigest produceDigest(NewsTopic topic, OperationContext context) { ... }
```

## @Condition Annotation

Marks methods that evaluate conditions. May take an `OperationContext` parameter to access the blackboard.
If they take domain object parameters, the condition will automatically be false until suitable instances are available.

```java
@Condition
public boolean hasCriticalTicket(Ticket ticket) {
    return ticket.isCritical();
}
```

> **Important:** Condition methods should not have side effects — they may be called multiple times.

### Dynamic SpEL Conditions

Specify dynamic preconditions directly on `@Action` annotations using SpEL:

```java
@Action(pre = {"spel:issueAssessment.urgency > 0.0"})
public void escalateUrgentIssue(GHIssue issue, IssueAssessment issueAssessment) { ... }

@Action(pre = {"spel:ghIssue instanceof T(org.kohsuke.github.GHPullRequest) && ghIssue.changedFiles > 10"})
public void reviewLargePullRequest(GHPullRequest issue, PullRequestAssessment assessment) { ... }
```

### SpEL Collection Filtering

```java
@Action(pre = {
    "spel:newEntity.newEntities.?[#this instanceof T(com.example.Issue) " +
    "&& !(#this instanceof T(com.example.PullRequest))].size() > 0"
})
public IssueAssessment reactToNewIssue(GHIssue ghIssue, NewEntity<?> newEntity, Ai ai) { ... }
```

| SpEL Pattern | Description |
|--------------|-------------|
| `spel:obj.property > value` | Simple property comparison |
| `spel:obj instanceof T(com.example.Type)` | Type checking |
| `spel:collection.size() > 0` | Check collection is not empty |
| `spel:collection.?[condition].size() > 0` | Check filtered collection |

> Use SpEL for simple property checks. For complex logic or reusable conditions, prefer `@Condition` methods.

## @Cost — Dynamic Cost/Value Computation

Marks a method that returns a cost value (a `double` between 0.0 and 1.0).
Referenced from `@Action` via `costMethod` or `valueMethod`.

Key rules: all domain object parameters must be **nullable** (`null` if not on blackboard), the method must return a `double` between 0.0 and 1.0, and the `Blackboard` can be passed as a parameter for direct access.

```java
@Cost(name = "urgencyValue")
public double computeUrgency(@Nullable Task task) {
    if (task == null) return 0.5;
    if (task.getPriority() == Priority.HIGH) return 1.0;
    return 0.2;
}

@Action(valueMethod = "urgencyValue")
public Result processTask(Task task) { ... }
```

## @Export — MCP Publishing

Used on `@Goal` methods to control MCP tool publishing.

```java
@Goal(description = "Produce a curated news digest")
@Action(export = @Export(remote = true, name = "newsDigest",
                         startingInputTypes = {UserInput.class}))
public NewsDigest produceDigest(NewsTopic topic, OperationContext context) { ... }
```

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `remote` | boolean | `false` | Publish as a remote MCP tool |
| `name` | String | — | Custom MCP tool name (defaults to action name) |
| `startingInputTypes` | Class<?>[] | — | Types that can start this goal chain |

## @RequireNameMatch — Binding by Name

Binds action parameters by declared name on the blackboard rather than by type.

```java
@Action
public void process(@RequireNameMatch Thing thingOne) { ... }
// Requires a Thing named "thingOne" on the blackboard
```

## Reactive Triggers with `trigger`

The `trigger` field on `@Action` enables reactive behavior — an action only fires when a specific type is the **most recently added** value to the blackboard.

```java
@Agent(description = "Chat message handler")
public class ChatAgent {

    @Goal(description = "Respond to user message")
    @Action(trigger = UserMessage.class)
    public Response handleMessage(UserMessage message, Conversation conversation) {
        return new Response("Received: " + message.content());
    }
}
```

Without `trigger`, an action fires as soon as all parameters are available. With `trigger`, the specified type must additionally be the most recent value added.

Useful when:
- You have multiple actions handling different event types
- You want to distinguish "data available" from "event just occurred"
- Building event-driven or reactive workflows

## @Provided — Inject Platform Beans

Marks an action method parameter as provided by Spring context rather than resolved from the blackboard.

```java
@EmbabelComponent
public class ReservationFlow {

    @State
    public record CollectDetails(String customerId) {

        @Action
        public ConfirmReservation confirm(
                ReservationDetails details,            // domain object from blackboard
                @Provided ReservationFlow flow         // injected from Spring context
        ) {
            var booking = flow.bookingService.reserve(details);
            return new ConfirmReservation(booking);
        }
    }
}
```

### When to Use @Provided
- Accessing the enclosing `@EmbabelComponent` or `@Agent` from a `@State` action
- Infrastructure services, configuration, or environment values

### Do NOT Use @Provided For
- Domain objects that should drive planning (use regular parameters)
- Objects that need to be tracked on the blackboard

Since `@State` classes must be static nested or top-level classes, `@Provided` is the recommended way to access the enclosing component's services.

## Common Pitfalls

1. **`@Goal` not `@AchievesGoal`** — Embabel v1.0.0 renamed `@AchievesGoal` to `@Goal`. Use `@Goal` everywhere.

2. **`clearBlackboard` on goal actions** — Avoid using `clearBlackboard = true` on `@Goal` methods. Clearing removes `hasRun` tracking conditions, interfering with goal satisfaction.

3. **Nullable parameters in `@Cost`** — All domain object parameters in `@Cost` methods must be nullable. `null` is passed when the object is not on the blackboard.

4. **`@Cost` returns `double`** — The method must return a `double` between 0.0 and 1.0.

5. **SpEL binding names** — SpEL expressions reference blackboard objects by their camelCase class name. Custom binding names override this.

6. **Condition side effects** — `@Condition` methods may be called multiple times. Do not mutate state inside them.

7. **`@Provided` vs blackboard** — `@Provided` takes precedence over blackboard resolution. Use it only for infrastructure beans, not domain objects.

8. **Unique method names** — Give `@Action` and `@Condition` methods unique names so the planner can distinguish between them.

9. **`@Export` on `@Goal`** — Use `@Export(remote = true)` on `@Goal` methods for MCP publishing, not on regular `@Action` methods.

10. **`readOnly` actions** — `readOnly = true` actions only analyze data without modifying external systems (APIs, databases, files). Useful for learning/catchup modes.
---

*Source: Embabel Agent v1.0.0 documentation*
