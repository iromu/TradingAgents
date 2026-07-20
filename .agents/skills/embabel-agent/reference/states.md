# States

States let GOAP plans express looping, branching, and multi-stage workflows that plain preconditions make awkward to model.

## How States Work with GOAP

Within each state, GOAP planning works normally — actions have preconditions (types they require) and effects (types they produce). The planner finds the optimal action sequence to reach the goal.

When an action returns a `@State`-annotated class, the framework:

1. **Hides previous state objects** — existing state objects are hidden from the blackboard
2. **Binds the new state object** — the returned instance is added to the blackboard
3. **Re-plans from the new state** — the planner considers only actions from the new state class
4. **Continues execution** — until a goal is reached or no plan can be found

**Context is preserved** across state transitions — non-state objects (user messages, customer data, conversation history) remain available. Only state objects are hidden, ensuring only the current state's actions are considered.

> **NOTE:** State transitions **hide** previous state objects but do **not clear** the blackboard. To clear the entire blackboard (e.g., for looping), use `clearBlackboard = true` on the action.

## When to Use States

- **Linear stages** where each stage naturally flows to the next
- **Branching workflows** where a decision point leads to different processing paths
- **Looping patterns** where processing may need to repeat (e.g., revise-and-review cycles)
- **Human-in-the-loop workflows** where user feedback determines the next state
- **Complex workflows** that are easier to reason about as discrete phases

States allow loopback to an entire state (which may contain multiple actions), which is more flexible than traditional GOAP looping that requires careful precondition management.

## Staying in the Current State

An action can return `this` to stay in the current state. Useful for actions that respond to inputs without changing state, such as chat handlers.

```java
@State
record ChitchatState(String context) {
    @Action(canRerun = true)  // required — actions run once per process by default
    ChitchatState respond(UserMessage message, Ai ai) {
        var response = ai.generateText("Respond to: " + message.content());
        return this;  // keeps same state instance active
    }
}
```

```kotlin
@State
data class ChitchatState(val context: String) {
    @Action(canRerun = true)  // required
    fun respond(message: UserMessage, ai: Ai): ChitchatState {
        val response = ai.generateText("Respond to: ${message.content()}")
        return this  // keeps same state instance active
    }
}
```

When an action returns `this`:
- The state remains active with no transition
- The blackboard is preserved
- The action can run again on subsequent planning cycles (requires `canRerun = true`)

## Looping States

For looping patterns where an action returns to a previously-visited state type, use `clearBlackboard = true`:

```java
@State
record ProcessingState(String data, int iteration) implements LoopOutcome {
    @Action(clearBlackboard = true)  // allows looping back to same state type
    LoopOutcome process() {
        if (iteration >= 3) {
            return new DoneState(data);  // terminal condition exits the loop
        }
        return new ProcessingState(data + "+", iteration + 1);  // new instance for another iteration
    }
}
```

Without `clearBlackboard = true`, the planner sees the output type already exists on the blackboard and skips the action. Clearing the blackboard resets the context, allowing natural loops.

> **TIP:** Only use `clearBlackboard = true` on actions that participate in loops. For linear state transitions, the default behavior (preserving the blackboard) is usually preferred.

## The @State Annotation

Classes returned from actions that should trigger state transitions must be annotated with `@State`:

```java
@State
record ProcessingState(String data) {
    @Action
    NextState process() {
        return new NextState(data.toUpperCase());
    }
}
```

### Behavior

When an action returns a `@State`-annotated class:

- Any previous state objects are **hidden** from the blackboard (not removed, but no longer visible)
- The returned object is bound to the blackboard (as `it`)
- Planning considers only actions defined within the **current** state class
- Any `@Goal` methods in the state become potential goals

Context (non-state objects) is preserved across state transitions — user messages, customer data, conversation history, etc. remain available.

## Inheritance

The `@State` annotation is inherited through the class hierarchy. Annotating a superclass or interface is enough — all subclasses and implementing classes are automatically state types.

```java
@State
interface Stage {}  // only the parent interface needs @State

record AssessStory(String content) implements Stage { ... }  // automatically a state type
record ReviseStory(String content) implements Stage { ... }
record Done(String content) implements Stage { ... }
```

This works with interfaces, abstract classes, concrete classes, and deep hierarchies.

### Parent State Interface Pattern

Define a parent interface that child states implement. Actions can return any implementation, enabling dynamic routing:

```java
@State
interface Stage {}

record AssessStory(String content) implements Stage {
    @Action
    Stage assess() {
        if (isAcceptable()) {
            return new Done(content);       // terminal branch
        } else {
            return new ReviseStory(content); // loop branch
        }
    }
}

record ReviseStory(String content) implements Stage {
    @Action
    AssessStory revise() {
        return new AssessStory(improvedContent());  // loop back
    }
}

record Done(String content) implements Stage {
    @Goal(description = "Processing complete")
    @Action
    Output complete() {
        return new Output(content);
    }
}
```

```kotlin
@State
interface Stage

data class AssessStory(val content: String) : Stage {
    @Action
    fun assess(): Stage {
        return if (isAcceptable()) {
            Done(content)               // terminal branch
        } else {
            ReviseStory(content)         // loop branch
        }
    }
}

data class ReviseStory(val content: String) : Stage {
    @Action
    fun revise(): AssessStory {
        return AssessStory(improvedContent())  // loop back
    }
}

data class Done(val content: String) : Stage {
    @Goal(description = "Processing complete")
    @Action
    fun complete(): Output {
        return Output(content)
    }
}
```

This pattern enables:
- **Polymorphic return types** — actions can return any implementation of the parent interface
- **Dynamic routing** — the runtime value determines which state is entered
- **Looping** — states can return other states that eventually loop back

The framework automatically discovers all implementations and registers their actions as potential next steps.

## Human-in-the-Loop with WaitFor

`WaitFor.formSubmission()` pauses agent execution until a user submits feedback:

```java
@State
record AssessStory(Story story) {
    @Action
    HumanFeedback getFeedback() {
        return WaitFor.formSubmission(
            "Please provide feedback on the story\n" + story.text,
            HumanFeedback.class
        );
    }
}
```

```kotlin
@State
data class AssessStory(val story: Story) {
    @Action
    fun getFeedback(): HumanFeedback {
        return WaitFor.formSubmission(
            "Please provide feedback on the story\n${story.text}",
            HumanFeedback::class.java
        )
    }
}
```

When the action executes, the agent enters a `WAITING` state, a form is generated from the target record structure, and the user fills it out. On submission, an instance of the target type is created and bound to the blackboard, and the agent resumes. The feedback stays within the current state until the next transition.

## Passing Data Through States

When using `clearBlackboard = true` for looping states, pass all necessary context through state record constructors since the blackboard is cleared:

```java
@State
record AssessStory(
    UserInput userInput,    // original user request
    Story story,            // current story draft
    Properties properties   // configuration
) implements Stage { ... }

@State
record ReviseStory(
    UserInput userInput,
    Story story,
    HumanFeedback humanFeedback,  // additional context for revision
    Properties properties
) implements Stage { ... }
```
---

*Source: Embabel Agent v1.0.0 documentation*
