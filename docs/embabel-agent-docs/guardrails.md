Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.30. Working with Guardrails
Guardrails have become an essential component in agentic AI systems.
They allow you to validate user inputs and LLM responses using configurable policies.
For vendor-supported guardrails, see:
- Guardrails Hub
- Amazon Bedrock GuardrailsEmbabel provides a framework for building custom guardrails, enabling developers to integrate validation logic of their choice.
#### 4.30.1. Motivation
While you can validate user prompts or thinking blocks using custom validators, Embabel provides a standardized framework through the `withGuardRails` API.
Guardrails can be implemented as POJOs or Spring beans that implement Embabel’s guardrail interfaces.Common use cases for guardrails:
- **Input validation**: Validate user prompts with common, streaming, or thinking prompt runners
- **Response validation with thinking**: The thinking API provides access to LLM thinking blocks, even when the LLM cannot construct an object
- **Object response validation**: When the LLM constructs an object, you can still validate the output (the content being validated is the object’s JSON representation)
- **Streaming validation**: In streaming mode, `StreamingEvent.Thinking` provides direct access to LLM reasoning content via the `doOnNext` callback (see Working with Streams)A key benefit of this framework is access to the `Blackboard` object, which allows guardrail logic to consider other entities participating in the agentic workflow.
#### 4.30.2. Concepts

- `UserInputGuardRail` and `AssistantMessageGuardRail` interfaces define guardrails for user inputs and LLM responses, respectively
- Guardrails are registered using the `withGuardRails` API, which can be chained
- Guardrail validation returns a `ValidationResult` object
- Validation errors are sorted by `ValidationSeverity` level and logged at the corresponding level
- A `CRITICAL` severity level causes a `GuardRailViolationException` to be thrown for user input guardrails, preventing the LLM operation from executing
- By design, `createObjectIfPossible` handles exceptions gracefully and completes without constructing an object; however, `GuardRailViolationException` is wrapped inside `ThinkingResponse` when using thinking mode
#### 4.30.3. Customizing Message Combining
In multi-turn conversations, guardrails often need to validate not just a single prompt but an entire conversation history. When `doTransform` or similar methods are called with multiple `UserMessage` objects, each `UserInputGuardRail` receives the full list and must combine them into a single string for validation.The `combineMessages` method controls how this combination happens. Different guardrails may need different formats:
- A toxicity filter might want all messages concatenated to check the overall tone
- An audit guardrail might want each message tagged with its position in the conversation
- A PII detector might want clear separators to identify which message contains sensitive dataThe default implementation joins messages with newlines:JavaKotlinFor example, three messages `["Hello", "How are you?", "Tell me about X"]` become:
```
Hello
How are you?
Tell me about X
```
To customize this behavior, override `combineMessages` in your guardrail:JavaKotlin
#### 4.30.4. Example: Blocking LLM Execution with CRITICAL Validation Errors
This example demonstrates how a guardrail with `CRITICAL` severity prevents LLM execution by throwing a `GuardRailViolationException`.**Step 1: Define the guardrails**First, define a user input guardrail that returns a `CRITICAL` validation error:JavaKotlinNext, define an assistant message guardrail to validate LLM responses:JavaKotlin**Step 2: Use the guardrails with a PromptRunner**JavaKotlin
#### 4.30.5. Example: Using Guardrails for Response Analysis
When the LLM cannot construct an object (for example, when the prompt is ambiguous), guardrails can still analyze the LLM’s thinking process. This is useful for understanding why object creation failed or for extracting insights from the reasoning.**Step 1: Define a simple user input guardrail**JavaKotlin**Step 2: Use guardrails with createObjectIfPossible**JavaKotlinWhen the LLM cannot provide a definitive answer, you might see reasoning like:
```
Since I must be SURE about EVERY field and cannot make assumptions or provide approximate values,
I cannot provide the success structure with confidence.
```
Guardrails can automate further analysis of LLM responses, for example by using semantic text processing tools like CoreNLP.For more examples, see:
```
embabel-agent-autoconfigure/models/embabel-agent-anthropic-autoconfigure/
 src/test/java/com/embabel/agent/config/models/anthropic/LLMAnthropicThinkingIT.java
```

