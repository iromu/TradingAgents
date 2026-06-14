Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.6. Annotation model
Embabel provides a Spring-style annotation model to define agents, actions, goals, and conditions.
This is the recommended model to use in Java, and remains compelling in Kotlin.
#### 4.6.1. The `@Agent` annotation
This annotation is used on a class to define an agent.
It is a Spring stereotype annotation, so it triggers Spring component scanning.
Your agent class will automatically be registered as a Spring bean.
It will also be registered with the agent framework, so it can be used in agent processes.You must provide the `description` parameter, which is a human-readable description of the agent.
This is particularly important as it may be used by the LLM in agent selection.
#### 4.6.2. The `@EmbabelComponent` annotation
This annotation is used on a class to indicate that this class exposes actions, goals and conditions that may be used by agents, but is not an agent in itself.
It is a Spring stereotype annotation, so it triggers Spring component scanning.
Your Embabel component class will automatically be registered as a Spring bean.
It will also be registered with the agent framework, so its actions, goals and conditions can be used in agent processes.Embabel Components are most useful in combination with the Utility AI planner that selects the most valuable next action among all available actions.
#### 4.6.3. The `@Action` annotation
The `@Action` annotation is used to mark methods that perform actions within an agent.Action metadata can be specified on the annotation, including:
- `description`: A human-readable description of the action.
- `pre`: A list of preconditions *additional to the input types* that must be satisfied before the action can be executed.
- `post`: A list of postconditions *additional to the output type(s)* that may be satisfied after the action is executed.
- `canRerun`: A boolean indicating whether the action can be rerun if it has already been executed.
Defaults to false.
- `readOnly`: A boolean indicating whether the action has no external side effects.
Read-only actions only analyze data and produce derived objects without modifying external systems (APIs, databases, files, etc.).
This is useful for learning/catchup modes where you want to ingest and understand data without triggering mutations.
Defaults to false.
- `clearBlackboard`: A boolean indicating whether to clear the blackboard after this action completes.
When true, all objects on the blackboard are removed except the action’s output.
This is useful for resetting context in multi-step workflows.
It can also make persistence of flows more efficient by dispensing with objects that are no longer needed.
Defaults to false.
- `cost`:Relative cost of the action from 0-1. Defaults to 0.0.
- `value`: Relative value of performing the action from 0-1. Defaults to 0.0.
##### Clearing the Blackboard
The `clearBlackboard` attribute is useful in two scenarios:
1.**Multi-step workflows** where you want to reset the processing context
1.**Looping states** where an action returns to a previously-visited state typeWhen an action with `clearBlackboard = true` completes, all objects on the blackboard are removed except the action’s output.
This prevents accumulated intermediate data from affecting subsequent processing and enables loops.
###### Looping States
The most common use case for `clearBlackboard` is enabling loops in state-based workflows:JavaKotlin
| **1** | `clearBlackboard = true` enables returning to the same state type || **2** | Without clearing, returning `ProcessingState` would be blocked since the type already exists |
See Using States for more details on looping state patterns.
###### Resetting Context
You can also use `clearBlackboard` to reset context in multi-step workflows:JavaKotlin
| **1** | After `preprocess` completes, the blackboard is cleared and only `ProcessedDocument` remains.
The original `RawDocument` is removed. || **2** | The `transform` action receives only the `ProcessedDocument`, not any earlier inputs. |

| | Avoid using `clearBlackboard` on goal-achieving actions (those with `@AchievesGoal`).
Clearing the blackboard removes `hasRun` tracking conditions, which may interfere with goal satisfaction.
Use `clearBlackboard` on intermediate actions instead. |

##### Dynamic Cost Computation with `@Cost`
While the `cost` and `value` fields on `@Action` allow specifying static values, you can compute these dynamically at planning time using the `@Cost` annotation.
This is useful when the cost of an action depends on the current state of the blackboard.The `@Cost` annotation marks a method that returns a cost value (a `double` between 0.0 and 1.0).
You then reference this method from the `@Action` annotation using `costMethod` or `valueMethod`.JavaKotlin
| **1** | The `@Cost` annotation marks a method for dynamic cost computation.
The `name` parameter identifies this cost method. || **2** | Domain object parameters in `@Cost` methods must be nullable.
If the object isn’t on the blackboard, `null` is passed. || **3** | The `costMethod` field references the `@Cost` method by name. |
Key differences from `@Condition` methods:
- All domain object parameters in `@Cost` methods must be nullable (use `@Nullable` in Java or `?` in Kotlin)
- When a domain object is not available on the blackboard, `null` is passed instead of causing the method to fail
- The method must return a `double` between 0.0 and 1.0
- The `Blackboard` can be passed as a parameter for direct access to all available objectsYou can also compute dynamic value using `valueMethod`:JavaKotlin
| | The `@Cost` method is called during planning, before the action executes.
It allows the planner to make informed decisions about which actions to prefer based on runtime state. |

