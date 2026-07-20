# Planners Reference

Embabel supports multiple planning strategies. All are typesafe in Java and Kotlin.

## Choosing a Planner

| Planner | Best For | Determinism | Algorithm |
|---------|----------|-------------|-----------|
| **GOAP** (default) | Business processes with defined outputs | High | A\* search over goal states |
| **Utility** | Exploration, event-driven systems | Medium | Highest net-value action picks |
| **Hybrid** | Reducer pipelines | Medium-High | Utility picking + goal termination |
| **Supervisor** | Flexible multi-step workflows | Low | LLM-orchestrated composition |

**Decision flow:**

1. **Well-defined goals?** → GOAP
2. **Event-driven / no clear end goal?** → Utility
3. **Research-then-synthesize with clean termination?** → Hybrid
4. **LLM should decide action ordering?** → Supervisor

---

## GOAP (Goal-Oriented Action Planning)

Uses A\* search to find a sequence of actions that achieves the goal state from the current blackboard state.

### How it works

1. Examines bindings on the blackboard
2. Identifies goals not yet achieved
3. Searches for actions whose postconditions satisfy the goal
4. Builds and executes a plan, replanning after each action

### When to use

- Well-defined goals with clear success conditions
- Deterministic, verifiable planning is required

### Example

```java
@Agent(description = "Travel planning agent")
public class TravelAgent {

    @Action
    public FlightInfo searchFlights(Destination dest, Ai ai) { ... }

    @Action
    public HotelInfo searchHotels(FlightInfo flight, Ai ai) { ... }

    @Goal(description = "Complete travel plan with flight and hotel")
    @Action
    public TravelPlan compilePlan(FlightInfo flight, HotelInfo hotel, Ai ai) { ... }
}
```

---

## Utility AI

Selects the action with the highest **net value** (`value - cost`) from all available actions at each step. Unlike GOAP, which plans a path to a goal, Utility AI makes greedy decisions based on immediate value.

### When to use

- **Event-driven systems** — react to incoming events with the most appropriate action
- **Chatbots** — multiple response options, select the best one
- **Exploration** — discover what's possible rather than achieve a specific goal

### Setting Planner Type on `@Agent`

```java
@Agent(description = "Triage support tickets", planner = PlannerType.UTILITY)
public class TicketTriageAgent { ... }
```

```kotlin
@Agent(description = "Triage support tickets", planner = PlannerType.UTILITY)
class TicketTriageAgent { ... }
```

### Using Utility AI with `@EmbabelComponent`

For Utility AI, actions are typically provided via `@EmbabelComponent` rather than `@Agent`. This allows the platform to select actions across multiple components based on utility.

```java
@EmbabelComponent
public class IssueActions {

    @Action(outputBinding = "ghIssue")
    public GHIssue saveNewIssue(GHIssue ghIssue, OperationContext context) {
        if (communityDataManager.findIssueByGithubId(ghIssue.getId()) == null) {
            context.add(communityDataManager.saveAndExpandIssue(ghIssue));
            return ghIssue;
        }
        return null;  // prevents further actions
    }

    @Action(pre = {"spel:newEntity.newEntities.?[#this instanceof T(Issue)].size() > 0"})
    public IssueAssessment reactToNewIssue(GHIssue ghIssue, NewEntity<?> newEntity, Ai ai) {
        return ai.withLlm(properties.getTriageLlm())
            .creating(IssueAssessment.class)
            .fromTemplate("first_issue_response", Map.of("issue", ghIssue));
    }

    @Action(pre = {"spel:issueAssessment.urgency > 0.0"})
    public void heavyHitterIssue(GHIssue issue, IssueAssessment issueAssessment) { ... }
}
```

```kotlin
@EmbabelComponent
class IssueActions {

    @Action(outputBinding = "ghIssue")
    fun saveNewIssue(ghIssue: GHIssue, context: OperationContext): GHIssue? {
        if (communityDataManager.findIssueByGithubId(ghIssue.id) == null) {
            context += communityDataManager.saveAndExpandIssue(ghIssue)
            return ghIssue
        }
        return null  // prevents further actions
    }

    @Action(pre = ["spel:newEntity.newEntities.?[#this instanceof T(Issue)].size() > 0"])
    fun reactToNewIssue(ghIssue: GHIssue, newEntity: NewEntity<*>, ai: Ai): IssueAssessment {
        return ai.withLlm(properties.triageLlm)
            .creating(IssueAssessment::class.java)
            .fromTemplate("first_issue_response", mapOf("issue" to ghIssue))
    }

    @Action(pre = ["spel:issueAssessment.urgency > 0.0"])
    fun heavyHitterIssue(issue: GHIssue, issueAssessment: IssueAssessment) { ... }
}
```

---

## Hybrid

Combines Utility AI's value-based action picking with goal-satisfaction termination — the "iterate then stop" mode.

### The Two-Goal Pattern

Declare both `NIRVANA` (unsatisfiable, keeps iteration alive) and the real terminal goal:

