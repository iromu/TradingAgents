# Planners Reference

## Planner Comparison

| Planner | Best For | Determinism | Algorithm |
|---------|----------|-------------|-----------|
| **GOAP** (default) | Business processes with defined outputs | High | A* search over goal states |
| **Utility** | Exploration, event-driven systems | Medium | Highest net-value action picks |
| **Hybrid** | Reducer pipelines | Medium-High | Utility picking + goal termination |
| **Supervisor** | Flexible multi-step workflows | Low | LLM-orchestrated composition |

## GOAP (Goal Oriented Action Planning)

The default planner. Uses A* search to find a sequence of actions that achieves the goal state.

### How it works
1. The planner examines the current state (bindings on the blackboard)
2. It identifies goals that haven't been achieved
3. It searches for actions whose postconditions satisfy the goal
4. It builds a plan and executes actions one by one
5. After each action, it replans from the new state

### When to use
- You have well-defined goals with clear success conditions
- You want deterministic, verifiable planning
- Your domain has a rich set of reusable actions

### Example

```java
@Agent(description = "Travel planning agent")
public class TravelAgent {

    @Action
    public FlightInfo searchFlights(Destination dest, Ai ai) { ... }

    @Action
    public HotelInfo searchHotels(FlightInfo flight, Ai ai) { ... }

    @AchievesGoal(description = "Complete travel plan with flight and hotel")
    @Action
    public TravelPlan compilePlan(FlightInfo flight, HotelInfo hotel, Ai ai) { ... }
}
```

## Utility AI

Selects the action with the highest **net value** (`value - cost`) from all available actions at each step.

### When to use
- Event-driven systems that react to incoming events
- Chatbots with multiple response options
- Exploration tasks where you want to discover what's possible

### Action Cost and Value

```java
@Action(cost = 0.1, value = 0.8)  // net value = 0.7
public Output highValueAction(Input input) { ... }
```

### States Pattern

```java
@State
public sealed interface TicketCategory permits CriticalTicket, BugTicket, GeneralTicket {}

@Action
public TicketCategory triageTicket(Ticket ticket) {
    if (ticket.description().contains("down")) {
        return new CriticalTicket(ticket);
    }
    // ...
}
```

## Hybrid Planner

Combines Utility AI's value-based action picking with goal-satisfaction termination.

### Two-Goal Pattern

```kotlin
val agent = Agent(
    name = "per-signal-triage",
    goals = setOf(
        NIRVANA,  // Keeps iteration alive
        Goal(name = "attention-candidate-produced", ...)  // Terminal goal
    ),
)
```

## Supervisor Planner

Uses an LLM to orchestrate actions dynamically. Non-deterministic.

### When to use
- Action ordering is context-dependent
- You want an LLM to synthesize across multiple sources
- Non-determinism is acceptable

### Example

```java
@Agent(planner = PlannerType.SUPERVISOR, description = "Market research report generator")
public class MarketResearchAgent {

    @Action(description = "Gather market data")
    public MarketData gatherMarketData(MarketDataRequest request, Ai ai) { ... }

    @Action(description = "Analyze competitors")
    public CompetitorAnalysis analyzeCompetitors(CompetitorAnalysisRequest request, Ai ai) { ... }

    @AchievesGoal(description = "Compile the final report")
    @Action(description = "Compile the final report")
    public FinalReport compileReport(ReportRequest request, Ai ai) { ... }
}
```

## Choosing a Planner

Ask yourself:

1. **Do I have well-defined goals with clear success conditions?**
   - Yes → GOAP
   - No → Utility or Supervisor

2. **Do I need deterministic, verifiable planning?**
   - Yes → GOAP or Hybrid
   - No → Supervisor

3. **Is this an event-driven system?**
   - Yes → Utility

4. **Do I need the LLM to make orchestration decisions?**
   - Yes → Supervisor

5. **Do I want iteration with a terminal condition?**
   - Yes → Hybrid