#### 4.30.6. Global Guardrails Configuration
In addition to attaching guardrails per-call via `withGuardRails(…​)`, Embabel supports declaring **global** guardrails through application properties.
A global guardrail is instantiated once at startup and applied to every LLM operation, in addition to any interaction-specific guardrails configured on the `PromptRunner`.This is useful for cross-cutting safety policies that should always run, regardless of which call site invokes the LLM (PII redaction, profanity filtering, cost limits, audit logging, etc.).
##### Property Configuration
Global guardrails are defined as comma-separated, fully-qualified class names in `application.properties` (or `application.yml`):Each class must:
- Implement the appropriate interface (`UserInputGuardRail` for `user-input`, `AssistantMessageGuardRail` for `assistant-message`)
- Provide a public no-arg constructor (instances are created via `BeanUtils.instantiateClass`)
| | Guardrails registered through these properties are **plain POJOs**, **not** Spring-managed beans. The registry calls `BeanUtils.instantiateClass(…​)` directly, so:
- `@Autowired`, `@Value`, and constructor injection of Spring beans **do not work**
- `@PostConstruct` / `@PreDestroy` lifecycle callbacks are **not invoked**
- `ApplicationContextAware`, `BeanNameAware`, and similar `*Aware` callbacks are **not invoked**
- Dynamic access patterns (e.g. composing the `name` field from a Spring-backed holder, or pulling a config bean via a static `ApplicationContext` accessor) will fail or return `null` at startup, because the holder may not be initialized yetIf your guardrail genuinely needs Spring dependencies, the recommended pattern is to bridge the `ApplicationContext` through a small `ApplicationContextAware` holder, and have the guardrail look up beans or properties **at validation time** rather than in the constructor (see Accessing Spring Beans from a POJO Guardrail). Alternatively, declare the guardrail as a `@Component` and attach it per-call through `withGuardRails(…​)` on the `PromptRunner`. |

##### Accessing Spring Beans from a POJO Guardrail
When a property-registered guardrail must consult Spring beans or environment properties (for example, a cost limit defined in `application.yml`, or a shared `MeterRegistry`), expose the `ApplicationContext` through a static holder and let the guardrail call back into it at validation time.A guardrail can then resolve its dependencies lazily, inside `validate(…​)`:JavaKotlin
| | Resolve Spring dependencies **inside `validate(…​)`**, not in the constructor or in field initializers. At construction time, the order in which the `SpringContextHolder` bean and the `GlobalGuardRailsRegistry` are processed is not guaranteed, so the static `context` field may still be `null`. By the time `validate(…​)` is invoked the holder is fully wired. |

##### Merging with Interaction-Specific Guardrails
When an LLM operation executes, global guardrails are merged with the interaction-specific guardrails set through `withGuardRails(…​)`:
- Global guardrails always run first, then interaction-specific ones
- Duplicates are removed based on **class identity** (not the `name` field), so a class registered both globally and per-call will be invoked only once
- The deduplication keeps the global instance, ensuring singleton semantics
##### Strict Mode: `fail-on-error`
By default (`fail-on-error=false`), if a configured guardrail cannot be instantiated (typo in the class name, missing no-arg constructor, constructor throws), the error is logged and the application continues with the remaining guardrails.Setting `fail-on-error=true` causes Spring startup to fail with a `GuardRailInstantiationException` if any of the following occurs:
- The class cannot be loaded
- The class does not implement the expected guardrail interface
- The constructor throws an exceptionStrict mode is recommended in production where missing a guardrail must be treated as a deployment error rather than a silent omission.
##### Example: Defining a Global Guardrail
JavaKotlinOnce registered in `application.properties`, the guardrail applies to every LLM call — no code changes are needed at the call site:
##### Programmatic Access
The registry can also be accessed programmatically, either as an injected Spring bean or via its companion accessor:JavaKotlin
#### 4.30.7. Relationship with Other Validation Mechanisms
The Agent API framework supports Jakarta Bean Validation (JSR-380) for domain object constraints. These constraints are injected into the schema and validated during object construction.In addition, a planned validation framework for Agent Actions will reuse the same data structures as guardrails, including `ValidationResult`, `ValidationError`, and `ContentValidator`.In summary, guardrails and bean validators are complementary but distinct:
- **Bean validation** ensures objects are well-formed and meet business constraints
- **Guardrails** ensure AI interactions are safe and compliant with policiesBoth can be enabled independently and serve different aspects of the AI safety stack.`@SecureAgentTool` is a third, orthogonal mechanism: it enforces *access control* rather than
content safety or data validity.
Where guardrails ask "is this content acceptable?", `@SecureAgentTool` asks "is this principal
allowed to invoke this agent action at all?"
The two work well together — `@SecureAgentTool` prevents unauthorised principals from calling
a tool, while guardrails validate the inputs and outputs of calls that are permitted.
See `@SecureAgentTool` for details.