```kotlin
import com.embabel.agent.core.support.NIRVANA

val agent = Agent(
    name = "per-signal-triage",
    provider = "example",
    version = "0.0.1",
    description = "Reducer pipeline: research → assess → wrap.",
    actions = listOf(/* … */),
    goals = setOf(
        NIRVANA,  // Keeps iterate-by-netValue picking alive
        Goal(
            name = "attention-candidate-produced",
            description = "An AttentionCandidate has been produced.",
            inputs = setOf(IoBinding("attentionCandidate", AttentionCandidate::class.java.name)),
            outputType = null,
        ),
    ),
)

val process = agentPlatform.runAgentFrom(
    agent = agent,
    processOptions = ProcessOptions(plannerType = PlannerType.HYBRID),
    bindings = signalBindings,
)
```

### When to use

- **Per-signal triage pipelines** — gather multiple context sources before a final assessment
- **Research-then-synthesize** — multiple actions contribute to a blackboard, one final action consumes everything
- **Extensible reducers** — new opportunistic actions can be added without rewriting the synthesis step

### Difference from `UTILITY`

`HYBRID` is `UTILITY` with one extra check: if the real goal is already satisfied, return an empty plan regardless of what actions remain — enabling clean termination of the two-goal pattern.

---

## Supervisor

Uses an LLM to orchestrate actions dynamically. **Non-deterministic** — the LLM may choose different sequences for the same inputs.

### Type-Informed vs Type-Driven

| Approach | Description |
|---|---|
| **Type-Driven** (GOAP) | Types *constrain* composition. Deterministic but rigid. |
| **Type-Informed** (Supervisor) | Types *inform* composition. LLM sees type schemas, decides semantically. |

Embabel's Supervisor is a **typed supervisor** — a middle ground between fully type-driven (GOAP) and untyped string-passing (typical LangGraph).

### When to use

- Action ordering is context-dependent and hard to predefine
- You want an LLM to synthesize information across multiple sources
- Non-determinism is acceptable

### When NOT to use

- You need reproducible, auditable execution paths
- Actions have strict dependency ordering that must be enforced
- Latency and cost matter (each decision requires an LLM call)

### Example

```java
@Agent(planner = PlannerType.SUPERVISOR, description = "Market research report generator")
public class MarketResearchAgent {

    @Action(description = "Gather market data including revenues and market share")
    public MarketData gatherMarketData(MarketDataRequest request, Ai ai) {
        return ai.withDefaultLlm().createObject(
            "Generate market data for: " + request.topic(), MarketData.class);
    }

    @Action(description = "Analyze competitors: strengths and positioning")
    public CompetitorAnalysis analyzeCompetitors(CompetitorAnalysisRequest request, Ai ai) {
        return ai.withDefaultLlm().createObject(
            "Analyze competitors: " + String.join(", ", request.companies()),
            CompetitorAnalysis.class);
    }

    @Goal(description = "Compile all information into a final report")
    @Action(description = "Compile the final report")
    public FinalReport compileReport(ReportRequest request, Ai ai) {
        return ai.withDefaultLlm().createObject(
            "Create a market research report for " + request.topic(), FinalReport.class);
    }
}
```

### Key Advantages over Typical Supervisor (e.g. LangGraph)

| Aspect | Typical Supervisor | Embabel Supervisor |
|---|---|---|
| Output Types | Strings — LLM must parse | Typed objects — validated |
| Tool Visibility | All tools always available | Tools filtered by blackboard state |
| Domain Awareness | None | Type schemas visible to LLM |
| Determinism | Fully non-deterministic | Semi-deterministic |

---

## Action Cost and Value

The `@Action` annotation supports `cost` and `value` parameters (both 0.0 to 1.0). Utility AI calculates **net value** as `value - cost` and selects the action with the highest net value.

```java
@Action(cost = 0.1, value = 0.8)  // net value = 0.7
public Output highValueAction(Input input) { ... }
```

```kotlin
@Action(cost = 0.1, value = 0.8)  // net value = 0.7
fun highValueAction(input: Input): Output { ... }
```

| Parameter | Range | Meaning |
|---|---|---|
| `cost` | 0.0 – 1.0 | Lower is cheaper |
| `value` | 0.0 – 1.0 | Higher is more valuable |

---

## Dynamic Cost Computation with `@Cost`

For cost (or value) that depends on the current blackboard state, use the `@Cost` annotation to compute values dynamically at planning time.

```java
@Agent(description = "Processor with dynamic cost")
public class DataProcessor {

    @Cost(name = "processingCost")  // <1>
    public double computeProcessingCost(@Nullable LargeDataSet data) {  // <2>
        if (data != null && data.size() > 1000) {
            return 0.9;  // High cost for large datasets
        }
        return 0.1;  // Low cost for small or missing datasets
    }

    @Action(costMethod = "processingCost")  // <3>
    public ProcessedData process(RawData input) {
        return new ProcessedData(input.transform());
    }
}
```

<1> `@Cost` marks a method for dynamic cost computation. The `name` parameter identifies it.
<2> Domain object parameters must be **nullable** — if the object isn't on the blackboard, `null` is passed.
<3> `costMethod` references the `@Cost` method by name.

You can also compute dynamic **value** using `valueMethod` in the same way.
---

*Source: Embabel Agent v1.0.0 documentation*
