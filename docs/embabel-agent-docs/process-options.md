Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.16. ProcessOptions
Agent processes can be configured with `ProcessOptions`.`ProcessOptions` controls:
- `contextId`: An identifier of any existing context in which the agent is running.
- `blackboard`: The blackboard to use for the agent.
Allows starting from a particular state.
- `test`: Whether the agent is running in test mode.
- `verbosity`: The verbosity level of the agent.
Allows fine grained control over logging prompts, LLM returns and detailed planning information
- `control`: Control options, determining whether the agent should be terminated as a last resort. `EarlyTerminationPolicy` can based on an absolute number of actions or a maximum budget.
- Delays: Both operations (actions) and tools can have delays.
This is useful to avoid rate limiting.
- `ephemaral`: blocks Agent Process from persisting in Agent Process Repository and from spawning child processes (light-weighted agent process)
- `toolCallContext`: Out-of-band metadata (e.g., auth tokens, tenant IDs, correlation IDs) passed to all tool invocations during the process.
This context propagates through the entire tool pipeline—including decorator chains and MCP tools—without being exposed to the LLM.
Set via `withToolCallContext()`:JavaKotlinSee Receiving Out-of-Band Context in Tools for how tools receive this context.