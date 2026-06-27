# Termination Reference

Graceful and immediate agent/action termination. See SKILL.md for the core workflow.

## Choosing Between Signal and Exception

| Mechanism | When to Use | Behavior |
|-----------|-------------|----------|
| **Graceful (Signal)** | "Let me finish my work, then stop" — side effects need to complete | Terminates at next checkpoint; current operation completes normally |
| **Immediate (Exception)** | "Stop now, nothing left to do" — no further processing needed | Terminates immediately; nothing executes after the exception |

## Agent Termination

### Graceful (Signal)

Use `terminateAgent()` when the current operation should complete before stopping:

```kotlin
@LlmTool(description = "Save all pending work and shutdown")
fun saveAndShutdown(ctx: ProcessContext): String {
    repository.saveAll(pendingRecords)  // side effect completes
    ctx.terminateAgent("All work saved, shutting down")
    return "Saved ${pendingRecords.size} records"  // tool finishes normally
}
```

### Immediate (Exception)

Use `TerminateAgentException` when the agent must stop immediately:

```kotlin
@LlmTool(description = "Validate critical prerequisites")
fun validatePrerequisites(): String {
    if (!hasPermission()) {
        throw TerminateAgentException("No permission to proceed")
    }
    return "Proceeding"
}
```

## Action Termination

Actions can be terminated early using `TerminateActionException` to skip the current action and let the planner re-evaluate.

## Key Points

- Graceful termination completes side effects; immediate does not
- Agent termination sets process status to `TERMINATED`
- Use signals for cleanup, exceptions for hard stops
- Action termination allows the planner to re-evaluate