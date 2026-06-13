# Error Handling Reference

Embabel provides several mechanisms for handling failures gracefully in agentic workflows.

## Retry on LLM Failure

`createObject()` automatically retries on failure. For custom retry logic:

```java
try {
    var result = context.ai().withDefaultLlm()
        .creating(Analysis.class)
        .fromPrompt("Analyze this data");
} catch (LlmInvocationException e) {
    // Retry with a different model or fallback
    var fallback = context.ai().withLlm(LlmOptions.withModel("gpt-4o-mini"))
        .creating(FallbackAnalysis.class)
        .fromPrompt("Analyze this data (fallback)");
}
```

## createObjectIfPossible

For non-critical LLM calls where null is acceptable:

```java
var result = context.ai().withDefaultLlm()
    .createObjectIfPossible(Analysis.class)
    .fromPrompt("Analyze this data");
// Returns null if the LLM fails, instead of throwing
```

## Guardrail Violations

Guardrails with `CRITICAL` severity throw `GuardRailViolationException`:

```java
try {
    context.ai().withDefaultLlm()
        .withGuardRails(new PiiGuardRail())
        .creating(Analysis.class)
        .fromPrompt("Analyze this data");
} catch (GuardRailViolationException e) {
    // Handle the violation — block, log, or escalate
    log.warn("Guardrail violation: {}", e.getMessage());
}
```

## EarlyTerminationPolicy

For hard process-level caps, combine cost tracking with early termination:

```java
var options = ProcessOptions.builder()
    .withEarlyTerminationPolicy(new EarlyTerminationPolicy() {
        @Override
        public boolean shouldTerminate(AgentProcess process) {
            return costListener.getCost(process.getId()) > 1.0; // $1 cap
        }
    })
    .build();
```

See **cost-tracking.md** for full budget guardrail pattern and cost listener setup.

## Process State Monitoring

Monitor process state changes for long-running agents:

```java
AgentProcess process = agentPlatform.createAgentProcess(myAgent, options, bindings);
agentPlatform.start(process);

// Poll for completion
while (true) {
    var state = process.getState();
    if (state == ProcessState.COMPLETED) {
        break;
    } else if (state == ProcessState.FAILED) {
        // Handle failure
        break;
    }
    Thread.sleep(1000);
}
```

### Process States

| State | Meaning |
|-------|---------|
| `NOT_STARTED` | Process created but not started |
| `RUNNING` | Process is executing |
| `COMPLETED` | Process finished successfully |
| `FAILED` | Process failed with an error |
| `TERMINATED` | Process was intentionally terminated |
| `KILLED` | Process was killed externally |
| `STUCK` | Process appears stuck (no progress) |
| `WAITING` | Process is waiting (e.g., human-in-the-loop) |
| `PAUSED` | Process was paused |

## Error Handling Best Practices

1. **Use `createObject()` for critical calls** — automatic retry on failure
2. **Use `createObjectIfPossible()` for non-critical calls** — graceful null return
3. **Handle `GuardRailViolationException`** — for safety/compliance blocks
4. **Set `EarlyTerminationPolicy`** — for hard cost/process-level caps
5. **Monitor process state** — for long-running agents
6. **Use `.withId()` on LLM calls** — for tracing errors back to specific actions
7. **Enable debug logging** — `embabel.agent.platform.logging.level: DEBUG` for troubleshooting

## Key Points

- `createObject()` retries automatically; `createObjectIfPossible()` returns null
- Guardrail violations with `CRITICAL` severity throw `GuardRailViolationException`
- Cost events fire **after** the call completes — they cannot stop the call that just ran
- Use a listener to accumulate costs, then a guardrail to block the **next** call
- For hard process-level caps, use `EarlyTerminationPolicy`
- Poll `process.getState()` for long-running agents
- Use `.withId()` on every LLM call for traceability
