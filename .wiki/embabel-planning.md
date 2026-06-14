---
title: "Emabel Planning Strategies"
type: "reference"
status: "active"
language: "default"
source_paths: ["docs/embabel-agent-docs/planners.md"]
updated_at: "2026-06-14"
---

# Embabel Planning Strategies

Embabel supports multiple planning strategies. All are typesafe in Java or Kotlin.

## GOAP (Goal-Oriented Action Planning) — Default

Plans a path from current state to goal using preconditions and effects. Deterministic and auditable.

- Best for: Business processes with defined outputs
- Actions have declared preconditions and effects based on type availability
- Strict dependency ordering enforced by types

## Utility AI

Selects the action with the highest **net value** (`value - cost`) from all available actions at each step. Greedy, exploratory.

- Best for: Exploration and event-driven systems
- No predetermined goal — reacts to incoming events
- Actions annotated with `cost` (0.0–1.0) and `value` (0.0–1.0)
- Can use `NIRVANA` goal (unsatisfiable) to keep process running indefinitely
- Often used with `@EmbabelComponent` rather than `@Agent`

### Utility AI with States

Combine with `@State` annotation for classification and routing:
1. An entry action classifies input and returns a `@State` instance
2. Each `@State` class contains an `@AchievesGoal` action producing final output
3. The `@AchievesGoal` output must not be a `@State` type (prevents infinite loops)

### UtilityInvocation

Lightweight utility pattern — fluent API to run utility-based workflows directly from `@EmbabelComponent` actions without creating a full `@Agent` class.

## Hybrid

Combines Utility AI's value-based action picking with goal-satisfaction termination. The "iterate then stop" mode.

- Best for: Reducer pipelines (gather many context-producing actions, run one synthesizer, stop)
- Uses **two-goal pattern**: real terminal goal + `NIRVANA` (unsatisfiable)
- While research is profitable, NIRVANA's plan wins. Once real goal is satisfied, empty plan wins and process terminates
- Unlike GOAP, fires opportunistic actions not on the cheapest plan path
- Unlike pure Utility, doesn't keep burning compute after goal is reached

### When to Use Each

| Scenario | Planner |
|----------|---------|
| Strict typed-dependency ordering, no opportunistic research | GOAP |
| Per-signal triage, gather context then synthesize | Hybrid |
| Genuine event loop with no terminal goal | Utility |
| Context-dependent action ordering, flexible composition | Supervisor |

## Supervisor

LLM-orchestrated composition. An LLM selects which actions to call based on type schemas and gathered artifacts.

- Best for: Flexible multi-step workflows
- **Non-deterministic** — LLM may choose different sequences for same inputs
- **Type-informed** (not type-driven) — types inform but don't constrain composition
- Actions return **typed outputs** that are validated
- LLM sees **type schemas** to understand what each action produces
- Results stored on **typed blackboard** for later actions

### Key Advantages Over Typical Supervisor (e.g., LangGraph)

| Aspect | Typical (LangGraph) | Embabel |
|--------|---------------------|---------|
| Output Types | Strings | Typed objects, validated |
| Tool Visibility | All tools always available | Filtered by blackboard state (currying) |
| Domain Awareness | None | Type schemas visible to LLM |
| State | Untyped message history | Typed blackboard with named artifacts |

### Curried Tool Filtering

When an action's inputs are already on the blackboard, those parameters are "curried out" — the tool signature simplifies. This reduces the LLM's decision space and signals which tools are "ready" to run.

### SupervisorInvocation

Lightweight supervisor pattern — fluent API to run supervisor-orchestrated workflows directly from `@EmbabelComponent` actions without creating a full `@Agent` class.

## Action Cost and Value

The `@Action` annotation supports `cost` and `value` parameters (both 0.0 to 1.0):

```java
@Action(cost = 0.3, value = 0.8)
```

Net value = `value - cost`. The Utility planner selects the action with the highest net value from all available actions.