| | Dynamic cost is especially useful with **Utility planning** (`PlannerType.UTILITY`), where cost/value tradeoffs are a core concept.
The utility planner evaluates actions based on their net value (value minus cost), making dynamic cost computation essential for sophisticated decision-making. |

#### 4.6.4. The `@Condition` annotation
The `@Condition` annotation is used to mark methods that evaluate conditions.
They can take an `OperationContext` parameter to access the blackboard and other infrastructure.
If they take domain object parameters, the condition will automatically be false until suitable instances are available.Condition methods should not have side effects—​for example, on the blackboard.
This is important because they may be called multiple times.
##### Dynamic Conditions with SpEL
In addition to using `@Condition` methods, you can specify dynamic preconditions directly on `@Action` annotations using Spring Expression Language (SpEL).
These expressions are evaluated against the blackboard, allowing you to create conditions based on runtime state without writing separate condition methods.The expression language is pluggable, but currently SpEL is the only supported implementation.
See the Spring Expression Language (SpEL) documentation for full syntax details.SpEL conditions are specified in the `pre` array with a `spel:` prefix:JavaKotlin
| **1** | The `spel:` prefix indicates this is a SpEL expression evaluated against the blackboard. |

###### Expression Syntax
SpEL expressions reference blackboard objects by their binding names (typically the camelCase form of the class name).
The expression must evaluate to a boolean.JavaKotlin
| **1** | Simple property comparison: action fires only when `urgency` property exceeds 0.0. || **2** | Type check with property access: action fires only for pull requests with more than 10 changed files.
The `T()` operator references a Java type for `instanceof` checks. |

###### Collection Filtering
SpEL’s collection selection syntax (`?[]`) is useful for checking conditions on collections stored in the blackboard:JavaKotlin
| **1** | The `?[]` operator filters the collection. `#this` refers to each element.
This expression checks that at least one element is an `Issue` but not a `PullRequest`. || **2** | Simpler filter checking for `PullRequest` instances. |

###### Common SpEL Patterns

| Pattern | Description || `spel:obj.property > value` | Simple property comparison || `spel:obj instanceof T(com.example.Type)` | Type checking using fully qualified class name || `spel:collection.size() > 0` | Check collection is not empty || `spel:collection.?[condition].size() > 0` | Check that filtered collection has elements || `spel:obj.property != null` | Null checking || `spel:condition1 && condition2` | Combining conditions with AND || `spel:condition1 || condition2` | Combining conditions with OR |

| | Use SpEL conditions for simple property checks and type discrimination.
For complex logic or conditions that need to be reused across multiple actions, prefer `@Condition` methods.
For reactive scenarios where you simply want an action to fire when a specific type is added to the blackboard, consider using the `trigger` field instead—it’s simpler than writing a SpEL expression. |

