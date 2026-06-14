Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.23. Working with LLMs
Embabel supports any LLM supported by Spring AI.
In practice, this is just about any LLM.
#### 4.23.1. Choosing an LLM
Embabel encourages you to think about LLM choice for every LLM invocation.
The `PromptRunner` interface makes this easy.
Because Embabel enables you to break agentic flows up into multiple action steps, each step can use a smaller, focused prompt with fewer tools.
This means it may be able to use a smaller LLM.Considerations:
- **Consider the complexity of the return type you expect** from the LLM.
This is typically a good proxy for determining required LLM quality.
A small LLM is likely to struggle with a deeply nested return structure.
- **Consider the nature of the task.** LLMs have different strengths; review any available documentation.
You don’t necessarily need a huge, expensive model that is good at nearly everything, at the cost of your wallet and the environment.
- **Consider the sophistication of tool calling required**.
Simple tool calls are fine, but complex orchestration is another indicator you’ll need a strong LLM.
(It may also be an indication that you should create a more sophisticated flow using Embabel GOAP.)
- **Consider trying a local LLM** running under Ollama or Docker.
| | Trial and error is your friend.
Embabel makes it easy to switch LLMs; try the cheapest thing that could work and switch if it doesn’t. |

#### 4.23.2. Tuning for Smaller and Local Models
A core goal of Embabel is to make agentic flows work well across the full range of LLMs, so you can choose the cheapest, smallest, or most private model that does the job — rather than always reaching for a frontier model.
Smaller chat models behave differently from frontier models in ways that the framework can compensate for:
- **Silent failures after tool calls.** Weaker open-weights models (e.g. `gpt-oss-20b`, some Qwen variants) sometimes return blank text with no further tool calls when they don’t know how to proceed. Without intervention the tool loop exits with empty content. Activate `embabel.agent.platform.toolloop.empty-response.max-retries: 1` to feed a synthetic nudge back to the model and give it one more chance — see Empty-Response Handling.
- **Tool-name confusion.** Smaller models more frequently call tools by approximate names. The default `AutoCorrectionPolicy` handles this by feeding back a "did you mean X?" suggestion; tune `embabel.agent.platform.toolloop.tool-not-found.max-retries` if your model needs more attempts.
- **Iteration headroom.** Recovery costs LLM calls. If you enable retry policies, raise `embabel.agent.platform.toolloop.max-iterations` so a turn that needs an extra round trip doesn’t run out of budget.These settings are off-by-default so existing deployments using strong models behave exactly as before.
Turn them on per-deployment when the model you’ve picked benefits from them.
#### 4.23.3. Advanced: Custom LLM Integration

| | If you want to use a standard provider (Anthropic, OpenAI, DeepSeek, Mistral) with a
user-supplied key at runtime, see Bring Your Own Key (BYOK).
That is the recommended path for BYOK scenarios.
This section covers implementing `LlmMessageSender` from scratch for providers not otherwise
supported by Embabel. |
Embabel’s tool loop is framework-agnostic, allowing you to integrate any LLM provider by implementing the `LlmMessageSender` interface.
This is useful when:
- You want to use an LLM provider not supported by Spring AI
- You need custom request/response handling
- You’re integrating with a proprietary or internal LLM service
##### The LlmMessageSender Interface
The core abstraction is the `LlmMessageSender` functional interface:JavaKotlinThe implementation makes a single LLM inference call and returns the response.
Importantly, it does **not** execute tools—​it only returns any tool call requests from the LLM.
Tool execution is handled by Embabel’s `DefaultToolLoop`.
| | Even more advanced control over tools execution is available - tools can be executed in parallel, controlled by `ParallelToolLoop`. In order to activate `ParallelToolLoop`, please set the following parameter: |
For full list of tool loop configuration parameters please refer to `ToolLoopConfiguration`.
##### Tool-Not-Found Recovery Policy
When the LLM calls a tool by a name that doesn’t exist in the available set, the behavior is controlled by `ToolNotFoundPolicy`.Two built-in policies are provided:
- **`AutoCorrectionPolicy`** (default) — feeds the error back to the LLM so it can self-correct. Uses case-insensitive fuzzy matching to suggest corrections for hallucinated tool names (e.g., `ragbot_vectorSearch` → suggests `vectorSearch`). When multiple candidates match, all are listed. Throws `ToolNotFoundException` after 3 consecutive failures.
- **`ImmediateThrowPolicy`** — throws `ToolNotFoundException` immediately.The system-wide default is `AutoCorrectionPolicy`, provided as a Spring bean with `@ConditionalOnMissingBean`.
To override it globally, define your own `ToolNotFoundPolicy` bean.For per-interaction control, use `withToolNotFoundPolicy()` on `PromptRunner`:JavaKotlinCustom policies can be implemented by implementing the `ToolNotFoundPolicy` interface:
##### Response Types
The `LlmMessageResponse` contains:
- `message`: The LLM’s response as an Embabel `Message`
- `textContent`: Text content from the response
- `usage`: Optional token usage informationFor responses that include tool calls, return an `AssistantMessageWithToolCalls`:JavaKotlin
##### Example: Custom LLM Provider
Here’s an example of implementing `LlmMessageSender` for a hypothetical HTTP-based LLM API:JavaKotlin
##### Creating an LlmService
To make your custom LLM available through Embabel’s `ModelProvider`, implement the `LlmService` interface:JavaKotlinThen register it as a Spring bean:JavaKotlinThe bean will be automatically discovered and made available through the `ModelProvider`.
##### How Model Discovery and Selection Works
When your application starts, `ConfigurableModelProvider` collects all `LlmService` beans from the Spring application context.
Your custom LLM is matched by the `name` property you set on your `LlmService` implementation.**By name**: Use the `name` from your `LlmService` directly.
This works with `@LlmCall`, `ai.withLlm()`, and `AgenticTool.withLlm()`:JavaKotlin**By role**: Map a role name to your model name in configuration, then reference it with the `#` prefix:
| **1** | Sets the default LLM used when no explicit model is specified || **2** | Maps the `best` role to your custom model || **3** | Maps the `cheapest` role to a different model |
Then reference roles with `#`:JavaKotlin
| | If no LLM is specified in `@LlmCall` or `withLlm()`, the `default-llm` from configuration is used. |

