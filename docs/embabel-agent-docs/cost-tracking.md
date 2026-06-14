Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.29. Tracking LLM Cost and Usage
Embabel emits an event for every LLM and embedding call your agent makes.
Subscribe to those events to know, in real time, how much each call cost,
which model handled it, and which agent process it belongs to.
#### 4.29.1. The events
Two events are available:
- `LlmInvocationEvent` — emitted once per LLM call.
- `EmbeddingInvocationEvent` — emitted once per embedding call.Each event exposes:
- `invocation.llmMetadata` (or `embeddingMetadata`) — model name and provider
- `invocation.usage` — token counts
- `invocation.cost()` — computed cost for that call
- `interactionId` — identifier of the originating interaction
- `agentProcess` — the agent process that triggered the call (use `agentProcess.id` to group, `agentProcess.agent.name` to label)
#### 4.29.2. Subscribing to cost events
Implement `AgenticEventListener` and react to the events you care about.
The listener is registered like any other Embabel event listener.JavaKotlinThe same pattern works for `EmbeddingInvocationEvent`.
| | Use a thread-safe data structure (as above) for any state your listener accumulates. Several agent processes may emit events at the same time. |

#### 4.29.3. Blocking spending: the Budget Guardrail pattern
Cost events fire **after** the call completes, so they cannot stop the call that just ran.
What they can do is stop the **next** one.The pattern combines two pieces you already know:
1.**A listener that counts.** Subscribe to `LlmInvocationEvent` and accumulate cost or tokens against the key you care about — agent process id, tenant, end user.
1.**A guardrail that blocks.** A `UserInputGuardRail` reads the counter before the next LLM call. If the budget is exceeded, the guardrail returns a `CRITICAL` validation error and the call never happens.The counter lives in your listener; the decision lives in your guardrail.
Embabel wires both into the agent process for you.
See Working with Guardrails for how to register a `UserInputGuardRail` and how `CRITICAL` validation errors stop execution.
| | For a hard cap on the agent process itself (e.g. "stop this run after $1 of total spend"), see `EarlyTerminationPolicy` in The AgentProcess. Use it standalone, or alongside the Budget Guardrail as a safety net. |