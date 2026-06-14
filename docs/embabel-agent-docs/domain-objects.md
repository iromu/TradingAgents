Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.4. Domain Objects
Domain objects in Embabel are not just strongly-typed data structures - they are real objects with behavior that can be selectively exposed to LLMs and used in agent actions.
#### 4.4.1. Objects with Behavior
Unlike simple structs or DTOs, Embabel domain objects can encapsulate business logic and expose it to LLMs through the `@Tool` annotation.
For example:JavaKotlin
| **1** | The `@Tool` annotation exposes this method to LLMs when the object is added via `PrompRunner.withToolObject()`. || **2** | Unannotated methods such as `updateLoyaltyLevel` are never exposed to LLMs, regardless of their visibility level.
This ensures that tool exposure is safe, explicit and controlled. |

#### 4.4.2. Selective Tool Exposure
The `@Tool` annotation allows you to selectively expose domain object methods to LLMs.
For example:
- **Business Logic**: Expose methods that provide *safely invocable* business value to the LLM
- **Calculated Properties**: Methods that compute derived values.
This can help LLMs with calculations they might otherwise get wrong.
- **Business Rules**: Methods that implement domain-specific rules
| | Always keep internal implementation details hidden, and think carefully before exposing methods that mutate state or have side effects. |

#### 4.4.3. Use of Domain Objects in Actions
Domain objects can be used naturally in action methods, combining LLM interactions with traditional object-oriented programming.
The availability of the domain object instances also drives Embabel planning.JavaKotlin
| **1** | The `Customer` domain object is provided as a tool object, allowing the LLM to call its `@Tool` methods.
The LLM has access to `customer.getLoyaltyDiscount()` and `customer.isPremiumEligible()`. |

| | Domain object methods, even if annotated, will not be exposed to LLMs unless explicitly added via `withToolObject()`. |

#### 4.4.4. Domain Understanding is Critical
As outlined in Context Engineering Needs Domain Understanding, Rod Johnson’s blog introducing DICE (Domain-Integrated Context Engineering), domain understanding is fundamental to effective context engineering.
Domain objects serve as the bridge between:
- **Business Domain**: Real-world entities and their relationships
- **Agent Behavior**: How LLMs understand and interact with the domain
- **Code Actions**: Traditional programming logic that operates on domain objects
#### 4.4.5. Benefits

- **Rich Context**: LLMs receive both data structure and behavioral context
- **Encapsulation**: Business logic stays within domain objects where it belongs
- **Reusability**: Domain objects can be used across multiple agents
- **Testability**: Domain logic can be unit tested independently
- **Evolution**: Adding new tools to domain objects extends agent capabilitiesThis approach ensures that agents work with meaningful business entities rather than generic data structures, leading to more natural and effective AI interactions.

### 6.1. Domain objects
A rich domain model helps build a good agentic system.
Domain objects should not merely contain state, but also expose behavior.
Avoid the anemic domain model.
Domain objects have multiple roles:
1.*Ensuring type safety and toolability.*
Code can access their state; prompts will be strongly typed; and LLMs know what to return.
1.*Exposing behavior to call in code*, exactly as in any well-designed object-oriented system.
1.*Exposing tools to LLMs*, allowing them to call domain objects.The third role *is* novel in the context of LLMs and Embabel.
| | When designing your domain objects, consider which methods should be callable by LLMs and which should not. |
Expose methods that LLMs should be able to call using the `@Tool` annotation:JavaKotlin
| **1** | The Spring AI `@Tool` annotation indicates that this method is callable by LLMs. |
When an `@Action` method issues a prompt, tool methods on all domain objects are available to the LLM.You can also add additional tool methods with the `withToolObjects` method on `PromptRunner`.Domain objects may or may not be persistent.
If persistent, they will likely be stored in a familiar JVM technology such as JPA or JDBC.
We advocate the use of Spring Data patterns and repositories, although you are free to use any persistence technology you like.