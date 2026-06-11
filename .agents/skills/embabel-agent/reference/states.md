# States Reference

## How States Work with GOAP

Within each state, GOAP planning works normally. When an action returns a `@State`-annotated class:

1. **Hides previous state objects** — existing states hidden from blackboard
2. **Binds the new state object** — added to blackboard
3. **Re-plans from the new state** — considers only actions from the new state
4. **Continues execution** — until goal reached or no plan found

**Context is preserved** across state transitions — non-state objects (user messages, customer data, conversation history) remain available. Only state objects are hidden.

## When to Use States

- **Linear stages** where each stage naturally flows to the next
- **Branching workflows** where a decision point leads to different paths
- **Looping patterns** where processing may need to repeat (revise-and-review)
- **Human-in-the-loop workflows** where user feedback determines the next state
- **Complex workflows** that are easier to reason about as discrete phases

## The @State Annotation

Classes returned from actions that should trigger state transitions:

```java
@State
public sealed interface TicketStatus permits NewTicket, InProgressTicket, ResolvedTicket {}

@Action
public TicketStatus triageTicket(Ticket ticket) {
    if (ticket.isCritical()) return new CriticalTicket(ticket);
    return new NewTicket(ticket);
}
```

### Inheritance

`@State` is inherited through the class hierarchy. Annotate only the base type:

```java
@State
public interface Stage {}

public record Draft(Story story) implements Stage {}  // automatically a state type
public record Review(Story story) implements Stage {} // automatically a state type
```

### Behavior

- Previous state objects are **hidden** (not removed, no longer visible to planning)
- The returned object is bound to the blackboard
- Planning considers only actions defined within the **current** state class
- Any `@AchievesGoal` methods in the state become potential goals

## Staying in the Current State

Return `this` with `canRerun = true`:

```java
@Action(canRerun = true)
public WaitFor<HumanFeedback> getFeedback() {
    return WaitFor.formSubmission("Please provide feedback", HumanFeedback.class);
}
```

## Looping States

For looping patterns, use `clearBlackboard = true`:

```java
@Action
public AssessStory reviseStory(ReviseStory input, Ai ai) {
    var revised = ai.withDefaultLlm()
        .creating(Story.class)
        .fromPrompt("Revise: " + input.story() + "\nFeedback: " + input.feedback());
    return new AssessStory(revised, input.feedback());  // loops back to AssessStory
}
```

Without `clearBlackboard = true`, the planner sees the output type already exists and skips.

## Parent State Interface Pattern

For dynamic state choice:

```java
@State
public sealed interface Stage permits Draft, Review, Done {}

@Action
public Stage process(Draft draft) {
    if (draft.isGood()) return new Done(draft.story());
    return new Review(draft.story());  // dynamic choice
}
```

Only the parent interface needs `@State` — all implementing classes inherit it.

## Human-in-the-Loop with WaitFor

```java
@Action
public WaitFor<HumanFeedback> getFeedback() {
    return WaitFor.formSubmission("Please provide feedback", HumanFeedback.class);
}
```

When this action executes:
1. The agent process enters a `WAITING` state
2. A form is generated from the record structure
3. User fills out and submits the form
4. The `HumanFeedback` instance is added to the blackboard
5. The agent resumes execution

## Passing Data Through States

When using `clearBlackboard = true`, pass all necessary context through state records:

```java
@State
public record AssessStory(Story story, String feedback, Properties props) implements Stage {}
```

For non-looping transitions, the blackboard is preserved and data is accessible directly.

## State Class Requirements

- **Must be static nested classes** (Java) or **top-level classes** (Kotlin)
- Non-static inner classes are **not allowed** (serialization/persistence issues)
- Java records declared inside a class are implicitly static — ideal for state classes
- In Kotlin, use **top-level declarations** (not inner data classes)

## Complete Example: WriteAndReviewAgent

```java
@State
public sealed interface Stage permits AssessStory, ReviseStory, Done {}

@Agent(description = "Write and review stories")
public class WriteAndReviewAgent {

    @Action
    public AssessStory craftStory(UserInput input, Ai ai) {
        var story = ai.withDefaultLlm()
            .creating(Story.class)
            .fromPrompt("Write a story: " + input.content());
        return new AssessStory(story, null);
    }

    @Action
    public WaitFor<HumanFeedback> getFeedback() {
        return WaitFor.formSubmission("Provide feedback", HumanFeedback.class);
    }

    @Action
    public Stage assess(AssessStory assess, HumanFeedback feedback, Ai ai) {
        if (isAcceptable(assess.story(), feedback)) {
            return new Done(assess.story());
        }
        return new ReviseStory(assess.story(), feedback.feedback());
    }

    @Action(clearBlackboard = true)
    public AssessStory revise(ReviseStory input, Ai ai) {
        var revised = ai.withDefaultLlm()
            .creating(Story.class)
            .fromPrompt("Revise: " + input.story() + "\nFeedback: " + input.feedback());
        return new AssessStory(revised, input.feedback());
    }

    @AchievesGoal(description = "Final reviewed story")
    @Action
    public ReviewedStory review(Done done, Ai ai) {
        return new ReviewedStory(done.story(), "Final review complete");
    }
}
```

## Key Points

- Annotate state classes with `@State` (or inherit from a `@State`-annotated type)
- `@State` is inherited through class hierarchies — annotate only the base type
- Use **static nested classes** (Java records) or **top-level classes** (Kotlin)
- Use a parent interface for polymorphic state returns
- **State scoping**: entering a new state hides previous states — only current state's actions are available
- **Context is preserved**: non-state objects remain available across transitions
- **Blackboard preserved** by default; use `clearBlackboard = true` for looping
- Return `this` with `canRerun = true` to stay in the current state
- Use `WaitFor` for human-in-the-loop interactions
- Goals are defined with `@AchievesGoal` on terminal state actions
