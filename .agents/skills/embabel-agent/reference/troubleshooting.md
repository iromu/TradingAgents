# Troubleshooting Reference

Common issues when working with Embabel agents and how to resolve them.

## Agent Not Discovered

**Symptom:** Agent doesn't appear in the platform's agent list.

**Fix:**
1. Ensure the class has `@Agent`, `@Agentic`, or is registered as an `@Bean` of type `Agent`
2. Check that `embabel.agent.platform.scanning.annotation` is `true` (default)
3. Verify the class is in a Spring component scan path

## Planner Can't Find a Plan

**Symptom:** Agent starts but immediately completes with no actions executed.

**Fix:**
1. Check that the input type matches an action's parameter type
2. Verify that at least one action has `@AchievesGoal`
3. Check that action postconditions form a valid chain to the goal
4. Enable debug logging: `embabel.agent.platform.logging.level: DEBUG`
5. Use `ProcessOptions.builder().withVerbose(true).build()`

## Blackboard Type Not Found

**Symptom:** `NoSuchElementException` when accessing blackboard by type.

**Fix:**
1. Ensure the type was added by a previous action
2. Check that the type isn't hidden (e.g., by a state transition)
3. Verify the type is nullable if it might not be present

## State Transitions Not Working

**Symptom:** Agent stays in the same state or transitions incorrectly.

**Fix:**
1. Ensure state classes are annotated with `@State` (or inherit from a `@State`-annotated type)
2. Use `clearBlackboard = true` for looping states
3. Check that state classes are static nested classes (Java) or top-level classes (Kotlin)
4. Verify that the action returns an instance of the correct state type

## LLM Calls Failing

**Symptom:** `LlmInvocationException` or timeout.

**Fix:**
1. Check API key is set (`OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, etc.)
2. Verify the model name is valid for the provider
3. Increase timeouts: `embabel.agent.platform.http-client.read-timeout: 2m`
4. Check network connectivity to the provider
5. Enable debug logging to see the full prompt

## High Costs

**Symptom:** Unexpectedly high LLM costs.

**Fix:**
1. Set `toolloop.max-iterations` to a reasonable limit (default 20)
2. Use cheaper models for non-critical actions (`gpt-4o-mini`)
3. Enable cost tracking with a `BudgetGuardRail`
4. Use `EarlyTerminationPolicy` for hard caps
5. Enable Anthropic prompt caching for repeated content

## Tool Loop Infinite Loop

**Symptom:** Agent runs for many iterations without making progress.

**Fix:**
1. Check `toolloop.max-iterations` — increase for complex agents
2. Verify action postconditions form a valid plan chain
3. Check for circular type dependencies between actions
4. Enable debug logging to see planning decisions
5. Use `ProcessOptions.verbosity` to debug planning

## Concurrent Mode Issues

**Symptom:** Actions don't run in parallel or results are inconsistent.

**Fix:**
1. Verify `embabel.agent.platform.process-type: CONCURRENT` is set
2. Ensure actions are truly independent (no shared mutable state)
3. Check that gather/fan-in logic handles partial results
4. Use `ProcessOptions` to verify mode at runtime

## MCP Agents Not Visible

**Symptom:** MCP clients (Claude Desktop, etc.) can't see the agent.

**Fix:**
1. Add `@Export(remote=true)` on the agent class
2. Verify MCP server is running and accessible
3. Check SSE endpoint: `curl -i http://localhost:1337/sse`
4. Verify MCP client configuration (transport type, URL)

## Guide Server Issues

**Symptom:** Guide server starts but can't answer questions.

**Fix:**
1. Load references: `curl -X POST http://localhost:1337/api/v1/data/load-references`
2. Check RAG storage: `curl http://localhost:1337/api/v1/data/stats`
3. Verify Neo4j or FalkorDB is running
4. Check `OPENAI_API_KEY` is set

## Debugging Tips

- **Enable debug logging** — `embabel.agent.platform.logging.level: DEBUG` shows planning decisions
- **Use personality logging** — `embabel.agent.logging.personality: starwars` makes logs readable
- **Check process state** — `process.getState()` tells you where the agent is stuck
- **Use `.withId()`** — Tags every LLM call for easy identification in logs and tests
- **Monitor tool loop iterations** — If the agent is looping, check `toolloop.max-iterations`
- **Use `ProcessOptions.verbosity`** — Verbose planning output helps diagnose plan failures
- **Check blackboard contents** — Debug what types are available at each step

## Key Points

- Agent discovery requires `@Agent`, `@Agentic`, or `@Bean(Agent)` in component scan path
- Planner failures usually mean missing `@AchievesGoal` or invalid type chains
- State transitions require `@State` annotation and proper class structure
- LLM failures often stem from missing API keys or invalid model names
- High costs are controlled via `max-iterations`, cheaper models, and guardrails
- MCP agents require `@Export(remote=true)` to be visible to clients
- Debug logging and `.withId()` are essential troubleshooting tools