##### Using Your Custom Implementation (Alternative)
If you need more control over the LLM operations layer itself, you can extend `ToolLoopLlmOperations`:JavaKotlinThe `ToolLoopLlmOperations` base class provides several extension points:
- `createMessageSender()`: Create the LLM communication layer
- `createOutputConverter()`: Parse LLM responses into typed objects
- `sanitizeStringOutput()`: Clean up raw text responses
- `emitCallEvent()`: Emit observability events
##### Key Implementation Notes

1.**Tool calls are not executed by your sender.** Just return the tool call requests—​Embabel’s tool loop handles execution and continuation.
1.**Handle both tool and non-tool responses.** Return `AssistantMessage` for plain text, `AssistantMessageWithToolCalls` when the LLM wants to invoke tools.
1.**Include usage information when available.** This enables cost tracking and observability.
1.**Message types matter.** The tool loop expects specific message types:
- `UserMessage`: User input
- `SystemMessage`: System prompts
- `AssistantMessage`: LLM text response
- `AssistantMessageWithToolCalls`: LLM response with tool requests
- `ToolResultMessage`: Result returned to LLM after tool execution
#### 4.23.4. Advanced: Custom Embedding Service
Just as you can integrate a custom LLM, you can implement a custom embedding service that doesn’t depend on Spring AI.
This is useful when:
- You want to use an embedding provider not supported by Spring AI
- You need custom pre/post-processing of embeddings
- You’re integrating with a proprietary or internal embedding API
##### The EmbeddingService Interface
The `EmbeddingService` interface is framework-agnostic.
Unlike `SpringAiEmbeddingService`, a custom implementation does not need to wrap a Spring AI `EmbeddingModel`:JavaKotlin
##### Example: Custom Embedding Provider
Here’s an example of implementing `EmbeddingService` for an HTTP-based embedding API:JavaKotlin
##### Registering as a Spring Bean
Register your custom embedding service as a Spring bean and it will be automatically discovered:JavaKotlin
##### Discovery and Selection
Custom embedding services follow the same discovery and selection pattern as LLMs (see How Model Discovery and Selection Works).**By name**: Use `ai.withEmbeddingService()` with the `name` from your implementation:JavaKotlin**By role**: Map a role name to your embedding service in configuration:
| **1** | Sets the default embedding service || **2** | Maps the `cheapest` role to your custom embedding service |

