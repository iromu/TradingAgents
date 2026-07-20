# Common Pitfalls — Embabel v1.0.0

## Annotation Pitfalls

### Missing `@Agent` / `@EmbabelComponent`

**Problem:** Forgetting `@Agent` on an agent class or `@EmbabelComponent` on a component that exposes actions/goals.

**Impact:** The class won't be registered as a Spring bean or discovered by the agent platform.

**Fix:** Annotate every agent with `@Agent(description = "...")` and every component with `@EmbabelComponent`.

### Missing `@Goal` on terminal actions

**Problem:** Not marking a terminal action with `@Goal` (v1.0.0 replaces the older `@AchievesGoal`).

**Impact:** The planner cannot determine goal satisfaction — the agent may loop indefinitely or complete without producing output.

**Fix:** Every agent needs at least one action marked with `@Goal(description = "...")`.

### Exposing sensitive methods without `@Tool` / `@LlmTool`

**Problem:** Relying on visibility modifiers (private, package-private) to hide methods from LLMs.

**Impact:** Annotated methods are safe; unannotated methods are never exposed regardless of visibility. But if a method *is* annotated with `@Tool` or `@LlmTool`, the LLM can call it — including methods that mutate state or leak data.

**Fix:** Explicitly annotate only the methods you want the LLM to call. Keep internal implementation details hidden.

## Action Pitfalls

### Missing `OperationContext` parameter

**Problem:** An `@Action` method that doesn't accept `OperationContext`.

**Impact:** The action cannot access the AI, blackboard, or invoke LLMs.

**Fix:** Include `OperationContext` as a parameter on every `@Action` method.

### Using `clearBlackboard` on goal-achieving actions

**Problem:** Setting `clearBlackboard = true` on an action annotated with `@Goal` / `@AchievesGoal`.

**Impact:** Clearing the blackboard removes `hasRun` tracking conditions, which may interfere with goal satisfaction detection.

**Fix:** Use `clearBlackboard = true` only on *intermediate* actions (e.g., preprocessing, loop-back actions). Never on the goal-achieving action.

### Ignoring `max-iterations`

**Problem:** Not configuring `toolloop.max-iterations` for complex agents.

**Impact:** Agent stops after 20 iterations (default), even if the goal isn't reached.

**Fix:** Increase via `embabel.agent.platform.toolloop.max-iterations` for complex multi-step agents.

### Using the default model everywhere

**Problem:** Not setting the model per-action.

**Impact:** Using the default model for everything wastes money or sacrifices quality.

**Fix:** Use `LlmOptions.withModel()` per action — cheaper models for simple tasks, stronger models for complex reasoning.

### Condition methods with side effects

**Problem:** Modifying the blackboard or having other side effects inside a `@Condition` method.

**Impact:** Conditions may be called multiple times, leading to inconsistent state.

**Fix:** `@Condition` methods must be pure — evaluate and return only.

## State Pitfalls

### Non-static inner classes for `@State`

**Problem:** Declaring a `@State` class as a non-static inner class (Java) or using `inner class` (Kotlin).

**Impact:** The class holds a reference to its enclosing instance, causing serialization and persistence failures. The framework throws `IllegalStateException` at startup.

**Fix:** Use Java records (implicitly static) or Kotlin top-level data classes.

### Forgetting `canRerun = true` when returning `this`

**Problem:** An action that returns `this` to stay in the current state without `canRerun = true`.

**Impact:** The action's `hasRun` flag prevents it from executing again, even though it returned `this`.

**Fix:** Add `@Action(canRerun = true)` for actions that return `this`.

### Missing `clearBlackboard = true` for looping states

**Problem:** An action that returns a new instance of the same `@State` type without `clearBlackboard = true`.

**Impact:** The planner sees the output type already exists on the blackboard and skips the action — the loop never executes.

**Fix:** Use `@Action(clearBlackboard = true)` on actions that loop back to a previously-visited state type.

### Not passing context through state records with `clearBlackboard`

**Problem:** Using `clearBlackboard = true` in a loop but not including all necessary data in the state record.

**Impact:** The blackboard is fully cleared — only the state record's fields survive.

**Fix:** Bundle all necessary context (user input, configuration, intermediate results) into the state record fields.

## Tool Pitfalls

### OneShotPerLoopTool without loop ID stamping

**Problem:** Wrapping a tool with `OneShotPerLoopTool` but not stamping a fresh `loopId` in `ToolCallContext` each turn.

**Impact:** Without a loop ID, `LoopMemo`'s fallback is "always emit" — the wrapper degrades to a passthrough and the tool is called every iteration instead of once per loop.

**Fix:** Stamp a fresh `UUID` as `ToolCallContext.LOOP_ID_KEY` in the `ToolCallContext` for every LLM call.

### Circular type dependencies

**Problem:** Action input/output types form a circular dependency (A → B → A with no entry point).

**Impact:** The planner can't find a valid plan path — the agent completes immediately with no actions.

**Fix:** Check that action input/output types form a valid directed acyclic graph. Use `ProcessOptions.verbosity` to debug planning.

## Testing Pitfalls

### Forgetting `.withId()` on LLM calls

**Problem:** Not using `.withId("...")` on `PromptRunner` calls.

**Impact:** Test verification becomes harder and debugging is opaque — you can't correlate LLM invocations with specific actions.

**Fix:** Always use `.withId("action-name")` for traceability. Essential for test assertions and log correlation.

### Not using `FakePromptRunner` for unit tests

**Problem:** Running integration tests against a real LLM.

**Impact:** Slow tests, non-deterministic results, API costs.

**Fix:** Use `FakePromptRunner` with `expectResponse()` for deterministic, fast unit tests.
---

*Source: Embabel Agent v1.0.0 documentation*
