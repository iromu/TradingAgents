# Common Pitfalls Reference

## Missing Agent Annotations

**Problem:** Forgetting `@Agent` or `@Agentic` on the class.

**Impact:** The agent won't be discovered by the platform.

**Fix:** Both annotations register beans, but `@Agentic` is auto-discovered via component scanning. Always annotate agent classes.

## Missing OperationContext

**Problem:** Not providing `OperationContext` as a parameter.

**Impact:** Actions can't access the AI or blackboard.

**Fix:** Every `@Action` method must include `OperationContext` as a parameter.

## Confusing Execution Modes

**Problem:** Mixing up execution modes (SIMPLE vs CONCURRENT) with autonomy modes (CLOSED vs OPEN).

**Impact:** Unexpected agent behavior — actions may run in parallel when you expect sequential, or the LLM may assemble agents incorrectly.

**Fix:**
- Execution mode (SIMPLE/CONCURRENT) controls how actions run within an agent
- Autonomy mode (CLOSED/OPEN) controls how the LLM selects agents
- These operate at different levels

## Missing @AchievesGoal

**Problem:** Not marking an action with `@AchievesGoal`.

**Impact:** The planner can't determine goal satisfaction — the agent may loop indefinitely or complete without producing output.

**Fix:** Every agent needs at least one action marked with `@AchievesGoal` to define what constitutes completion.

## Ignoring Tool Loop Iterations

**Problem:** Not setting `toolloop.max-iterations` for complex agents.

**Impact:** Agent stops after 20 iterations (default), even if the goal isn't reached.

**Fix:** Increase via `embabel.agent.platform.toolloop.max-iterations` for complex multi-step agents.

## Using Default Model Everywhere

**Problem:** Not setting model per-action.

**Impact:** Using the default model for everything wastes money or sacrifices quality.

**Fix:** Use `LlmOptions.withModel()` for each action — cheaper models for simple tasks, stronger models for complex reasoning.

## Forgetting .withId()

**Problem:** Not using `.withId()` on LLM calls.

**Impact:** Test verification becomes harder and debugging is opaque.

**Fix:** Always use `.withId("action-name")` for traceability. Essential for test assertions and log correlation.

## Non-Static Inner Classes for @State

**Problem:** Using non-static inner classes for `@State`-annotated types.

**Impact:** Serialization and persistence issues.

**Fix:** Use Java records (implicitly static) or Kotlin top-level classes.

## Missing clearBlackboard for Looping States

**Problem:** Not using `clearBlackboard = true` for looping states.

**Impact:** Planner sees output type already exists and skips the action — the loop never executes.

**Fix:** Use `@Action(clearBlackboard = true)` for revise-and-review or iterative loops.

## Exposing Sensitive Methods

**Problem:** Allowing LLMs to call sensitive methods.

**Impact:** LLM may call methods it shouldn't, leading to data leaks or unintended side effects.

**Fix:** Always gate with `@Tool`/`@LlmTool`; unannotated methods stay hidden regardless of visibility.

## Missing @Export on MCP Agents

**Problem:** Forgetting `@Export(remote=true)` on MCP agents.

**Impact:** Agents won't be visible to MCP clients like Claude Desktop.

**Fix:** Add `@Export(remote=true)` to agents you want to expose via MCP.

## Circular Type Dependencies

**Problem:** Action input/output types form a circular dependency.

**Impact:** Planner can't find a valid plan path — agent completes immediately with no actions.

**Fix:** Check that action input/output types form a valid plan path. Use `ProcessOptions.verbosity` to debug planning.

## Custom Conditions Without Postconditions

**Problem:** An action sets a custom condition but doesn't mark it in `post`.

**Impact:** Other actions that depend on it mark it in `pre` but the planner doesn't see it as satisfied.

**Fix:** If an action sets a custom condition, mark it in `post`; if another action depends on it, mark it in `pre`.

## Key Points

- Always annotate agent classes with `@Agent`, `@Agentic`, or register as `@Bean(Agent)`
- Every `@Action` needs `OperationContext` as a parameter
- Execution mode and autonomy mode operate at different levels
- Every agent needs at least one `@AchievesGoal` action
- Increase `max-iterations` for complex multi-step agents
- Use `LlmOptions.withModel()` per action for cost/quality optimization
- Always use `.withId()` for traceability in tests and logs
- Use Java records or Kotlin top-level classes for `@State` types
- Use `clearBlackboard = true` for looping states
- Gate all LLM-accessible methods with `@Tool`/`@LlmTool`
- Add `@Export(remote=true)` for MCP-exposed agents
- Check action type chains for circular dependencies
- Mark custom conditions in both `pre` and `post` as needed