| | Blackboard binding names are derived from the class name in camelCase by default.
You can specify explicit binding names using `outputBinding` on actions or by adding objects to the blackboard with specific names. |
Both Action and Condition methods may be inherited from superclasses.
That is, annotated methods on superclasses will be treated as actions on a subclass instance.Give your Action and Condition methods unique names, so the planner can distinguish between them.
#### 4.6.5. Parameters
`@Action` methods must have at least one parameter.
`@Condition` methods must have zero or more parameters, but otherwise follow the same rules as `@Action` methods regarding parameters.
Ordering of parameters is not important.Parameters fall in two categories:
- *Domain objects*.
These are the normal inputs for action methods.
They are backed by the blackboard and will be used as inputs to the action method.
A nullable domain object parameter will be populated if it is non-null on the blackboard.
This enables nice-to-have parameters that are not required for the action to run.
In Kotlin, use a nullable parameter with `?`: in Java, mark the parameter with the `org.springframework.lang.Nullable` or another `Nullable` annotation.
- *Infrastructure parameters*, such as the `OperationContext`, `ProcessContext`, and `Ai` may be used in action or condition methods.
| | Domain objects drive planning, specifying the preconditions to an action. |
The `ActionContext` or `ExecutingOperationContext` subtype can be used in action methods.
It adds `asSubProcess` methods that can be used to run other agents in subprocesses.
This is an important element of composition.Use the least specific type possible for parameters.
Use `OperationContext` unless you are creating a subprocess.
##### Custom Parameters
Besides two default parameter categories described above, you can provide your own parameters by implementing the `ActionMethodArgumentResolver` interface.
The two main methods of this interface are:
- `supportsParameter`, which indicates what kind of parameters are supported, and
- `resolveArgument`, which resolves the argument into an object used to invoke the action method.
| | Note the similarity with Spring MVC, where you can provide custom parameters by implementing a `HandlerMethodArgumentResolver`. |
All default parameters are provided by `ActionMethodArgumentResolver` implementations.To register your custom argument resolver, provide it to the `DefaultActionMethodManager` component in your Spring configuration.
Typically, you will register (some of) the defaults as well your custom resolver, in order to support the default parameters.
| | Make sure to register the `BlackboardArgumentResolver` as last resolver, to ensure that others take precedence. |

##### The `@Provided` Annotation
The `@Provided` annotation marks an action method parameter as being provided by the platform context (such as Spring’s `ApplicationContext`) rather than resolved from the blackboard.This is particularly useful for:
- **Accessing the enclosing component** from within `@State` classes (which must be static or top-level)
- **Injecting services** that aren’t domain objects but are needed for processing
- **Accessing configuration** or other platform-managed beansJavaKotlin
| **1** | `ReservationDetails` is a domain object resolved from the blackboard. || **2** | `ReservationFlow` is injected via `@Provided` from the Spring context - this gives access to the services in the enclosing component. |

###### How It Works
When Spring is available, the `SpringContextProvider` resolves `@Provided` parameters by looking up beans from the `ApplicationContext`.
The parameter type must match a bean in the context.JavaKotlin
| **1** | Any Spring bean can be injected using `@Provided`. || **2** | Multiple `@Provided` parameters can be used in a single method. |

###### When to Use `@Provided`
Use `@Provided` when you need access to:
- The enclosing `@EmbabelComponent` or `@Agent` class from a `@State` action
- Services that are infrastructure concerns, not domain objects
- Configuration or environment valuesDo **not** use `@Provided` for:
- Domain objects that should drive planning (use regular parameters instead)
- Objects that need to be tracked on the blackboard
| | Since `@State` classes must be static nested classes or top-level classes, `@Provided` is the recommended way to access the enclosing component’s services.
This keeps state classes serializable while still providing access to dependencies. |

| | `@Provided` parameters are resolved before blackboard parameters.
If a type could come from either source, `@Provided` takes precedence. |

#### 4.6.6. Binding by name
The `@RequireNameMatch` annotation can be used to bind parameters by name.
#### 4.6.7. Reactive triggers with `trigger`
The `trigger` field on the `@Action` annotation enables reactive behavior where an action only fires when a specific type is the *most recently added* value to the blackboard.
This is useful in event-driven scenarios where you want to react to a particular event even when multiple parameters of various types are available.For example, in a chat system you might want an action to fire only when a new user message arrives, not when other context is updated:JavaKotlin
| **1** | The `trigger` field means this action only fires when `UserMessage` is the last result added to the blackboard. || **2** | `Conversation` must also be available, but doesn’t need to be the triggering event. |
Without `trigger`, an action fires as soon as all its parameters are available on the blackboard.
With `trigger`, the specified type must additionally be the most recent value added.This is particularly useful when:
- You have multiple actions that could handle different event types
- You want to distinguish between "data available" and "event just occurred"
- You’re building event-driven or reactive workflowsJavaKotlin
| **1** | `handleEventA` fires when `EventA` is added (and `EventB` is available). || **2** | `handleEventB` fires when `EventB` is added (and `EventA` is available). |

| | The `trigger` field checks that the specified type matches the `lastResult()` on the blackboard.
The last result is the most recent object added via any binding operation. |

