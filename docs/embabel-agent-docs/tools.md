Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.9. Tools
Tools can be passed to LLMs to allow them to perform actions.
Tools can either be outside the JVM process, as with MCP, or inside the JVM process, as with domain objects exposing `@LlmTool` methods.Embabel allows you to provide tools to LLMs in two ways:
- Via the `PromptRunner` by providing one or more in process **tool instances**.
A tool instance is an object with methods annotated with Embabel `@LlmTool` or Spring AI `@Tool`.
- At action or `PromptRunner` level, from a **tool group**.`LlmReference` implementations also expose tools, but this is handled internally by the framework.
#### 4.9.1. In Process Tools: Implementing Tool Instances
Implement one or more methods annotated with `@LlmTool` on a class.
You do not need to annotate the class itself.
Each annotated method represents a distinct tool that will be exposed to the LLM.A simple example of a tool method:JavaKotlinClasses implementing tools can be stateful.
They are often domain objects.
Tools on mapped entities are especially useful, as they can encapsulate state that is never exposed to the LLM.
See Domain Tools: Direct Access, Zero Ceremony for a discussion of tool use patterns.The `@Tool` annotation comes from Spring AI.Tool methods can have any visibility, and can be static or instance scope.
They are allowed on inner classes.You can define any number of arguments for the method (including no argument) with most types (primitives, POJOs, enums, lists, arrays, maps, and so on).
Similarly, the method can return most types, including void.
If the method returns a value, the return type must be a serializable type, as the result will be serialized and sent back to the model.The following types are not currently supported as parameters or return types for methods used as tools:
- Optional
- Asynchronous types (e.g. CompletableFuture, Future)
- Reactive types (e.g. Flow, Mono, Flux)
- Functional types (e.g. Function, Supplier, Consumer).
— Spring AITool CallingYou can obtain the current `AgentProcess` in a Tool method implementation via `AgentProcess.get()`.
This enables tools to bind to the `AgentProcess`, making objects available to other actions.
For example:JavaKotlin
#### 4.9.2. Receiving Out-of-Band Context in Tools
Tool methods often need access to infrastructure metadata—auth tokens, tenant IDs, correlation IDs—that should not be part of the LLM-facing JSON schema.
`ToolCallContext` provides this: an immutable key-value bag that flows through the tool pipeline without the LLM ever seeing it.Think of it like HTTP headers on a request.
The caller sets them at the boundary (a REST filter, an event handler), and every handler in the chain can read them—but the request body (what the LLM provides) is unaffected.
##### Injecting ToolCallContext into @LlmTool Methods
Declare a `ToolCallContext` parameter on any `@LlmTool` method.
The framework will:
- **Inject** the current context at call time (or `ToolCallContext.EMPTY` if none was set)
- **Exclude** the parameter from the JSON schema the LLM seesJavaKotlinThe LLM sees only the `customerId` parameter.
The `ToolCallContext` parameter is invisible in the tool’s schema.This works for both `KotlinMethodTool` and `JavaMethodTool`—the `ToolCallContext` parameter can appear at any position in the method signature.
##### Setting Context via ProcessOptions
Context is set at the process boundary using `ProcessOptions.withToolCallContext()`.
It then propagates to every tool invocation in the process—including MCP tools, where it bridges to Spring AI’s `ToolContext`.JavaKotlin
##### Context Propagation Through Decorators
`ToolCallContext` flows automatically through decorator chains.
Any tool implementing `DelegatingTool` forwards the context to its delegate by default.
Built-in decorators like `ArtifactSinkingTool` and `ReplanningTool` follow this pattern, so context reaches the underlying tool without any extra wiring.
##### Per-Loop One-Shot Tools (`OneShotPerLoopTool`)
Some tools are meant to fire **at most once per agentic loop iteration** — typically because the call returns content that, once delivered, lives in the LLM’s conversation history for the rest of the turn.
The canonical example is a **skill activator**: calling it returns the skill body so the LLM can use it; calling it again returns the same body and accomplishes nothing except wasting tokens and a round-trip.Stronger models follow a system-prompt rule like "call each activator once" reliably.
Weaker models (qwen, gpt-oss, smaller open models) reflexively re-call the same tool turn after turn even when the body is already in conversation history.
The system-prompt rule isn’t enforceable purely with words — `OneShotPerLoopTool` makes the constraint mechanical.Wrap the underlying tool with `OneShotPerLoopTool`, supplying an `advice` string that tells the LLM what to do **instead** of calling again:JavaKotlinThe first call within a given loop delegates to the underlying tool as normal.
Every subsequent call within the same loop short-circuits with:
```
ALREADY LOADED. The body of '<tool name>' was returned earlier in this turn —
read it from your conversation history above. Do not call this tool again.
<advice>
```
Loop scoping is provided by `LoopMemo` reading `ToolCallContext.loopId()`, so the orchestrator must stamp a fresh loop id per turn:If no loop id is stamped, `LoopMemo’s documented fallback is "always emit" — every call is treated as the first, so the wrapper degrades to a passthrough rather than silently locking.Implements `DelegatingTool`, so the underlying tool is reachable via `delegate` and the canonical two-arg `call` overload is the only one a subclass would override.For the underlying memoisation primitive in isolation (e.g. for "first describe per loop emits the rules block once" inside a tool’s own `call`), see `LoopMemo`.
##### Using Context in Framework-Agnostic Tools
For programmatically created tools, use `Tool.ContextAwareFunction` to receive context in the handler.
The `Tool.of()` factory method accepts a `ContextAwareFunction` as the last parameter:JavaKotlinWhen no context is provided, the function receives `ToolCallContext.EMPTY`.
| | Context is immutable and safe to read from any thread.
If you need to pass context from a web request through to tool invocations, set it once on `ProcessOptions` and every tool in the process will receive it. |

##### Setting Context per Interaction via PromptRunner
For context that is specific to one LLM call rather than the whole agent run, use `withToolCallContext()` on the `PromptRunner` directly inside an `@Action` method.
This is the right place for domain-level metadata that belongs to a particular interaction — for example, which entity the action is working on.JavaKotlin
| **1** | `withToolCallContext()` also accepts a plain `Map<String, Any>` for convenience. |

##### Context Merge Semantics
Context from both sources is merged automatically in `ToolLoopLlmOperations.resolveToolCallContext()`.
Interaction-level values win on conflict.
| `ProcessOptions` | `PromptRunner` | Effective context || `tenantId=acme` | — | `{tenantId=acme}` || — | `authToken=xyz` | `{authToken=xyz}` || `tenantId=acme` | `authToken=xyz` | `{tenantId=acme, authToken=xyz}` || `tenantId=acme` | `tenantId=override` | `{tenantId=override}` — interaction wins |
This means `ProcessOptions` is the right place for cross-cutting infrastructure concerns (tenant routing, correlation IDs, credentials injected at the gateway), while `PromptRunner.withToolCallContext()` is the right place for domain-specific per-interaction concerns (which entity the action is working on).
##### Controlling What Crosses the MCP Boundary
When Embabel calls a remote MCP server, the `ToolCallContext` entries are forwarded as MCP `_meta` on the wire.
`_meta` is a first-class field in the MCP 2025-06-18 specification, and MCP server tools can receive it via `McpMeta` parameters (or Spring AI’s `ToolContext`).By default **all** context entries are forwarded (`passThrough` behavior).
For production deployments calling untrusted third-party MCP servers, register a `ToolCallContextMcpMetaConverter` bean to control what crosses the process boundary.Think of it like an HTTP header filter on a reverse proxy: the converter decides which entries are safe to propagate and which should stay local.JavaKotlinIf no bean is defined, the framework defaults to `passThrough()` for backward compatibility.
The available factory methods are:
| Method | Behavior | Use Case || `passThrough()` | Forwards all entries | Fully trusted internal MCP servers (default) || `noOp()` | Forwards nothing | Zero-trust: block all metadata from crossing || `allowKeys(vararg keys)` | Forwards only named keys | Production (recommended): explicit allowlist || `denyKeys(vararg keys)` | Forwards all except named keys | When secrets are well-known and enumerable |

| | The cleanest security approach is to put only safe values into `ToolCallContext` in the first place — secrets belong in `@Value` / Vault / environment variables, not in the context bag.
`ToolCallContextMcpMetaConverter` is the secondary defence for cases where the context is populated at the infrastructure boundary and not all entries should reach third-party servers. |

#### 4.9.3. Tool Groups
Embabel introduces the concept of a **tool group**.
This is a level of indirection between user intent and tool selection.
For example, we don’t ask for Brave or Google web search: we ask for "web" tools, which may be resolved differently in different environments.
| | Tools use should be focused.
Thus tool groups are not specified at agent level, but on individual actions. |
Tool groups are often backed by MCP.
##### Configuring Tool Groups in configuration files
If you have configured MCP servers in your application configuration, you can selectively expose tools from those servers to agents by configuring tool groups.
The easiest way to do this is in your `application.yml` or `application.properties` file.
Select tools by name.For example:
##### Configuring Tool Groups in Spring @Configuration
You can also use Spring’s `@Configuration` and `@Bean` annotations to expose ToolGroups to the agent platform with greater control.
The framework provides a default `ToolGroupsConfiguration` that demonstrates how to inject MCP servers and selectively expose MCP tools:JavaKotlin
| **1** | This method creates a Spring bean of type `ToolGroup`.
This will automatically be picked up by the agent platform, allowing the tool group to be requested by name (role). |

##### Key Configuration Patterns
**MCP Client Injection:**
The configuration class receives a `List<McpSyncClient>` via constructor injection.
Spring automatically provides all available MCP clients that have been configured in the application.**Selective Tool Exposure:**
Each `McpToolGroup` uses a `filter` lambda to control which tools from the MCP servers are exposed to agents.
This allows fine-grained control over tool availability and prevents unwanted or problematic tools from being used.**Tool Group Metadata:**
Tool groups include descriptive metadata like `name`, `provider`, and `description` to help agents understand their capabilities.
The `permissions` property declares what access the tool group requires (e.g., `INTERNET_ACCESS`).
##### Creating Custom Tool Group Configurations
Applications can implement their own `@Configuration` classes to expose custom tool groups, which can be backed by any service or resource, not just MCP.JavaKotlinThis approach leverages Spring’s dependency injection to provide tool groups with the services and resources they need, while maintaining clean separation of concerns between tool configuration and agent logic.
##### Using Tools in Action Methods
Tools are specified on the `PromptRunner` when making LLM calls.
This gives you fine-grained control over which tools are available for each specific prompt.Here’s an example from the `StarNewsFinder` agent that demonstrates web tool usage within an action:JavaKotlin
##### Key Tool Usage Patterns
**PromptRunner Tool Methods:**
Tools are added to the `PromptRunner` fluent API using methods like `withToolGroup()`, `withTools()`, and `withToolObject()`.**Multiple Tool Groups:**
Actions can add multiple tool groups by chaining `withToolGroup()` calls when they need different types of capabilities.**Tool-Aware Prompts:**
Prompts should explicitly instruct the LLM to use the available tools.
For example, "use web tools and generate search queries" clearly directs the LLM to utilize the web search capabilities.
##### Additional PromptRunner Examples
JavaKotlin**Adding Tool Objects with @LlmTool Methods:**You can also provide domain objects with `@LlmTool` methods directly to specific prompts:JavaKotlin**Available PromptRunner Tool Methods:**
- `withToolGroup(String)`: Add a single tool group by name
- `withToolGroup(ToolGroup)`: Add a specific ToolGroup instance
- `withToolGroups(Set<String>)`: Add multiple tool groups
- `withTools(vararg String)`: Convenient method to add multiple tool groups
- `withToolObject(Any)`: Add domain object with `@LlmTool` or `@Tool` methods
- `withToolObject(ToolObject)`: Add ToolObject with custom configuration
- `withTool(Tool)`: Add a framework-agnostic Tool instance
- `withTools(List<Tool>)`: Add multiple framework-agnostic Tool instances
#### 4.9.4. Framework-Agnostic Tool Interface
In addition to Spring AI’s `@Tool` annotation, Embabel provides its own framework-agnostic `Tool` interface in the `com.embabel.agent.api.tool` package.
This allows you to create tools that are not tied to any specific LLM framework, making your code more portable and testable.The `Tool` interface includes nested types to avoid naming conflicts with framework-specific types:
- `Tool.Definition` - Describes the tool (name, description, input schema)
- `Tool.InputSchema` - Defines the parameters the tool accepts
- `Tool.Parameter` - A single parameter with name, type, and description
- `Tool.Result` - The result returned by a tool (text, artifact, or error)
- `Tool.Handler` - Functional interface for implementing tool logic
##### Creating Tools Programmatically
You can create tools using the `Tool.create()` factory methods:JavaKotlinThe `Tool.Parameter` class provides factory methods for common parameter types:
- `Tool.Parameter.string(name, description)` - String parameter
- `Tool.Parameter.string(name, description, required)` - String with optional flag
- `Tool.Parameter.string(name, description, required, enumValues)` - String with allowed values
- `Tool.Parameter.integer(name, description)` - Integer parameter
- `Tool.Parameter.double(name, description)` - Floating-point parameterAll factory methods default to `required = true`.
##### Creating Strongly Typed Tools
For tools with complex input and output structures, use `Tool.fromFunction()` to work with domain objects directly.
The input schema is generated automatically from the input type, and JSON marshaling is handled for you.JavaKotlinYou can also instantiate `TypedTool` directly:Key features of typed tools:
- **Automatic schema generation**: The input schema is derived from the input type’s structure
- **JSON marshaling**: Input JSON is deserialized to the input type, and output is serialized from the output type
- **String pass-through**: If the output type is `String`, it’s returned directly without JSON serialization
- **Result pass-through**: If the function returns a `Tool.Result`, it’s used as-is
- **Exception handling**: Exceptions thrown by the function are converted to `Tool.Result.Error`
- **Control flow signals**: Exceptions implementing `ToolControlFlowSignal` (like `ReplanRequestedException`) propagate through
##### Creating Tools from Annotated Methods
Embabel provides `@LlmTool` and `@LlmTool.Param` annotations for creating tools from annotated methods.
This approach is similar to Spring AI’s `@Tool` but uses Embabel’s framework-agnostic abstractions.JavaKotlinThe `@LlmTool` annotation supports:
- `name`: Tool name (defaults to method name if empty)
- `description`: Description of what the tool does (required)
- `returnDirect`: Whether to return the result directly without further LLM processingThe `@LlmTool.Param` annotation supports:
- `description`: Description of the parameter (helps the LLM understand what to provide)
- `required`: Whether the parameter is required (defaults to true)
##### Adding Framework-Agnostic Tools via PromptRunner
Use `withTool()` or `withTools()` to add framework-agnostic tools to a `PromptRunner`:JavaKotlin
##### Tool Results
Tools return `Tool.Result` which can be one of three types:JavaKotlin
##### Modifying Tool Descriptions
Tools provide `withDescription()` and `withNote()` methods to create copies with modified descriptions.
This is useful when you need to customize a tool’s description for a specific context without modifying the original tool.**withDescription(newDescription)**Creates a new tool with a completely replaced description:JavaKotlin**withNote(note)**Creates a new tool with an appended note to the existing description:JavaKotlinBoth methods preserve all other tool properties (name, input schema, metadata, functionality):JavaKotlin
##### When to Use Each Approach

| Approach | Use When || Spring AI `@Tool` | You’re comfortable with Spring AI and want IDE support for tool annotations on domain objects || `Tool.create()` / `Tool.of()` | You need programmatic tool creation with simple inputs, want framework independence, or are creating tools dynamically || `Tool.fromFunction()` | You need programmatic tool creation with complex typed inputs and outputs, automatic JSON marshaling, and schema generation || `@LlmTool` / `@LlmTool.Param` | You prefer annotation-based tools but want Embabel’s framework-agnostic abstractions || Tool Groups | You need to organize related tools, use MCP servers, or control tool availability at deployment time |

#### 4.9.5. Tool Decoration: Extending Tool Behavior
Embabel uses a powerful decoration pattern to extend tool behavior without modifying the underlying tool or complicating the `PromptRunner`.
A decorated tool wraps another tool, intercepting calls to add functionality like artifact capture, event publishing, or blackboard integration.This pattern is fundamental to Embabel’s architecture:
- **Subagents** use decoration to wrap agent execution as a tool
- **Asset tracking** uses decoration to capture tool outputs for chatbot interfaces
- **Blackboard publishing** uses decoration to make tool results available to other actions
- **Event streaming** uses decoration to publish tool calls to external systems
- Internal platform features like observability and exception handling also use decoration
##### The DelegatingTool Interface
All tool decorators implement `DelegatingTool`:JavaKotlinThis allows decorators to be unwrapped when needed, and enables chaining multiple decorators.
##### ArtifactSinkingTool: Capturing Tool Outputs
`ArtifactSinkingTool` captures artifacts from `Tool.Result.WithArtifact` results and sends them to a sink.
This is the foundation for making structured tool outputs available elsewhere.JavaKotlin
##### Built-in Sinks
Embabel provides several `ArtifactSink` implementations:
| Sink | Purpose || `BlackboardSink` | Publishes to the current `AgentProcess` blackboard, making artifacts available to other actions || `ListSink` | Collects artifacts into a list, useful for aggregating results || `CompositeSink` | Delegates to multiple sinks, enabling multi-destination publishing |

##### Creating Custom Sinks
Implement `ArtifactSink` to create custom destinations:JavaKotlin
##### How Decoration Enables Extension
The decoration pattern lets Embabel add sophisticated behavior while keeping `PromptRunner` simple.
When you use `Subagent.ofClass(MyAgent.class)` (Java) or `Subagent.ofClass(MyAgent::class.java)` (Kotlin), Embabel creates a tool that:
1.Wraps agent execution in a `Tool.call()` method
1.Shares the parent blackboard with the child process
1.Captures the agent’s result as a tool artifactSimilarly, when you configure asset tracking in a chatbot, Embabel wraps tools with `AssetAddingTool` to capture outputs as viewable assets.This approach has key advantages:
- **Composable**: Multiple decorators can be chained
- **Transparent**: The underlying tool doesn’t know it’s wrapped
- **Extensible**: New behaviors can be added without framework changes
- **Type-safe**: Generic decorators like `ArtifactSinkingTool<T>` preserve type information
#### 4.9.6. Subagent: Agent Handoffs as Tools
A `Subagent` is a specialized `Tool` that delegates to another Embabel agent.
When the LLM invokes this tool, it runs the specified agent as a subprocess, sharing the parent process’s blackboard context.
This enables composition of agents and "handoff" patterns where one agent delegates specialized tasks to another.
##### Creating Subagents
Subagent uses a fluent builder pattern.
First select how to reference the agent, then specify the input type using `consuming()`:JavaKotlinThe `consuming()` method specifies the input type that the LLM will provide when invoking this tool.
This type is used to generate the JSON schema that guides the LLM’s tool invocation.
##### Using Subagents with PromptRunner
Use `withTool()` to add a Subagent to your prompt:JavaKotlin
| **1** | The LLM can now invoke `PerformanceFinder` as a tool, providing `WorksToFind` input to delegate the performance search task. |

##### Subagent with Asset Tracking
For chat applications that track assets, wrap the Subagent with `AssetAddingTool` to automatically track returned artifacts:JavaKotlin
| **1** | Wrap with `addReturnedAssets()` to track artifacts returned by the subagent. |

##### Input Type and JSON Schema
The input type you specify with `consuming()` determines the JSON schema that the LLM sees when invoking the tool.For example:JavaKotlinThe Subagent tool will:
- Use "PerformanceFinder" as the tool name (from `@Agent` annotation)
- Use "Finds performances" as the tool description (from `@Agent` annotation)
- Generate a JSON schema from `WorksToFind`**From the LLM’s perspective, a Subagent is just another tool.**
The calling LLM sees the JSON schema for `WorksToFind` and can populate it directly:When the tool is invoked, Subagent deserializes this JSON into a `WorksToFind` object and passes it to the target agent.
The input type should match the first non-injected parameter of the agent’s entry-point action.
##### Blackboard Sharing
When a Subagent runs, it receives a **spawned blackboard** from the parent process.
This means:
- The subagent can read objects from the parent’s blackboard
- Objects added by the subagent are available to the parent after the subagent completes
- The subagent operates in its own process context but shares state appropriately
##### When to Use Subagent

| Scenario | Recommendation || Complex specialized task that has its own multi-action workflow | Use Subagent - the target agent can plan and execute multiple steps || Simple tool call with deterministic logic | Use a regular `@LlmTool` method instead || LLM-orchestrated mini-workflow with sub-tools | Consider AgenticTool which operates at the tool level || Need the full power of GOAP planning for the subtask | Subagent is ideal - the target agent uses its own planner |

#### 4.9.7. Agentic Tools
An **agentic tool** is a tool that uses an LLM to orchestrate other tools.
Unlike a regular tool which executes deterministic logic, an agentic tool delegates to an LLM that decides which sub-tools to call based on a prompt.This pattern is useful for encapsulating a mini-orchestration as a single tool that can be used in larger workflows.Embabel provides three agentic tool implementations, each offering different levels of control over tool availability:
##### Choosing an Agentic Tool

| Tool Type | Tool Availability | Best For | Example Use Case || `SimpleAgenticTool` | All tools available immediately | Simple orchestration, exploration tasks | Math calculator with add/multiply/divide tools || `PlaybookTool` | Progressive unlock via conditions (prerequisites, artifacts, blackboard) | Structured workflows, guided processes | Research workflow: search → analyze → summarize || `StateMachineTool` | State-based availability using enum states | Formal state machines, multi-phase processes | Order processing: DRAFT → CONFIRMED → SHIPPED → DELIVERED |
All three implement the `AgenticTool` interface and share a common fluent API with `with*` methods.The `AgenticTool` interface defines:JavaKotlinThe `AgenticSystemPromptCreator` functional interface receives both the `ExecutingOperationContext` (for access to blackboard, process options, etc.) and the input string passed to the tool:JavaKotlin
| | For complex workflows with defined outputs, branching logic, loops, or state management, use Embabel’s GOAP planner, Utility AI, or @State workflows instead.
These provide deterministic, typesafe planning that is far more powerful and predictable than LLM-driven orchestration. |

##### SimpleAgenticTool: Flat Tool Orchestration
`SimpleAgenticTool` makes all sub-tools available immediately.
The LLM decides freely which tools to use based on the prompt.JavaKotlin
##### PlaybookTool: Conditional Tool Unlocking
`PlaybookTool` allows tools to be progressively unlocked based on conditions:
- **Prerequisites**: unlock after other tools have been called
- **Artifacts**: unlock when certain artifact types are produced
- **Blackboard**: unlock based on process state
- **Custom predicates**: unlock based on arbitrary conditionsJavaKotlinWhen a locked tool is called before its conditions are met, the LLM receives an informative message guiding it to use prerequisite tools first.
##### StateMachineTool: State-Based Availability
`StateMachineTool` uses explicit states defined by an enum.
Tools are registered with specific states where they’re available, and can trigger transitions to other states.JavaKotlinThe `startingIn(state)` method allows starting in a different state at runtime:JavaKotlin
##### Domain Tools: Tools from Retrieved Objects
All three agentic tools support **domain tools** - `@LlmTool` methods on domain objects that become available when a single instance is retrieved.JavaKotlinDomain tools are "declared" to the LLM immediately but return an error until an instance is bound.
When a tool returns a **single** artifact (not a collection) of a registered type, that instance is bound and its `@LlmTool` methods become executable.
##### Creating Agentic Tools
Create agentic tools using the constructor and fluent `with*` methods:JavaKotlin
| | The `withSystemPrompt` call is optional.
By default, agentic tools generate a system prompt from the tool’s description: *"You are an intelligent agent that can use tools to help you complete tasks.
Use the provided tools to perform the following task: {description}"*.
Only call `withSystemPrompt` if you need custom orchestration instructions. |

##### Defining Input Parameters

| | You must define input parameters for your agentic tool so the LLM knows what arguments to pass when calling it.
Without parameters, the LLM won’t know what input format to use. |
Use the `withParameter` method with `Tool.Parameter` factory methods for concise parameter definitions:JavaKotlinAvailable parameter factory methods:
- `Tool.Parameter.string(name, description, required?)` - String parameter
- `Tool.Parameter.integer(name, description, required?)` - Integer parameter
- `Tool.Parameter.double(name, description, required?)` - Floating-point parameterAll factory methods default to `required = true`.
Set `required = false` for optional parameters.
##### Creating Agentic Tools from Annotated Objects
Use `withToolObject` or `withToolObjects` to add tools from objects with `@LlmTool`-annotated methods:JavaKotlinObjects without `@LlmTool` methods are silently ignored, allowing you to mix objects safely.
##### Agentic Tools with Spring Dependency Injection
Agentic tools can encapsulate stateful services via dependency injection:JavaKotlin
##### How Agentic Tools Execute
When an agentic tool’s `call()` method is invoked:
1.The tool retrieves the current `AgentProcess` context
1.It configures a `PromptRunner` with the specified `LlmOptions`
1.It adds all sub-tools to the prompt runner
1.It executes the prompt with the input, allowing the LLM to orchestrate the sub-tools
1.The final LLM response is returned as the tool resultThis means agentic tools create a nested LLM interaction: the outer LLM decides to call the agentic tool, then the inner LLM orchestrates the sub-tools.
##### Modifying Agentic Tools
Use the `with*` methods to create modified copies:JavaKotlinThe available modification methods are:
- `withParameter(Tool.Parameter)`: Add an input parameter (use `Tool.Parameter.string()`, `.integer()`, `.double()`)
- `withLlm(LlmOptions)`: Set LLM configuration
- `withTools(vararg Tool)`: Add additional Tool instances
- `withToolObject(Any)`: Add tools from an object with `@LlmTool` methods
- `withToolObjects(vararg Any)`: Add tools from multiple annotated objects
- `withSystemPrompt(String)`: Set a fixed system prompt
- `withSystemPrompt((ExecutingOperationContext, String) → String)`: Set a dynamic prompt based on execution context and input
- `withCaptureNestedArtifacts(Boolean)`: Control whether artifacts from nested agentic tool calls are captured (default: `false`)
- `withToolChainingFrom(Class<T>)`: Register a class whose `@LlmTool` methods become available when an artifact of that type is returned
- `withToolChainingFrom(Class<T>, DomainToolPredicate<T>)`: Register with a predicate to filter which instances contribute tools
- `withToolChainingFromAny()`: Auto-discover tools from any returned artifact with `@LlmTool` methods
##### Controlling Artifact Capture in Nested Agentic Tools
When an agentic tool orchestrates other tools, those sub-tools may return artifacts (via `Tool.Result.WithArtifact`).
By default, artifacts from nested agentic tool calls are **not** captured—only the final result from the outermost agentic tool is returned.This prevents intermediate artifacts from bubbling up when you only care about the final result.
For example, if an outer `assembleConcert` tool calls an inner `findPerformances` tool, you typically want only the final `Concert` artifact, not all the intermediate `Performance` artifacts.Use `withCaptureNestedArtifacts(true)` if you need to capture artifacts from nested agentic tools:JavaKotlin
| | This setting only affects artifacts from nested agentic tool calls.
Artifacts from regular (non-agentic) tools are always captured. |

##### Tool Chaining
When working with objects returned by tools, you often want to expose `@LlmTool` methods on those objects as additional tools—but only after the object has been retrieved.
The `withToolChainingFrom()` method enables this pattern.
| | Tool chaining increases determinism.
Once a tool returns a specific object, the LLM gains access to that object’s business methods—navigating a data structure through well-defined operations rather than unstructured reasoning.
This keeps the LLM on a guided path through your domain logic. |
Tool chaining is available on both `AgenticTool` and `PromptRunner`, via the shared `ToolChaining` interface.
This means you can use tool chaining not only in agentic tool loops, but also in simple `createObject` and `generateText` calls through `PromptRunner`.
This is significant because it enables any action to dynamically discover and use tools from returned artifacts without requiring a full agentic tool setup.When you register a class, placeholder tools are created for each `@LlmTool` method on that class.
Initially, these tools return "not available yet" messages.
When a tool returns an artifact matching the registered type, the placeholder tools become active and delegate to the bound instance.**Last Wins Semantics**: When multiple artifacts of the same type are returned, only the most recent one’s tools are active.
This ensures the LLM always works with the "current" instance.JavaKotlin
###### Predicate-Based Filtering
You can control which instances contribute tools using a predicate.
The predicate receives the artifact and the current `AgentProcess`, allowing filtering based on object state or process context.JavaKotlin
###### Auto-Discovery Mode
For maximum flexibility, use `withToolChainingFromAny()` to automatically discover and expose tools from any returned artifact that has `@LlmTool` methods.
Unlike registered sources, auto-discovery replaces ALL previous bindings when a new artifact is discovered—ensuring only one "current" object’s tools are active at a time.JavaKotlinThis pattern is useful when:
- **Objects have operations**: The object itself knows how to perform actions (e.g., `user.updateEmail()`, `order.cancel()`)
- **Context-dependent tools**: Operations only make sense after retrieving a specific instance
- **Clean API design**: Tools are defined on the class rather than as separate tool classes
- **Exploratory workflows**: The LLM dynamically works with whatever object is "current"All agentic tool types support tool chaining:
- `SimpleAgenticTool`: Chained tools are available as soon as an artifact is returned
- `PlaybookTool`: Chained tools are available immediately (not subject to unlock conditions)
- `StateMachineTool`: Chained tools are available globally (not state-bound)
###### Tool Chaining on PromptRunner
Tool chaining is not limited to agentic tools.
Because both `AgenticTool` and `PromptRunner` implement the `ToolChaining` interface, you can use `withToolChainingFrom()` and `withToolChainingFromAny()` directly on a `PromptRunner` obtained from an action’s `OperationContext`.This is important because it enables dynamic tool discovery within simple `createObject` and `generateText` calls—without requiring a full `SimpleAgenticTool` wrapper.JavaKotlin
##### Filtering Artifacts for Asset Tracking
When using tools with an `AssetTracker` (common in chat applications), you can filter which artifacts become tracked assets.
The `addReturnedAssets` and `addAnyReturnedAssets` methods accept a `Predicate<Asset>` filter that works with both Java and Kotlin:JavaKotlinThe filter is applied after type matching, so you can use type-specific criteria to decide which artifacts are worth tracking.
##### Migration from Other Frameworks
If you’re coming from frameworks like LangChain or Google ADK, Embabel’s agentic tools provide a familiar pattern similar to their "supervisor" architectures:
| Framework | Pattern | Embabel Equivalent || LangChain/LangGraph | Supervisor agent with worker agents | `SimpleAgenticTool` with sub-tools || Google ADK | Coordinator with `sub_agents` / `AgentTool` | `SimpleAgenticTool` with sub-tools |
The key differences:
- **Tool-centric**: Embabel’s agentic tools operate at the tool level, not the agent level.
They’re lightweight and can be mixed freely with regular tools.
- **Simpler model**: No graph-based workflows or explicit Sequential/Parallel/Loop patterns—just LLM-driven orchestration.
- **Composable**: An agentic tool is still "just a tool" that can be used anywhere tools are accepted.However, for anything beyond simple orchestration, Embabel offers far more powerful alternatives:
| Scenario | Use This Instead || Business processes with defined outputs | GOAP planner - deterministic, goal-oriented planning with preconditions and effects || Exploration and event-driven systems | Utility AI - selects highest-value action at each step || Branching, looping, or stateful workflows | @State workflows - typesafe state machines with GOAP planning within each state |
These provide **deterministic, typesafe planning** that is far more predictable and powerful than supervisor-style LLM orchestration.
Use `SimpleAgenticTool` for simple cases, `PlaybookTool` for structured workflows, or `StateMachineTool` for formal state machines.
Graduate to GOAP, Utility, or @State for production workflows where predictability matters.
| | For supervisor-style orchestration with typed outputs and full blackboard state management, see SupervisorInvocation.
It operates at a higher level than agentic tools, orchestrating `@Action` methods rather than `Tool` instances, and produces typed goal objects with currying support. |

#### 4.9.8. Progressive Tools
Great fleas have little fleas upon their backs to bite 'em,
And little fleas have lesser fleas, and so ad infinitum.
And the great fleas themselves, in turn, have greater fleas to go on;
While these again have greater still, and greater still, and so on.
— Augustus De Morgan
**Progressive tools** enable dynamic tool disclosure—presenting a simplified interface initially, then revealing more granular tools based on context or when the LLM expresses intent.
##### The Progressive Tool Hierarchy
Embabel provides a hierarchy of progressive tool interfaces:
- **`ProgressiveTool`**: The base interface for tools that can reveal inner tools based on context. Its `innerTools(process: AgentProcess)` method returns tools that may vary depending on the current agent process state.
- **`UnfoldingTool`**: A `ProgressiveTool` with a fixed set of inner tools. When invoked, it "unfolds" to reveal its contents—like opening a folded map to see the details inside. This is the most commonly used progressive tool type.An `UnfoldingTool` presents a high-level description to the LLM and, when invoked, exposes its inner tools.
This pattern is useful for **progressive tool disclosure**—reducing initial complexity while allowing access to detailed functionality on demand.
##### When to Use UnfoldingTool
UnfoldingTool is useful when:
- You have many related tools that might overwhelm the LLM with choices
- You want to group tools by category (e.g., "database operations", "file operations")
- You want the LLM to express intent before revealing detailed options
- You need to reduce token usage for tool descriptions
##### Creating a Simple UnfoldingTool
The simplest form exposes all inner tools when invoked:JavaKotlin
##### Fluent Builder API
UnfoldingTool supports a fluent builder pattern for combining tools from multiple sources.
Use `withTools()` to add individual tools or `withToolObject()` to add tools from `@LlmTool` annotated objects:JavaKotlinThis is useful when:
- **Combining existing tools**: Merge tools from different sources into one progressive facade
- **Adding ad-hoc tools**: Start with annotated tool classes and add programmatic tools
- **Context-specific grouping**: Build different tool combinations for different invocation contextsThe builder preserves all properties (`childToolUsageNotes`, etc.) from the original UnfoldingTool.
##### Category-Based Tool Selection
Use `byCategory` to expose different tools based on the category the LLM selects:JavaKotlin
##### Custom Selection Logic
For more complex selection logic, use `selectable`:JavaKotlin
##### Guide Tool Behavior
When an UnfoldingTool is invoked, it is replaced by its inner tools plus a **guide tool** with the same name as the original facade.
If the LLM calls the parent tool name again on a subsequent turn (a common tool-calling mistake), the guide tool returns a listing of the available sub-tools instead of failing with a `ToolNotFoundException`.This behavior is automatic — no configuration needed. The `removeOnInvoke` property is deprecated and ignored; the guide tool replacement always applies.
##### Enabling UnfoldingTool in the Tool Loop
UnfoldingTool is **enabled by default** when using Embabel’s tool loop.
The `ToolInjectionStrategy.DEFAULT` includes `UnfoldingToolInjectionStrategy`, so no additional configuration is needed.If you need to combine with custom strategies, use `ChainedToolInjectionStrategy`:JavaKotlin
##### How UnfoldingToolWorks

1.**Initial state**: The LLM sees only the facade tool (e.g., "database_operations")
1.**LLM invokes**: The LLM calls the facade with optional arguments
1.**Strategy evaluates**: `UnfoldingToolInjectionStrategy` detects the invocation
1.**Tools replaced**: The facade is replaced by a guide tool and inner tools are added
1.**Continue**: The LLM now sees and can use the specific inner toolsThis flow reduces the initial tool set complexity while allowing the LLM to access detailed tools when it needs them.
##### Context Preservation and Usage Notes
When a UnfoldingTool is expanded, its child tools replace the facade.
Without context preservation, the LLM would lose important information about *why* these tools are grouped together.For example, a "spotify_search" tool containing `vector_search`, `text_search`, and `regex_search` would expand to just three generic search tools - the LLM wouldn’t know these are specifically for searching Spotify music data.Embabel solves this by automatically injecting a **context tool** alongside the child tools.
This context tool:
- Preserves the parent’s description ("Search Spotify for music data")
- Lists the available child tools
- Includes optional usage notes (via `childToolUsageNotes`)The `childToolUsageNotes` parameter provides guidance on when and how to use the child tools.
This guidance appears **once** in the context tool rather than being duplicated in each child tool’s description:JavaKotlinAfter the LLM invokes `spotify_search`, it will see:
- `vector_search` - the actual search tool
- `text_search` - the actual search tool
- `regex_search` - the actual search tool
- `spotify_search_context` - context tool with description and usage notesThe context tool’s description includes the original purpose and available tools.
When called, it returns full details about each child tool plus the usage notes - providing a single reference point without polluting individual tool descriptions.
##### Exclusive Mode
By default, when an UnfoldingTool is expanded, its inner tools are added alongside any sibling tools already in the tool set.
In some cases the LLM may ignore the inner tools and instead pick a sibling tool, defeating the purpose of the unfolding.Setting `exclusive = true` removes **all** other tools when the UnfoldingTool is expanded, so the LLM sees only the inner tools until the interaction ends.
Use this when the LLM consistently picks the wrong sibling tool instead of using the revealed inner tools.JavaKotlinWhen `exclusive` is false (the default), the parent tool is replaced by its inner tools and all sibling tools remain available.
When `exclusive` is true, every tool in the current tool set is removed and only the inner tools are injected.
##### Annotation-Based UnfoldingTool
For a more declarative approach, use the `@UnfoldingTools` class annotation combined with `@LlmTool` method annotations:JavaKotlinYou can also specify `childToolUsageNotes` in the annotation to provide guidance on using the child tools:JavaKotlin
##### Category-Based Selection with Annotations
Add `category` to `@LlmTool` annotations to automatically create a category-based UnfoldingTool:JavaKotlin
##### @UnfoldingTools Annotation Attributes

| Attribute | Type | Default | Description || `name` | String | Required | Name of the facade tool the LLM will see || `description` | String | Required | Description explaining the tool category || `removeOnInvoke`*(deprecated)* | boolean | `true` (ignored — always replaced by guide tool) | Whether to remove the facade after invocation || `categoryParameter` | String | `"category"` | Name of the parameter for category selection |

##### @LlmTool Category Attribute
The `category` attribute on `@LlmTool` is used when the containing class has `@UnfoldingTools`:
- Tools with the same category are grouped together
- Tools without a category are added to all category groups plus an "all" category
- If no tools have categories, a simple (non-category-based) UnfoldingTool is created
##### Real-World Example: Spotify Integration
Here’s a real-world example from the Impromptu chatbot that uses `@UnfoldingTools` to progressively disclose Spotify functionality:JavaKotlinWith this setup:
1.The LLM initially sees a single `spotify` tool
1.When the user says "play some jazz", the LLM invokes `spotify`
1.The `spotify` facade is replaced with all the inner tools (`getPlaylists`, `searchTracks`, `playTrack`, etc.)
1.The LLM can then call `searchTracks` or `playTrack` to fulfill the request
##### Auto-Detection with Tool.fromInstance()
When you use `Tool.fromInstance()` on a class annotated with `@UnfoldingTools`, it automatically creates an `UnfoldingTool`:JavaKotlinThis works seamlessly with `withToolObject()` on PromptRunner:JavaKotlin
##### Wrapping Tool Objects with fromToolObject()
`UnfoldingTool.fromInstance()` requires the class to be annotated with `@UnfoldingTools`.
This doesn’t work for objects like interface implementations with `@LlmTool` default methods that you cannot or should not annotate with `@UnfoldingTools`.Use `fromToolObject()` to wrap **any** object with `@LlmTool` methods into an `UnfoldingTool`, providing name and description explicitly:JavaKotlinAll standard options are available:JavaKotlin
| | Use `fromToolObject()` when the tool class is an interface, a third-party class, or any class where adding `@UnfoldingTools` is impractical.
Use `fromInstance()` when you control the class and can add the `@UnfoldingTools` annotation. |

##### Nested UnfoldingTools
UnfoldingTools can be nested for multi-level progressive disclosure.
This enables organizing large tool collections into logical hierarchies where the LLM navigates by invoking facade tools.
###### Programmatic Nesting
Use `UnfoldingTool.of()` to create nested hierarchies programmatically:JavaKotlin
###### Annotation-Based Nesting with Inner Classes
You can also create nested hierarchies using `@UnfoldingTools` annotations on inner classes.
When `UnfoldingTool.fromInstance()` is called, it automatically discovers and includes any nested inner classes that are also annotated with `@UnfoldingTools`:JavaKotlinThis approach provides several benefits:
- **Encapsulation**: All related tools are organized in a single class hierarchy
- **Automatic discovery**: No manual wiring - inner classes with `@UnfoldingTools` are automatically included
- **Arbitrary depth**: Nest as many levels as needed to organize your tools logically
- **Mixed content**: Each level can have both direct `@LlmTool` methods and nested `@UnfoldingTools` classes
##### Dynamically Configured Inner Tools
A powerful pattern with `UnfoldingTool.selectable()` is creating inner tools that are **configured** based on the parameters passed when invoking the facade.
The selector function can create new tool instances with captured state, connection strings, or other configuration:JavaKotlinThis pattern is useful for:
- **Multi-tenant systems**: Configure tools with tenant-specific credentials or endpoints
- **Environment selection**: Let the LLM choose between dev/staging/prod environments
- **Stateful operations**: Create tools that share state (like a shopping cart’s item list)
- **Dynamic service discovery**: Configure tools based on runtime service locations
###### Example: Stateful Shopping Cart Tools
JavaKotlin
##### Comparison with Other Approaches
Other agent frameworks address large tool collections with different approaches, each with trade-offs:
- **Anthropic’s Tool Search Tool**: Uses a `defer_loading: true` flag to prevent tools from being loaded upfront.
Tools are discovered via a separate "Tool Search Tool" that searches tool metadata.
This requires maintaining searchable tool descriptions and adds latency for each discovery step.
- **LangGraph Dynamic Tool Calling**: Uses vector stores and semantic search to select relevant tools based on the user’s query.
This requires embedding infrastructure, vector database setup, and careful tuning of similarity thresholds.
- **Google ADK AgentTool**: Uses sub-agents that recursively delegate to other agents, each potentially having their own tool sets.
Tool discovery is implicit through the agent hierarchy.
- **LangChain4j ToolProvider**: Provides a `ToolProvider` interface for dynamic tool selection, but it works *before* the LLM call by analyzing the incoming user message.
For example, "if the message contains 'booking', include booking tools." This is pre-filtering based on message content, not progressive disclosure through tool invocation.
LangChain4j’s documentation also suggests embedding-based classification, RAG over tool descriptions, or two-pass LLM selection—all requiring additional infrastructure or extra LLM calls.UnfoldingTool takes a fundamentally different approach: **invoke to reveal**.
Instead of searching through tool metadata, the LLM simply invokes a facade tool to unlock the tools it contains.**Beyond Search: Dynamic Tool Configuration**Crucially, UnfoldingTool goes far beyond what any search-based approach can offer.
Search can only **find** pre-existing tools—it cannot create new ones or modify their behavior.
With `UnfoldingTool.selectable()`, the selector function can:
- **Create entirely new tool instances** with different implementations based on runtime parameters
- **Capture configuration** (connection strings, credentials, endpoints) into the tool’s behavior
- **Share mutable state** between the tools created in a single invocation
- **Customize tool descriptions** to reflect the specific context of useFor example, when an LLM invokes a "database" UnfoldingTool with `{"connection": "prod-db.example.com"}`, the returned tools don’t just have different descriptions—they have **different behavior** that operates on that specific database.
This is fundamentally impossible with search-based discovery, which can only return references to pre-defined tools.This provides several advantages:
| Aspect | Other Approaches | UnfoldingTool || **Infrastructure** | Requires vector stores, embeddings, search indices, or pre-filtering logic | No additional infrastructure required || **Selection Timing** | Before LLM call (pre-filtering based on message analysis) | After LLM decides to invoke a facade (LLM-driven discovery) || **Latency** | Search/embedding adds latency; two-pass selection doubles LLM calls | Instant unlock on invocation || **Scalability** | Search quality degrades with very large tool sets; requires careful tuning | Scales to any number of tools via nesting without degradation || **Determinism** | Search results can vary based on embedding similarity | Deterministic: invoking a facade always reveals the same tools || **Cost** | Embedding generation, vector search, or extra LLM calls incur compute costs | No additional compute beyond the tool call itself || **Dynamic Behavior** | Can only return references to pre-existing tools | Can create new tool instances with runtime-configured behavior |
The hierarchical nesting capability of UnfoldingTool means you can organize thousands of tools into a logical tree structure.
The LLM navigates this tree by making simple invocations, with no search overhead at any level.
For example, a top-level "admin_operations" facade might reveal 5 category facades, each revealing 20 specific tools—giving access to 100 tools with at most 2 invocations.
| | UnfoldingTool vs LlmReferenceBoth `UnfoldingTool` and `LlmReference` expose tools to the LLM, but they serve different purposes:**Use UnfoldingTool when:**
- You have a single top-level capability that the LLM can invoke as one tool
- The prompt contribution is short and can fit in the tool description
- Example: A "database" tool that reveals query/insert/delete tools on invocation**Use LlmReference when:**
- The prompt contribution is long or of general significance (appears in system prompt)
- You have a bunch of related tools, not just one top-level tool
- You need `notes()` for detailed usage instructions separate from the tool descriptions
- The reference contributes context beyond just tool availability**Implementing both:**Classes like `Memory` and `ToolishRag` implement both `Tool` and `LlmReference`, giving maximum flexibility: |
JavaKotlinWhen used as an `LlmReference`, the `tools()` method exposes the inner tools directly.
When used as a `Tool`, the implementation wraps them in an `UnfoldingTool` facade.
#### 4.9.9. Process Introspection Tools
Embabel provides built-in `UnfoldingTool` implementations for introspecting the current agent process and its blackboard.
These tools enable agentic workflows where the LLM can monitor its own progress, check resource usage, and access data from previous steps.
##### AgentProcessTools: Runtime Awareness
`AgentProcessTools` provides tools for the LLM to understand its current execution context.
This is useful when you want an agent to be aware of its own operational status - for example, to check how much budget remains before undertaking an expensive operation, or to review what actions have been taken so far.When to use `AgentProcessTools`:
- **Budget-aware agents**: Check remaining cost or token budget before expensive operations
- **Long-running workflows**: Monitor elapsed time and action history
- **Debugging and logging**: Understand what models and tools have been used
- **Self-reflection**: Agents that need to reason about their own behavior**Sub-tools exposed:**
| Tool Name | Purpose || `process_status` | Current process ID, status, running time, and goal information || `process_budget` | Budget limits (cost, tokens, actions) and remaining capacity || `process_cost` | Total cost (LLM and embedding invocations), invocation counts, and detailed token usage || `process_history` | List of actions taken so far with execution times || `process_tools_stats` | Tool usage statistics (call counts per tool) || `process_models` | All models (LLM and embedding) that have been invoked |
JavaKotlin
| | These tools require an active `AgentProcess` context.
If called outside of an agent execution, they return an error message indicating no process is available. |

##### BlackboardTools: Accessing Workflow Data
`BlackboardTools` provides tools for the LLM to access objects in the current process’s blackboard.
The blackboard is Embabel’s shared context mechanism - it holds artifacts from previous actions, tool outputs (when using `ArtifactSink`), and any other objects bound to the process.When to use `BlackboardTools`:
- **Multi-step workflows**: Access results from earlier actions without re-execution
- **Tool output access**: When tools use `ArtifactSink` to publish structured data, BlackboardTools lets the LLM retrieve it
- **Context awareness**: Let the LLM explore what data is available in the current context
- **Debugging**: Inspect blackboard contents during development**Sub-tools exposed:**
| Tool Name | Purpose || `blackboard_list` | List all objects in the blackboard with their types and indices || `blackboard_get` | Get an object by its binding name (e.g., "user", "searchResults") || `blackboard_last` | Get the most recent object of a given type (matches simple name or FQN) || `blackboard_describe` | Get a detailed description/formatting of an object by binding name || `blackboard_count` | Count the number of objects of a given type in the blackboard |
JavaKotlin**Formatting blackboard entries:**By default, `BlackboardTools` uses `DefaultBlackboardEntryFormatter` which:
- Uses `infoString()` for objects implementing `HasInfoString`
- Uses `content` property for objects implementing `HasContent`
- Falls back to `toString()` for other objectsYou can provide a custom `BlackboardEntryFormatter` to control how objects are presented to the LLM.**Type matching:**The `blackboard_last` and `blackboard_count` tools match types by:
- **Simple name**: `"Person"` matches any class named `Person`
- **Fully qualified name**: `"com.example.Person"` matches that specific classThis flexibility lets the LLM query by whatever name is most convenient.
##### Combining Process Introspection Tools
For agents that need full situational awareness, combine both tools:JavaKotlin
#### 4.9.10. Process Communication Tools
Embabel provides two built-in tools that allow the LLM to communicate with the user during agent execution.
Both route messages through the current `AgentProcess` output channel, but differ in their intent and presentation.
| Tool | Purpose | Presentation || `progress` | Report transient status updates during long-running work | Shown as a progress banner (ephemeral) || `communicate` | Send a permanent message to the user | Shown as an assistant chat bubble (persistent) |

##### ProgressTool
`ProgressTool` allows the LLM to report what it is currently doing during long-running actions.
Progress messages are transient—they indicate activity but are not part of the final conversation output.JavaKotlinWhen the LLM calls the `progress` tool, it sends a `ProgressOutputChannelEvent` to the output channel with a short status message.
If no `AgentProcess` is active on the current thread, the tool logs a warning and returns gracefully—agent execution is not interrupted.
##### CommunicateTool
`CommunicateTool` allows the LLM to send a permanent message to the user.
Unlike progress updates, communicate messages appear as assistant chat bubbles and remain part of the conversation.
Use this for reporting results, sharing links (e.g., PR URLs), or informing the user of important outcomes.JavaKotlinWhen the LLM calls the `communicate` tool, it sends a `MessageOutputChannelEvent` containing an `AssistantMessage` to the output channel.
Like `ProgressTool`, it handles the absence of an active `AgentProcess` gracefully.
##### Combining Communication Tools
For agents that need both transient progress reporting and persistent messaging, provide both tools:JavaKotlin
| | Both tools require an active `AgentProcess` context to deliver messages to the user.
If called outside of an agent execution, they return a soft acknowledgment and log a warning—they do not throw exceptions or interrupt agent execution. |

| | The `OutputChannelHighlightingEventListener` automatically suppresses raw tool-call progress banners for `progress` and `communicate` tools, since these tools produce their own user-visible output. |

#### 4.9.11. Just-in-Time Tool Group Initialization
By default, Embabel initializes MCP tool groups at application startup.
This breaks deployments where MCP servers authenticate requests using the
caller’s OAuth token forwarded via the `Authorization` header, because no
user token exists at startup time.To defer the MCP handshake until the first agent request, set these three
properties together:With lazy init enabled, the startup log confirms no MCP traffic occurred at startup:The MCP handshake fires only when the first agent action that requires
an MCP-backed tool group executes — at which point the user’s OAuth token
is already present in the security context.
#### 4.9.12. McpToolFactory: MCP Tool Integration
`McpToolFactory` is an interface that provides a convenient way to integrate Model Context Protocol (MCP) tools into your application.
It creates Embabel `Tool` instances from MCP servers, with support for filtering tools and wrapping them in `UnfoldingTool` facades.`SpringAiMcpToolFactory` is the Spring AI-based implementation.
##### Creating McpToolFactory
`SpringAiMcpToolFactory` requires a list of `McpSyncClient` instances, which are typically provided by Spring’s MCP auto-configuration:JavaKotlin
| | MCP clients are configured in `application.yml`.
See MCP Integration for configuration details. |

##### Getting Individual MCP Tools
Use `toolByName` to retrieve a single MCP tool by its exact name:JavaKotlin
##### Creating UnfoldingToolFacades from MCP
`McpToolFactory` can wrap groups of MCP tools in an `UnfoldingTool` facade for progressive disclosure.
This is useful when you have many MCP tools but want to present them as logical categories.**By Exact Tool Names:**JavaKotlin**By Regex Patterns:**JavaKotlin**With Custom Filter:**JavaKotlin
##### Controlling Facade Removal
After invocation, `UnfoldingTool` facades created by `McpToolFactory` are replaced by a guide tool and their inner tools.
The `removeOnInvoke` parameter is deprecated and ignored:JavaKotlin
##### Real-World Example: Chatbot with MCP Tools
Here’s a real-world example from a production chatbot that uses `McpToolFactory` to integrate MCP tools with graceful degradation:JavaKotlinThis pattern:
- **Gracefully degrades** when MCP tools aren’t available (e.g., in test environments)
- **Groups related tools** behind a descriptive facade using `UnfoldingTool`
- **Adds usage hints** with `withNote()` to guide the LLM on when to use external tools
- **Checks for empty results** before adding tools to avoid empty facades
##### McpToolFactory Method Summary

| Method | Description || `toolByName(String)` | Get a single MCP tool by exact name. Returns `null` if not found. || `requiredToolByName(String)` | Get a single MCP tool by exact name. Throws `IllegalArgumentException` if not found, with a helpful error message listing available tools. || `unfoldingByName(name, description, toolNames)` | Create an `UnfoldingTool` containing tools with exact matching names. || `unfoldingMatching(name, description, patterns)` | Create an `UnfoldingTool` containing tools matching any of the regex patterns. || `unfolding(name, description, filter)` | Create an `UnfoldingTool` with a custom filter predicate. |