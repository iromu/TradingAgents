# Troubleshooting Reference

Common issues when developing with Embabel agents and how to resolve them.

## Common Issues

### Agent Not Discovered

**Symptom:** The agent doesn't appear in the platform's agent list or can't be invoked by name.

**Fix:**
1. Ensure the class has `@Agent`, `@Agentic`, or is registered as an `@Bean` of type `Agent`
2. Verify the class is in a Spring component scan path
3. Check that `embabel.agent.platform.scanning.annotation` is `true` (default)

### Planner Can't Find a Plan

**Symptom:** Agent starts but immediately completes with no actions executed.

**Fix:**
1. Check that the input type matches an action's parameter type
2. Verify at least one action has `@Goal`
3. Ensure action postconditions form a valid chain to the goal
4. Enable debug logging: `embabel.agent.platform.logging.level: DEBUG`
5. Use `ProcessOptions.builder().withVerbose(true).build()`

### Blackboard Type Not Found

**Symptom:** `NoSuchElementException` when accessing blackboard by type.

**Fix:**
1. Ensure the type was added by a previous action in the flow
2. Check that the type isn't hidden (e.g., by a state transition)
3. Verify the type is nullable if it might not be present

### State Transitions Broken

**Symptom:** Agent stays in the same state or transitions incorrectly.

**Fix:**
1. Ensure state classes are annotated with `@State` (or inherit from a `@State`-annotated type)
2. Use `clearBlackboard = true` for looping states
3. Check that state classes are static nested classes (Java) or top-level classes (Kotlin)
4. Verify that the action returns an instance of the correct state type

### Custom Conditions Not Working

**Symptom:** Plans skip over custom conditions or never trigger them.

**Fix:**
1. Declare `post` conditions on `@Action` methods that may **set** the custom condition
2. Declare `pre` conditions on actions that depend on the custom condition
3. Otherwise the planner assumes the condition is never set

### LLM Calls Failing

**Symptom:** `LlmInvocationException`, timeout, or prompts look wrong.

**Fix:**
1. Check API key is set (`OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, etc.)
2. Verify the model name is valid for the provider
3. Increase timeouts: `embabel.agent.platform.http-client.read-timeout: 2m`
4. Check network connectivity to the provider
5. Write unit tests to capture and verify the exact prompts being sent

### High Costs

**Symptom:** Unexpectedly high LLM costs.

**Fix:**
1. Set `toolloop.max-iterations` to a reasonable limit (default 20)
2. Use cheaper models for non-critical actions (e.g., `gpt-4o-mini`)
3. Enable cost tracking with a `BudgetGuardRail`
4. Use `EarlyTerminationPolicy` for hard caps
5. Enable Anthropic prompt caching for repeated content

### Process Stuck

**Symptom:** Agent appears to hang or run for an unusually long time.

**Fix:**
1. Check `toolloop.max-iterations` — increase for complex agents
2. Enable debug logging to see planning decisions
3. Check for circular type dependencies between actions
4. Use `ProcessOptions.verbosity` to debug planning
5. Check `process.getState()` to see where the agent is stuck

### Missing Dependencies

**Symptom:** Compilation errors or `ClassNotFoundException` at runtime.

**Fix:**
1. Ensure all `com.embabel.agent` artifacts use the same version (check Maven/Gradle)
2. Version mismatches between Embabel modules cause compilation issues
3. Check that required tool groups are available in your environment

### Tools Not Available to Agent

**Symptom:** Agent fails when trying to use a tool.

**Fix:**
1. Specify tools via `withToolGroup()` or `withTools()` on your `PromptRunner`
2. Tools must be explicitly added to each LLM call that needs them
3. Check that required tool groups are available in your environment

### Agent Produces Poor Results

**Symptom:** Agent runs but outputs are low quality.

**Fix:**
1. Review prompt engineering and persona configuration
2. Adjust LLM temperature and model selection
3. Provide more context to actions
4. Write tests to capture actual vs expected outputs

### Struggling to Express Plan Logic

**Symptom:** Can't express desired flow with individual actions.

**Fix:**
1. Use custom conditions for complex flow control
2. Use builders like `ScatterGatherBuilder` and `RepeatUntilBuilder`
3. Familiarize yourself with Embabel's type-driven data flow concepts

### Agent Has No Goals

**Symptom:** Agent cannot execute — no terminal action found.

**Fix:**
1. Use `@Goal` on the terminal action that defines completion
2. Every agent needs at least one action marked with `@Goal`

### Agent Not Visible to MCP Client

**Symptom:** MCP clients (Claude Desktop, etc.) can't see the agent.

**Fix:**
1. Add `@Export(remote=true)` to the `@Goal` annotation
2. Verify MCP server is running and accessible
3. Check Docker configuration if using the default Docker MCP Gateway
4. Verify Spring AI MCP client configuration

### Upstream MCP Tools Error

**Symptom:** Agent can't use upstream MCP tools; errors about possible misconfiguration.

**Fix:**
1. Check Docker configuration if using the default Docker MCP Gateway
2. Verify Docker containers are running and accessible
3. For other MCP configs, ensure Spring AI MCP client configuration is correct

---

## Quick-Fix Table

| Issue | Quick Fix |
|-------|-----------|
| Agent not discovered | Add `@Agent` / `@Agentic` / `@Bean(Agent)` in component scan path |
| Planner can't find plan | Check `@Goal`, valid type chains, enable `DEBUG` logging |
| Blackboard type not found | Ensure type was added by a prior action; check nullability |
| State transitions broken | Annotate with `@State`; use static nested classes (Java); set `clearBlackboard` |
| Custom conditions not working | Add `post` conditions on setters, `pre` conditions on consumers |
| LLM calls failing | Check API key, model name, timeouts, network; test prompts |
| High costs | Limit `max-iterations`, use cheaper models, add `BudgetGuardRail` |
| Process stuck | Check `max-iterations`, circular deps, enable `DEBUG` logging |
| Missing dependencies | Pin all `com.embabel.agent` artifacts to the same version |
| Tools not available | Call `withToolGroup()` / `withTools()` on `PromptRunner` |
| Poor results | Tune persona, temperature, model, context; test actual vs expected |
| Can't express plan | Use `ScatterGatherBuilder`, `RepeatUntilBuilder`, custom conditions |
| No goals | Annotate terminal action with `@Goal` |
| MCP not visible | Add `@Export(remote=true)` on `@Goal` |
| MCP tools error | Verify Docker MCP Gateway or Spring AI MCP client config |

---

## Debug Logging

Customize Embabel logging in `application.yml` or `application.properties` to see detailed agent execution:

```yaml
logging:
  level:
    com.embabel.agent: DEBUG
```

### Useful Debugging Tips

- **Enable debug logging** — `com.embabel.agent: DEBUG` shows planning decisions
- **Use personality logging** — `embabel.agent.logging.personality: starwars` makes logs readable
- **Check process state** — `process.getState()` tells you where the agent is stuck
- **Use `.withId()`** — Tags every LLM call for easy identification in logs and tests
- **Monitor tool loop iterations** — If the agent is looping, check `toolloop.max-iterations`
- **Use `ProcessOptions.verbosity`** — Verbose planning output helps diagnose plan failures
- **Check blackboard contents** — Debug what types are available at each step
- **Write unit tests** — Capture and verify exact prompts and outputs for LLM debugging

### Getting Help

The Embabel community is active and helpful. Join their [Discord](https://discord.gg/t6bjkyj93q) server to ask questions and share experiences.
---

*Source: Embabel Agent v1.0.0 documentation*
