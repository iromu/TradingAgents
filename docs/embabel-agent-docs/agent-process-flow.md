Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.2. Agent Process Flow
When an agent is invoked, Embabel creates an `AgentProcess` with a unique identifier that manages the complete execution lifecycle.
#### 4.2.1. AgentProcess Lifecycle
An `AgentProcess` maintains state throughout its execution and can transition between various states:**Process States:**
- `NOT_STARTED`: The process has not started yet
- `RUNNING`: The process is executing without any known problems
- `COMPLETED`: The process has completed successfully
- `FAILED`: The process has failed and cannot continue
- `TERMINATED`: The process was killed by an early termination policy
- `KILLED`: The process was killed by the user or platform
- `STUCK`: The process cannot formulate a plan to progress (may be temporary)
- `WAITING`: The process is waiting for user input or external event
- `PAUSED`: The process has paused due to scheduling policy**Process Execution Methods:**
- `tick()`: Perform the next single step and return when an action completes
- `run()`: Execute the process as far as possible until completion, failure, or a waiting stateThese methods are not directly called by user code, but are managed by the framework to control execution flow.Each `AgentProcess` maintains:
- **Unique ID**: Persistent identifier for tracking and reference
- **History**: Record of all executed actions with timing information
- **Goal**: The objective the process is trying to achieve
- **Failure Info**: Details about any failure that occurred
- **Parent ID**: Reference to parent process for nested executions
#### 4.2.2. Planning
Planning occurs after each action execution using Goal-Oriented Action Planning (GOAP).
The planning process:
1.**Analyze Current State**: Examine the current blackboard contents and world state
1.**Identify Available Actions**: Find all actions that can be executed based on their preconditions
1.**Search for Action Sequences**: Use A* algorithm to find optimal paths to achieve the goal
1.**Select Optimal Plan**: Choose the best action sequence based on cost and success probability
1.**Execute Next Action**: Run the first action in the plan and replanThis creates a dynamic **OODA loop** (Observe-Orient-Decide-Act):
- **Observe**: Check current blackboard state and action results - **Orient**: Understand what has changed since the last planning cycle - **Decide**: Formulate or update the plan based on new information - **Act**: Execute the next planned actionThe replanning approach allows agents to:
- Adapt to unexpected action results
- Handle dynamic environments where conditions change
- Recover from partial failures
- Take advantage of new opportunities that arise
#### 4.2.3. Blackboard
The Blackboard serves as the shared memory system that maintains state throughout the agent process execution.
It implements the Blackboard architectural pattern, a knowledge-based system approach.Most of the time, user code doesn’t need to interact with the blackboard directly, as it is managed by the framework.
For example, action inputs come from the blackboard, and action outputs are automatically added to the blackboard, and conditions are evaluated based on its contents.**Key Characteristics:**
- **Central Repository**: Stores all domain objects, intermediate results, and process state
- **Type-Based Access**: Objects are indexed and retrieved by their types
- **Ordered Storage**: Objects maintain the order they were added, with latest being default
- **Immutable Objects**: Once added, objects cannot be modified (new versions can be added)
- **Condition Tracking**: Maintains boolean conditions used by the planning system**Core Operations:**JavaKotlin
| **1** | **Adding Objects**: Objects are added to the blackboard automatically when returned from action methods, so you don’t typically need to call this API.
They can also be added manually using the `+=` operator (Kotlin only) or `add`/`set` method with an optional key. || **2** | **Conditions**: Conditions are normally calculated in `@Condition` methods, so you don’t usually need to check or set them via the API. || **3** | **Hiding Objects**: Prevents an object from being considered in future planning cycles.
For example, the object might be a command that we have handled.
It will remain in the blackboard history but will not be available to planning or via the Blackboard API. |
**Data Flow:**
1.**Input Processing**: Initial user input is added to the blackboard
1.**Action Execution**: Each action reads inputs from blackboard and adds results
1.**State Evolution**: Blackboard accumulates objects representing the evolving state
1.**Planning Input**: Current blackboard state informs the next planning cycle
1.**Result Extraction**: Final results are retrieved from blackboard upon completionThe blackboard enables:
- **Loose Coupling**: Actions don’t need direct references to each other
- **Flexible Data Flow**: Actions can consume any available data of the right type
- **State Persistence**: Complete execution history is maintained
- **Debugging Support**: Full visibility into state evolution for troubleshooting
#### 4.2.4. Binding
By default, items in the blackboard are matched by type.
When there are multiple candidates of the same type, the most recently added one is provided.
It is also possible to assign a specific name to blackboard items.An example of explicit binding in an action method:JavaKotlin
| **1** | Explicit binding to the blackboard.
Not usually necessary as action method return values are automatically bound. |
The following example requires a `Thing` named `thingOne` to be present in the blackboard:JavaKotlin
| **1** | The `@RequireNameMatch` annotation on the parameter specifies that the parameter should be matched by both type and name.
Multiple parameters can be so annotated. |
The following example uses `@Action.outputBinding` to cause a `thingOne` to be bound in the blackboard, satisfying the previous example:JavaKotlin
| | When routing flows by type, the name is not important, but for reference the default name is 'it'. |

#### 4.2.5. Context
Embabel offers a way to store longer term state: the `com.embabel.agent.core.Context`.
While a blackboard is tied to a specific agent process, a context can persist across multiple processes.Contexts are identified by a unique `contextId` string.
When starting an agent process, you can specify a `contextId` in the `ProcessOptions`.
This will populate that process’s blackboard with any data stored in the specified context.
| | Context persistence is dependent on the implementation of `com.embabel.agent.spi.ContextRepository`.
The default implementation works only in memory, so does not survive server restarts. |

### 8.4. Goal-Oriented Action Planning (GOAP)

- Here’s an Introduction to GOAP, the planning algorithm used by Embabel.
Explains the core concepts and why GOAP is effective for AI agent planning.
#### 8.4.1. Small Language Model Agents - NVIDIA Research

- This Research paper discusses the division between "code agency" and "LLM agency" - concepts that inform Embabel’s architecture.
#### 8.4.2. OODA Loop - Wikipedia
Here’s a Background on the Observe-Orient-Decide-Act loop that underlies Embabel’s replanning approach.