#### 4.6.8. Handling of return types
Action methods normally return a single domain object.Nullable return types are allowed.
Returning null will trigger replanning.
There may or not be an alternative path from that point, but it won’t be what the planner was previously trying to achieve.There is a special case where the return type can essentially be a union type, where the action method can return one ore more of several types.
This is achieved by a return type implementing the `SomeOf` tag interface.
Implementations of this interface can have multiple nullable fields.
Any non-null values will be bound to the blackboard, and the postconditions of the action will include all possible fields of the return type.For example:JavaKotlinThis enables routing scenarios in an elegant manner.
| | Multiple fields of the `SomeOf` instance may be non-null and this is not an error.
It may enable the most appropriate routing. |
Routing can also be achieved via subtypes, as in the following example:JavaKotlin
| **1** | Classification action returns supertype `Intent`.
Real classification would likely use an LLM. || **2** | `billingAction` and other action methods takes a subtype of `Intent`, so will only be invoked if the classification action returned that subtype. |

#### 4.6.9. Action method implementation
Embabel makes it easy to seamlessly integrate LLM invocation and application code, using common types.
An `@Action` method is a normal method, and can use any libraries or frameworks you like.The only special thing about it is its ability to use the `OperationContext` parameter to access the blackboard and invoke LLMs.
#### 4.6.10. The `@AchievesGoal` annotation
The `@AchievesGoal` annotation can be added to an `@Action` method to indicate that the completion of the action achieves a specific goal.
#### 4.6.11. The `@SecureAgentTool` annotation
`@SecureAgentTool` declares the security contract for an Embabel `@Action` method or `@Agent`
class exposed as a remote MCP tool.
It accepts a Spring Security SpEL expression evaluated against the current `Authentication`
at the point of tool invocation, before Embabel’s GOAP planner executes the action body.
##### Placement
`@SecureAgentTool` can be placed on the `@Agent` class to protect every `@Action` uniformly,
or on individual methods for finer-grained control.
Method-level annotation takes precedence over class-level when both are present.**Class-level** — one annotation secures all actions in the agent, including intermediate steps
that run before the goal-achieving action:KotlinJava
| **1** | One annotation on the class protects every `@Action` in the agent. || **2** | Both `extractTopic` and `produceDigest` require `news:read`.
Without class-level protection, intermediate actions like `extractTopic` would run freely
before the security check on the goal-achieving action fires. |
**Method-level override** — a method-level annotation takes precedence over the class-level
expression, allowing one action to require elevated authority:KotlinJava
| **1** | All actions default to requiring `market:read`. || **2** | `synthesiseReport` requires `market:admin` — the method-level annotation overrides the class. |

##### Supported expressions
Any Spring Security SpEL expression is valid:
| Expression | Meaning || `hasAuthority('finance:read')` | Principal must carry this exact authority || `hasAnyAuthority('finance:read', 'finance:admin')` | Principal must carry at least one of the listed authorities || `hasRole('ADMIN')` | Principal must carry `ROLE_ADMIN` (the `ROLE_` prefix is added automatically) || `isAuthenticated()` | Any authenticated principal, regardless of authorities || `hasAuthority('payments:write') and #request.amount < 10000` | Combines an authority check with a method parameter expression |

##### Setup
Add the MCP security starter to your `pom.xml`:The starter auto-configures `SecureAgentToolAspect` and the required Spring Security
`MethodSecurityExpressionHandler`.
No additional `@EnableMethodSecurity` annotation is required.
| | `@SecureAgentTool` is a method-level security control, not an HTTP-level one.
For production use, combine it with a `SecurityFilterChain` that validates JWT Bearer tokens
so unauthenticated requests are rejected before reaching the GOAP planner.
See the Spring Security JWT Resource Server documentation for general setup,
or MCP Security for an MCP-specific example. |

