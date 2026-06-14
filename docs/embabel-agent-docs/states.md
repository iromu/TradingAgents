Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.19. Using States
GOAP planning has many benefits, but can make looping hard to express.
For this reason, Embabel supports the notion of **states** within a GOAP plan.
#### 4.19.1. How States Work with GOAP
Within each state, GOAP planning works normally.
Actions have preconditions based on the types they require, and effects based on the types they produce.
The planner finds the optimal sequence of actions to reach the goal.When an action returns a `@State`-annotated class, the framework:
1.**Hides previous state objects** - Any existing state objects are hidden from the blackboard
1.**Binds the new state object** - The returned state is added to the blackboard
1.**Re-plans from the new state** - The planner considers only actions from the new state
1.**Continues execution** - Until a goal is reached or no plan can be found**Context is preserved** across state transitions - non-state objects (such as user messages, customer data, and conversation history) remain available.
Only state objects are hidden, ensuring that only the current state’s actions are considered by the planner.
| | State transitions **hide** previous state objects but do **not clear** the blackboard.
Non-state objects remain available in the new state.
To clear the entire blackboard (e.g., for looping), use `clearBlackboard = true` on the action. |

#### 4.19.2. When to Use States
States are ideal for:
- **Linear stages** where each stage naturally flows to the next
- **Branching workflows** where a decision point leads to different processing paths
- **Looping patterns** where processing may need to repeat (e.g., revise-and-review cycles)
- **Human-in-the-loop workflows** where user feedback determines the next state
- **Complex workflows** that are easier to reason about as discrete phasesStates allow loopback to a whole state, which may contain one or more actions.
This is more flexible than traditional GOAP, where looping requires careful management of preconditions.
#### 4.19.3. Staying in the Current State
An action can return `this` to stay in the current state.
This is useful for actions that respond to inputs without changing state, such as chat handlers:JavaKotlin
| **1** | `canRerun = true` is required - by default, actions only run once per process || **2** | Returning `this` keeps the same state instance active |
When an action returns `this`:
- The state remains active with no transition
- The blackboard is preserved (no clearing)
- The action can run again on subsequent planning cycles (if `canRerun = true`)
| | Without `canRerun = true`, the action’s `hasRun` flag would prevent it from executing again, even though it returned `this`. |

#### 4.19.4. Looping States
For looping patterns where an action may return to a previously-visited state type, use `clearBlackboard = true` on the looping action:JavaKotlin
| **1** | `clearBlackboard = true` allows the action to loop back to the same state type || **2** | Terminal condition exits the loop || **3** | Returns a new instance of the same state type for another iteration |
Without `clearBlackboard = true`, the planner would see the output type already exists on the blackboard and skip the action.
Clearing the blackboard resets the context, allowing natural loops.
| | Only use `clearBlackboard = true` on actions that participate in loops.
For linear state transitions, the default behavior (preserving the blackboard) is usually preferred. |

#### 4.19.5. The @State Annotation
Classes returned from actions that should trigger state transitions must be annotated with `@State`:JavaKotlin
##### Inheritance
The `@State` annotation is inherited through the class hierarchy.
If a superclass or interface is annotated with `@State`, all subclasses and implementing classes are automatically considered state types.
This means you don’t need to annotate every class in a hierarchy - just annotate the base type.JavaKotlin
| **1** | Only the parent interface needs `@State` || **2** | Implementing records/data classes are automatically treated as state types |
This works with:
- **Interfaces**: Classes implementing a `@State` interface are state types
- **Abstract classes**: Classes extending a `@State` abstract class are state types
- **Concrete classes**: Classes extending a `@State` class are state types
- **Deep hierarchies**: The annotation is inherited through multiple levels
##### Behavior
When an action returns a `@State`-annotated class (or a class that inherits `@State`):
- Any previous state objects are **hidden** from the blackboard (not removed, but no longer visible)
- The returned object is bound to the blackboard (as `it`)
- Planning considers only actions defined within the **current** state class
- Any `@AchievesGoal` methods in the state become potential goalsContext (non-state objects) is preserved across state transitions.
This means user messages, customer data, conversation history, etc. remain available in the new state.
Only state objects are hidden, providing **state scoping** - ensuring only the current state’s actions are considered.
| | For looping states that return to a previously-visited state type, use `@Action(clearBlackboard = true)` on the looping action.
This clears the blackboard (including hasRun conditions) and allows the loop to continue.
See Looping States for details. |

