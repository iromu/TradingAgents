# Cost Tracking

Embabel emits an event for every LLM and embedding call your agent makes. Subscribe to those events to track real-time cost, model usage, and per-agent spend — then use that data to enforce budget guardrails that block the *next* call.

## Cost Tracking Overview

Two event types are available:

- **`LlmInvocationEvent`** — emitted once per LLM call.
- **`EmbeddingInvocationEvent`** — emitted once per embedding call.

Each event exposes:

| Field | Description |
|---|---|
| `invocation.llmMetadata` (or `embeddingMetadata`) | Model name and provider |
| `invocation.usage` | Token counts |
| `invocation.cost()` | Computed cost for that call |
| `interactionId` | Identifier of the originating interaction |
| `agentProcess` | The agent process that triggered the call (`agentProcess.id` to group, `agentProcess.agent.name` to label) |

## Listening for LlmInvocationEvent

Implement `AgenticEventListener` and react to the events you care about. The listener is registered like any other Embabel event listener.

```java
import io.emabel.agent.framework.events.AgenticEventListener;
import io.emabel.agent.framework.events.LlmInvocationEvent;
import io.emabel.agent.framework.events.AgentProcessEvent;
import java.util.concurrent.ConcurrentHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleAdder;

public class OrganizationCostTracker implements AgenticEventListener {

    private final ConcurrentHashMap<String, DoubleAdder> costPerAgent = new ConcurrentHashMap<>();

    @Override
    public void onProcessEvent(AgentProcessEvent event) {
        if (event instanceof LlmInvocationEvent llm) {
            costPerAgent
                .computeIfAbsent(llm.getAgentProcess().getAgent().getName(), k -> new DoubleAdder())
                .add(llm.getInvocation().cost());
        }
    }
}
```

**Best practices:**

- Use a thread-safe data structure (e.g. `ConcurrentHashMap` + `DoubleAdder`). Multiple agent processes may emit events concurrently.
- The same pattern works for `EmbeddingInvocationEvent`.
- Group by `agentProcess.id` for fine-grained accounting, or `agentProcess.agent.name` for a high-level label.

## Budget Management

Cost events fire **after** the call completes, so they cannot stop the call that just ran. What they can do is stop the *next* one.

The pattern combines two pieces:

1. **A listener that counts.** Subscribe to `LlmInvocationEvent` and accumulate cost or tokens against the key you care about — agent process id, tenant, or end user.
2. **A guardrail that blocks.** A `UserInputGuardRail` reads the counter before the next LLM call. If the budget is exceeded, the guardrail returns a `CRITICAL` validation error and the call never happens.

```
                    LLM call ───► LlmInvocationEvent ─┐
                                                       ▼
                                         counter (per agent / tenant / user)
                                                       │
  next call ──► UserInputGuardRail reads counter ──────┘
                               │
                      over budget? ──► CRITICAL ──► call blocked
```

### Budget Guardrail Example

```java
import io.emabel.agent.framework.guardrails.UserInputGuardRail;
import io.emabel.agent.framework.guardrails.ValidationResult;
import java.util.concurrent.ConcurrentHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleAdder;

public class BudgetGuardrail implements UserInputGuardRail {

    private final DoubleAdder totalSpend = new DoubleAdder();
    private final double budgetLimit;

    public BudgetGuardrail(double budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    // Called by the cost-tracking listener after each LLM call
    public void recordCost(double cost) {
        totalSpend.add(cost);
    }

    @Override
    public ValidationResult validate(String userInput) {
        if (totalSpend.sum() >= budgetLimit) {
            return ValidationResult.critical(
                "Budget exceeded: $" + String.format("%.2f", totalSpend.sum())
                + " / $" + String.format("%.2f", budgetLimit)
            );
        }
        return ValidationResult.valid();
    }
}
```

Wire the listener and guardrail into the same agent process. The listener accumulates cost; the guardrail reads it and blocks when over budget. Register the guardrail as a `UserInputGuardRail` (see Guardrails reference).

> **NOTE:** For a hard cap on the agent process itself (e.g. "stop this run after $1 of total spend"), see `EarlyTerminationPolicy`. Use it standalone or alongside the Budget Guardrail as a safety net.
---

*Source: Embabel Agent v1.0.0 documentation*