#### 4.23.5. Advanced Caching with Anthropic
While many providers have implicit caching managed internally, Anthropic exposes public APIs for explicit prompt caching control.
This allows you to optimize costs and latency for applications with long prompts, many tools, or extended conversations.
##### Motivation
Anthropic’s prompt caching feature provides significant benefits:
- **Cost savings**: Cache reads cost 90% less than regular input tokens
- **Latency improvements**: Cached content is processed faster
- **Ideal for**: Long system prompts, extensive tool definitions, multi-turn conversationsWithout caching, every API call processes the entire prompt from scratch.
With caching, repeated content (system prompts, tools, conversation history) can be cached and reused across requests.
##### How It Works
Anthropic caches the trailing portion of your prompt context.
The cache is identified by an exact match of the content hashcode.
Any change to the cached portion invalidates the cache.**Key concepts:**
- **Cache creation**: First time content is seen, it’s written to cache with a 25% premium over regular input tokens (for 5-minute TTL)
- **Cache reads**: Subsequent requests with matching content read from cache at 10% of regular input token cost
- **Cache TTL**: 5 minutes (default) or 1 hour (premium, higher creation cost)
- **Minimum size**: 1024 tokens for older models, 4096 tokens for Claude Sonnet 4.5 and newer.
##### Cache Strategies
Embabel provides several caching strategies through `AnthropicCachingConfig`:**System Prompt Caching**Cache the system prompt for reuse across multiple requests:JavaKotlin**Tools Caching**Cache tool definitions when using many tools or tools with large schemas:JavaKotlin**System + Tools Caching**Combine both strategies:JavaKotlin**Conversation History Caching**Cache conversation history for long multi-turn conversations:JavaKotlin
##### Advanced Configuration
**Message Type Minimum Content Length**Control which messages are eligible for caching based on their content length:JavaKotlin**Message Type TTL**Set cache TTL per message type (default is 5 minutes):JavaKotlin
##### Accessing Cache Metrics
Embabel provides extension methods to access Anthropic-specific cache metrics from the `Usage` object:JavaKotlin
##### Best Practices

1.**Cache long, stable content**: System prompts and tool definitions that don’t change frequently are ideal candidates
1.**Mind the minimum size**: Content must meet the minimum token requirement (1024 or 4096 depending on model)
1.**Monitor cache metrics**: Use the cache extension methods to track cache hit rates and validate savings
1.**Consider TTL vs cost**: 1-hour TTL has higher creation cost but better for longer sessions
1.**Test before deploying**: Cache behavior can vary based on prompt structure and usage patterns
##### Reference
For complete details on Anthropic’s prompt caching, see:
docs.anthropic.com/en/docs/build-with-claude/prompt-caching
#### 4.23.6. Advanced Feature: Native Structured Output
Native structured output enables the model provider to enforce a JSON Schema directly, rather than relying only on prompt instructions and Embabel’s object construction.
E**mbabel still owns schema generation and object binding**; the native provider path is an optimization and compatibility feature used only when it is explicitly supported and safe for the call.
##### Payload Format
For OpenAI-compatible providers, Spring AI wraps Embabel’s schema in a `response_format` JSON schema payload.
For example, a `MonthItem` object can produce a request fragment like:OpenAI validates this schema before generation.
If the schema is not valid for the provider-native path, the request fails before the model produces output.
##### Participants Roles

- **Embabel** generates the JSON Schema, decides whether the native path is enabled for a call, and parses the final response into the requested object.
- **Spring AI** wraps Embabel’s schema into provider-specific format options, such as OpenAI’s `response_format`, and serializes the HTTP request.
- **The provider** such as OpenAI validates the native structured-output payload and returns output that matches the accepted schema.
##### Configuration and API
Native structured output is advertised in model metadata:For a specific call, enable it through `LlmOptions`:JavaKotlin
- `NativeStructuredOutputMode.ENABLED`: Try the native path when model capability and schema compatibility allow it.
- `NativeStructuredOutputMode.DISABLED`: Force Embabel’s normal object construction path.
- `NativeStructuredOutputMode.DEFAULT`: Let Embabel decide from model capability, API shape, and schema compatibility.For Java objects, mark required reference fields explicitly:
##### Limitations

- **Required fields**: OpenAI native structured output requires `required` to include every property. Optional Java reference fields are not treated as required unless annotated, for example with `@JsonProperty(required = true)` or `@NotNull`.
- **Optional values**: Provider-native optionality usually needs nullable required properties, for example `type: ["string", "null"]`, rather than omitting the property from `required`.
- **Arrays of objects**: Some providers, including OpenAI according to Spring AI documentation, do not support arrays of objects natively. Embabel falls back to normal object construction for unsupported shapes.
- **Streaming**: Native structured output is not supported for streaming by Spring AI currently
- **`ifPossible` APIs**: `createObjectIfPossible` and related paths use `MaybeReturn` semantics and stay on Embabel’s fallback object construction path.
- **Provider coverage**: OpenAI-compatible `response_format` is the primary supported path. Anthropic native structured output is currently disabled by default until its semantics are verified.