#### 4.6.12. Implementing the `StuckHandler` interface
If an annotated agent class implements the `StuckHandler` interface, it can handle situations where an action is stuck itself.
For example, it can add data to the blackboard.Example:JavaKotlin
#### 4.6.13. Advanced Usage: Nested processes
An `@Action` method can invoke another agent process.
This is often done to use a stereotyped process that is composed using the DSL.Use the `ActionContext.asSubProcess` method to create a sub-process from the action context.For example:JavaKotlin
#### 4.6.14. Running Subagents with `RunSubagent`
The `RunSubagent` utility provides a convenient way to run a nested agent from within an `@Action` method without needing direct access to `ActionContext`.
This is particularly useful when you want to delegate work to another `@Agent`-annotated class or an `Agent` instance.
##### Running an `@Agent`-annotated Instance
Use `RunSubagent.fromAnnotatedInstance()` when you have an instance of a class annotated with `@Agent`:
| | The annotated instance can be Spring-injected into your agent class.
Since `@Agent` is a Spring stereotype annotation, you can inject one agent into another and run it as a subagent.
This enables clean separation of concerns while maintaining testability. |
JavaKotlin
| **1** | Spring injects the `InnerSubAgent` bean via constructor injection. || **2** | The injected instance is passed to `RunSubagent.fromAnnotatedInstance()`. |
In Kotlin, you can use the reified version for a more concise syntax:JavaKotlin
##### Running an `Agent` Instance
Use `RunSubagent.instance()` when you already have an `Agent` object (for example, one created programmatically or via `AgentMetadataReader`):JavaKotlinIn Kotlin with reified types:JavaKotlin
##### How It Works
`RunSubagent` methods throw a `SubagentExecutionRequest` exception that is caught by the framework.
The framework then executes the subagent as a subprocess within the current agent process, sharing the same blackboard context.
The result of the subagent’s goal-achieving action is returned to the calling action.This approach has several advantages:
- **Cleaner syntax**: No need to pass `ActionContext` to the action method
- **Type safety**: The return type is enforced at compile time
- **Composition**: Easily compose complex workflows from simpler agents
- **Reusability**: The same subagent can be used in multiple contexts
##### Comparison with `ActionContext.asSubProcess`
Both `RunSubagent` and `ActionContext.asSubProcess` achieve the same result, but differ in style:
| Approach | When to use | Example || `RunSubagent.fromAnnotatedInstance()` | When you have an `@Agent`-annotated instance and don’t need `ActionContext` | `RunSubagent.fromAnnotatedInstance(new SubAgent(), Result.class)` || `RunSubagent.instance()` | When you have an `Agent` object | `RunSubagent.instance(agent, Result.class)` || `ActionContext.asSubProcess()` | When you need access to `ActionContext` for other operations | `context.asSubProcess(Result.class, agent)` |

| | Use `RunSubagent` when your action method only needs to delegate to a subagent.
Use `ActionContext.asSubProcess()` when you need additional context operations. |

#### 4.6.15. Action Exception Handling
Exception handling within Action is governed by Retry Policy.All exceptions below, except `TransientAiException` are considered as non-retryable.
More specifically, policy categorises non-retryable exception in the order:
- ReplanRequestedException
- TerminateActionException
- TerminateAgentException
- ToolControlFlowSignal
- NonTransientAiException
- IllegalArgumentException
- IllegalStateException
- UnsupportedOperationException
- ClassCastExceptionIf exception does not belong to any of the exceptions from the list above - it gets mapped to retryable exception.Framework allows creating custom Retryable / NonRetryable exception in order for developers to exercise complete control over Action Retry.Embabel provides with two approaches for defining custom retryable and non-retryable exceptions:
1.**Extend ActionException** - Convenient base classes with built-in retry classification
1.**Implement marker interfaces** - Maximum flexibility for existing exception hierarchies
##### Approach 1: Extending ActionException
The recommended approach is to extend `ActionException.Transient` for retryable failures or `ActionException.Permanent` for non-retryable failures:KotlinJava+
##### Approach 2: Implementing Marker Interfaces

```
For existing exception hierarchies or when you need more control, implement the `Retryable` or `NonRetryable` marker interfaces directly:
```
KotlinJava+
##### Common Use Cases
**Transient Failures** (use `ActionException.Transient` or `Retryable`):
- Network timeouts
- Rate limiting (429 errors)
- Temporary resource unavailability
- Connection failures
- Database deadlocks**Permanent Failures** (use `ActionException.Permanent` or `NonRetryable`):
- Validation errors
- Business rule violations
- Invalid parameters
- Resource not found (404 errors)
- Authentication failures (401 errors)
- Authorization failures (403 errors)