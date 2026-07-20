# Termination

Embabel provides three mechanisms to terminate agents and actions early: graceful signals, immediate exceptions, and process-level policies.

## Graceful Termination

Graceful termination signals the agent to **finish the current operation**, then stop at the next checkpoint. Use when side effects must complete before shutdown.

### Agent-level: `terminateAgent(reason)`

Stops the entire agent process after the current tool call completes.

```java
@LlmTool(description = "Save all pending work and shutdown")
public String saveAndShutdown(ProcessContext ctx) {
    repository.saveAll(pendingRecords);  // side effect completes
    ctx.terminateAgent("All work saved, shutting down");
    return "Saved " + pendingRecords.size() + " records";  // tool finishes normally
}
```

### Action-level: `terminateAction(reason)`

Stops only the current action; the agent continues with the next planned action. The action must be defined with `canRerun = true` for graceful action termination to allow retry.

```java
import static com.embabel.agent.api.termination.Termination.terminateAction;

@Action
public String firstAction(UserInput input, ActionContext context) {
    context.set("firstActionRan", true);

    // Graceful action termination via static import
    terminateAction(context.getProcessContext(), "Save complete, no more work needed");

    return "first-" + input.getContent();
}
```

> **NOTE:** Graceful action termination only works for LLM-based actions that use a tool loop. For simple transformation actions, use `TerminateActionException` instead.

## Immediate Termination

Immediate termination stops execution **right now** by throwing an exception. No further tool calls, actions, or code after the throw point execute. Use for critical errors where no recovery is possible.

### Agent-level: `TerminateAgentException`

```java
@LlmTool(description = "Validate critical prerequisites")
public String validatePrerequisites() {
    if (!authService.hasRequiredPermissions()) {
        throw new TerminateAgentException("Missing required permissions");
        // nothing after this runs
    }
    return "Prerequisites validated";
}
```

### Action-level: `TerminateActionException`

```java
@LlmTool(description = "Check service health")
public String checkHealth() {
    if (!mcpClient.isConnected("required_service")) {
        throw new TerminateActionException("Service unavailable");
        // nothing after this runs
    }
    return "Healthy";
}
```

### Catching Both Types

Both exception types extend `TerminationException`, allowing unified handling:

```java
try {
    tool.execute();
} catch (TerminationException e) {
    logger.info("Terminated: " + e.getReason());
    // Handle both agent and action termination
}
```

## Early Termination Policy

`EarlyTerminationPolicy` is a process-level control option in `ProcessOptions` that terminates the entire agent process as a last resort. It can be configured based on:

- **Absolute number of actions** — stop after N actions
- **Maximum budget** — stop after a dollar/spend cap is reached

```java
var processOptions = ProcessOptions.builder()
    .control(new EarlyTerminationPolicy(
        /* maxActions */ 100,
        /* maxBudget */ new Money(10.00, CurrencyUnit.USD)
    ))
    .build();
```

Use `EarlyTerminationPolicy` standalone or alongside the Budget Guardrail as a safety net. See [Cost Tracking](../cost-tracking.md) for the `Budget Guardrail` complement.

## Decision Matrix

| Scope | Mechanism | Method/Exception | When to Use |
|-------|-----------|------------------|-------------|
| **Agent** | Graceful | `ctx.terminateAgent(reason)` | "Finish current work, then stop" — side effects must complete |
| **Agent** | Immediate | `throw TerminateAgentException(reason)` | "Stop now" — critical error, no recovery |
| **Action** | Graceful | `terminateAction(ctx, reason)` | "Finish current tool, then stop action" — allow next action |
| **Action** | Immediate | `throw TerminateActionException(reason)` | "Stop now" — try a different approach |
| **Process** | Policy | `EarlyTerminationPolicy` | Hard cap on actions or budget — last-resort safeguard |
---

*Source: Embabel Agent v1.0.0 documentation*
