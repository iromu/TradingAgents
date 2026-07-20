# Agent Process Flow

Covers the core execution model: AgentProcess lifecycle, blackboard, binding, context, and the planning loop.

## AgentProcess Lifecycle

When an agent is invoked, Embabel creates an `AgentProcess` with a unique identifier that manages the complete execution lifecycle.

### Process States

| State | Description |
|-------|-------------|
| `NOT_STARTED` | The process has not started yet |
| `RUNNING` | The process is executing without any known problems |
| `COMPLETED` | The process has completed successfully |
| `FAILED` | The process has failed and cannot continue |
| `TERMINATED` | The process was killed by an early termination policy |
| `KILLED` | The process was killed by the user or platform |
| `STUCK` | The process cannot formulate a plan to progress (may be temporary) |
| `WAITING` | The process is waiting for user input or external event |
| `PAUSED` | The process has paused due to scheduling policy |

### Execution Methods

| Method | Description |
|--------|-------------|
| `tick()` | Perform the next single step and return when an action completes |
| `run()` | Execute the process as far as possible until completion, failure, or a waiting state |

These methods are managed by the framework, not called directly by user code.

### Process Metadata

Each `AgentProcess` maintains:

- **Unique ID** — Persistent identifier for tracking and reference
- **History** — Record of all executed actions with timing information
- **Goal** — The objective the process is trying to achieve
- **Failure Info** — Details about any failure that occurred
- **Parent ID** — Reference to parent process for nested executions

---

## Planning (OODA Loop)

Planning occurs after each action execution using Goal-Oriented Action Planning (GOAP).

1. **Observe** — Examine the current blackboard contents and world state
2. **Orient** — Understand what has changed since the last planning cycle
3. **Decide** — Use A* search to find optimal action sequences to achieve the goal
4. **Act** — Execute the first action in the plan and replan

This creates a dynamic OODA loop that allows agents to:

- Adapt to unexpected action results
- Handle dynamic environments where conditions change
- Recover from partial failures
- Take advantage of new opportunities that arise

---

## Blackboard

The Blackboard is the shared memory system that maintains state throughout the agent process execution. It implements the [Blackboard architectural pattern](https://en.wikipedia.org/wiki/Blackboard_(design_pattern)).

**Key Characteristics:**

- **Central Repository** — Stores all domain objects, intermediate results, and process state
- **Type-Based Access** — Objects are indexed and retrieved by their types
- **Ordered Storage** — Objects maintain the order they were added; latest is the default
- **Immutable Objects** — Once added, objects cannot be modified (new versions can be added)
- **Condition Tracking** — Maintains boolean conditions used by the planning system

### Core Operations

**Java**

```java
// Add objects to blackboard
blackboard.add(person);
blackboard.set("result", analysis);

// Retrieve objects by type
Person person = blackboard.last(Person.class);
List<Person> allPersons = blackboard.all(Person.class);

// Check conditions
blackboard.setCondition("userVerified", true);
boolean verified = blackboard.getCondition("userVerified");

// Hide an object (prevents it from being considered in future planning)
blackboard.hide(somethingWeDontWantToPlanOnLater);
```

**Kotlin**

```kotlin
// Add objects to blackboard
blackboard += person
blackboard["result"] = analysis

// Retrieve objects by type
val person = blackboard.last<Person>()
val allPersons = blackboard.all<Person>()

// Check conditions
blackboard.setCondition("userVerified", true)
val verified = blackboard.getCondition("userVerified")

// Hide an object
blackboard.hide(somethingWeDontWantToPlanOnLater)
```

### Data Flow

1. **Input Processing** — Initial user input is added to the blackboard
2. **Action Execution** — Each action reads inputs from blackboard and adds results
3. **State Evolution** — Blackboard accumulates objects representing the evolving state
4. **Planning Input** — Current blackboard state informs the next planning cycle
5. **Result Extraction** — Final results are retrieved from blackboard upon completion

Most of the time, user code doesn't need to interact with the blackboard directly — action inputs come from it, and action outputs are automatically added to it.

---

## Binding

By default, blackboard items are matched by type. When there are multiple candidates of the same type, the most recently added one is provided. You can also assign specific names to blackboard items.

### Explicit Binding

```java
@Action
public Person extractPerson(UserInput userInput, OperationContext context) {
    PersonImpl maybeAPerson = context.promptRunner()
        .withLlm(LlmOptions.withModel(OpenAiModels.GPT_4O_MINI))
        .createObjectIfPossible("Create a person from this input...", PersonImpl.class);
    if (maybeAPerson != null) {
        context.bind("user", maybeAPerson);
    }
    return maybeAPerson;
}
```

### @RequireNameMatch

The `@RequireNameMatch` annotation on a parameter specifies that it should be matched by both type and name:

```java
@Action
public Whatever doWithThing(@RequireNameMatch Thing thingOne) { ... }
```

### @Action.outputBinding

```java
@Action(outputBinding = "thingOne")
public Thing bindThing1() { ... }
```

When routing flows by type, the name is not important — the default name is `'it'`.

---

## Context

Embabel offers a way to store longer-term state via `com.embabel.agent.core.Context`. While a blackboard is tied to a specific agent process, a context can persist across multiple processes.

Contexts are identified by a unique `contextId` string. When starting an agent process, you can specify a `contextId` in the `ProcessOptions`. This populates that process's blackboard with any data stored in the specified context.

> **Note:** Context persistence depends on the implementation of `com.embabel.agent.spi.ContextRepository`. The default implementation works only in memory and does not survive server restarts.

---

*Source: Embabel Agent v1.0.0 documentation — `reference/flow`*
