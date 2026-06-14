Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.15. Execution Modes
Embabel supports two execution modes for agent processes, controlled by the
`embabel.agent.platform.process-type` property.
#### 4.15.1. SimpleAgentProcess (Default)
`SimpleAgentProcess` is the default execution mode.
On each planning tick it selects the single best action from the current plan
and runs it to completion before replanning.This sequential approach is predictable and easy to reason about:
actions never run in parallel and the blackboard is always in a consistent state
when the next action begins.
#### 4.15.2. ConcurrentAgentProcess
`ConcurrentAgentProcess` extends `SimpleAgentProcess` and runs **all currently
achievable actions in parallel** on each planning tick.Instead of picking one action per tick, it finds every action in the plan that
is achievable given the current world state and launches them concurrently using
the platform’s `Asyncer` abstraction (backed by Spring’s managed task executor
with virtual threads).
Once all launched actions have completed the process replans, and the cycle repeats.This is useful when an agent has independent sub-tasks that can proceed simultaneously—
for example, enriching multiple data items in parallel, or running several
analysis steps whose outputs do not depend on each other.
##### Activating ConcurrentAgentProcess
Set the following property in your application configuration:or in `application.properties`:The default value is `SIMPLE`.
##### Replanning in Concurrent Mode
Both `SimpleAgentProcess` and `ConcurrentAgentProcess` support
`ReplanRequestedException`, which an action can throw to signal that the agent
should update the blackboard and replan before proceeding.In `ConcurrentAgentProcess`, multiple concurrently running actions may throw
`ReplanRequestedException` at the same time.
When this happens only the first request is honoured—its blackboard updates
are applied and the triggering action is blacklisted for the next planning
cycle to prevent an immediate infinite loop.
The remaining requests are silently dropped, as they ran in a context that is
about to be replanned anyway.The blacklist is cleared automatically after a successful planning cycle.
If no plan can be found while a blacklist is active, the blacklist is cleared
and planning is retried, ensuring the agent does not become permanently stuck.
##### Choosing an Execution Mode

| Mode | When to use | Trade-offs || `SIMPLE` | Most agents; sequential pipelines; when action order matters | Predictable; easier to debug; no concurrency overhead || `CONCURRENT` | Independent parallel sub-tasks; fan-out/fan-in patterns; throughput-sensitive workloads | Higher throughput; requires actions to be safe to run concurrently against a shared blackboard |