#### 4.19.6. Parent State Interface Pattern
For dynamic choice between states, define a parent interface (or sealed interface/class) that child states implement.
Thanks to inheritance, you only need to annotate the parent interface - all implementing classes are automatically state types:JavaKotlin
| **1** | `@State` on the parent interface || **2** | No `@State` needed on implementing records/data classes - they inherit it from `Stage` |
This pattern enables:
- **Polymorphic return types**: Actions can return any implementation of the parent interface
- **Dynamic routing**: The runtime value determines which state is entered
- **Looping**: States can return other states that eventually loop backThe framework automatically discovers all implementations of the parent interface and registers their actions as potential next steps.
#### 4.19.7. Example: WriteAndReviewAgent
The following example demonstrates a complete write-and-review workflow with:
- State-based flow control with looping
- Human-in-the-loop feedback using `WaitFor`
- LLM-powered content generation and assessment
- Configurable properties passed through statesJavaKotlin
| **1** | **Personas**: Reusable prompt contributors that give the LLM context about its role || **2** | **Parent state interface**: Allows actions to return any implementing state dynamically || **3** | **Properties record**: Configuration bundled together for easy passing through states || **4** | **Entry action**: Uses LLM to generate initial story draft || **5** | **State transition**: Returns `AssessStory` with all necessary data || **6** | **HITL data type**: Simple record/data class to capture human feedback || **7** | **WaitFor integration**: Pauses execution and waits for user to submit feedback form || **8** | **Looping action**: `clearBlackboard = true` enables returning to a previously-visited state type || **9** | **Terminal branch**: If acceptable, transitions to `Done` state || **10** | **Loop branch**: If not acceptable, transitions to `ReviseStory` with the feedback || **11** | **Looping action**: `clearBlackboard = true` enables looping back to `AssessStory` || **12** | **Loop back**: Returns new `AssessStory` for another round of feedback || **13** | **Goal achievement**: Final action that produces the reviewed story and exports it |

#### 4.19.8. Execution Flow
The execution flow for this agent:
1.**`craftStory`** executes with LLM, returns `AssessStory` → enters `AssessStory` state
1.**`getFeedback`** calls `WaitFor.formSubmission()` → agent pauses, waits for user input
1.User submits feedback → `HumanFeedback` added to blackboard
1.**`assess`** executes with LLM to interpret feedback:
- If acceptable: returns `Done` → blackboard cleared, enters `Done` state
- If not acceptable: returns `ReviseStory` → blackboard cleared, enters `ReviseStory` state
1.If in `ReviseStory`: **`reviseStory`** executes with LLM, returns `AssessStory` → blackboard cleared, loop back to step 2
1.When in `Done`: **`reviewStory`** executes with LLM, returns `ReviewedStory` → goal achievedThe planner handles all transitions automatically, including loops.
The looping actions (`assess` and `reviseStory`) use `clearBlackboard = true` to enable returning to previously-visited state types.
#### 4.19.9. Human-in-the-Loop with WaitFor
The `WaitFor.formSubmission()` method is key for human-in-the-loop workflows:JavaKotlinWhen this action executes:
1.The agent process enters a `WAITING` state
1.A form is generated based on the `HumanFeedback` record structure
1.The user sees the prompt and fills out the form
1.Upon submission, the `HumanFeedback` instance is created and added to the blackboard
1.The agent resumes execution with the feedback availableThis integrates naturally with the state pattern: the feedback stays within the current state until the next state transition.
#### 4.19.10. Passing Data Through States
When using `clearBlackboard = true` for looping states, all necessary context must be passed through state records since the blackboard is cleared:JavaKotlin
| | Use a `Properties` record/data class to bundle configuration values that need to pass through multiple states, rather than repeating individual fields. |

| | For non-looping state transitions (where `clearBlackboard` is not used), the blackboard is preserved, and data can be accessed from the blackboard directly.
This is useful when states need access to shared context like user identity or conversation history. |

#### 4.19.11. State Class Requirements

| | State classes **must be** either **static nested classes** (Java) or **top-level classes** (Kotlin).
Non-static inner classes are **not allowed** because they hold a reference to their enclosing instance, causing serialization and persistence issues.
The framework will throw an `IllegalStateException` if it detects a non-static inner class annotated with `@State`. |
JavaKotlinIn Java, records declared inside a class are implicitly static, making them ideal for state classes.
In Kotlin, data classes declared inside a class are inner by default; use **top-level declarations** instead.
| | Top-level state classes are the recommended pattern for Kotlin.
They can access the enclosing component via the `@Provided` annotation.
See The @Provided Annotation for full documentation. |

#### 4.19.12. Key Points

- Annotate state classes with `@State` (or inherit from a `@State`-annotated type)
- `@State` is inherited through class hierarchies - annotate only the base type
- Use **static nested classes** (Java records) or **top-level classes** to avoid persistence issues
- Use a parent interface for polymorphic state returns
- State actions are automatically discovered and registered
- **State scoping**: When entering a new state, previous states are hidden - only current state’s actions are available
- **Context is preserved**: Non-state objects (user data, conversation, etc.) remain available across transitions
- **Blackboard preserved**: State transitions hide previous states but preserve all other blackboard contents
- **Staying in state**: Return `this` with `canRerun = true` to stay in the current state without transitioning
- For **looping states**, use `@Action(clearBlackboard = true)` to enable returning to previously-visited state types
- When using `clearBlackboard = true`, pass all necessary data through state record fields
- Goals are defined with `@AchievesGoal` on terminal state actions
- Use `WaitFor` for human-in-the-loop interactions within states
- Within a state, normal GOAP planning applies to sequence actions