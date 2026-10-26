# LLM Integration — Per-Call Configuration

Embabel supports any LLM backed by Spring AI. The central abstraction for selecting and configuring models is `LlmOptions`, which is passed through the `PromptRunner` fluent API or injected via the `Ai` interface.

## LlmOptions — Per-Call Configuration

`LlmOptions` is a fluent builder that specifies which model to use and its hyperparameters. It is deserializable, so you can set it in `application.yml`.

### Key Methods

| Method | Description |
|---|---|
| `withModel(String)` | Direct model name (e.g. `"gpt-4o"`) |
| `withRole(String)` | Role name mapped in config via `embabel.models.llms.<role>` |
| `withTemperature(Double)` | Creativity / randomness (`0.0`–`1.0`) |
| `withTopP(Double)` | Nucleus-sampling parameter |
| `withTopK(Integer)` | Top-K sampling parameter |
| `withPersona(String)` | Inject a system-message persona |

### Basic Usage

**Java**

```java
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.model.OpenAiModels;

LlmOptions creative = LlmOptions
    .withModel(OpenAiModels.GPT_4O_MINI)
    .withTemperature(0.8);

LlmOptions analytical = LlmOptions
    .withModel(OpenAiModels.GPT_4O_MINI)
    .withTemperature(0.2)
    .withTopP(0.9);
```

**Kotlin**

```kotlin
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.OpenAiModels

val creative = LlmOptions
    .withModel(OpenAiModels.GPT_4O_MINI)
    .withTemperature(0.8)
```

### Factory Methods

| Factory | Purpose |
|---|---|
| `LlmOptions.withDefaultLlm()` | Resolve the configured `default-llm` |
| `LlmOptions.withModel(String)` | Hard-code a model name |
| `LlmOptions.withRole(String)` | Resolve via `embabel.models.llms.<role>` |
| `LlmOptions.fromCriteria(ModelSelectionCriteria)` | Pick model by capability criteria |

---

## Mixing Models

Break agentic flows into discrete action steps — each step can target a different model.

```java
@Action
public Story createStory(UserInput input, OperationContext context) {
    var writer = context.ai()
        .withLlm(LlmOptions.withModel(OpenAiModels.GPT_4O));

    Story draft = writer.createObject(
        "Write a story about: " + input.getContent(), Story.class);

    var reviewer = context.ai()
        .withLlm(LlmOptions.withModel(OpenAiModels.GPT_4O_MINI)
            .withTemperature(0.2));

    return reviewer.generateText("Critique this story:\n" + draft);
}
```

```kotlin
@Action
fun createStory(input: UserInput, context: OperationContext): String {
    val writer = context.ai()
        .withLlm(LlmOptions.withModel(OpenAiModels.GPT_4O))

    val draft = writer.createObject(
        "Write a story about: ${input.content}", Story::class.java)

    val reviewer = context.ai()
        .withLlm(LlmOptions.withModel(OpenAiModels.GPT_4O_MINI)
            .withTemperature(0.2))

    return reviewer.generateText("Critique this story:\n$draft")
}
```

---

## Role-Based Model Selection

Map role names to models in config, then reference them with `#` prefix or `withRole()`.

**application.yml**

```yaml
embabel:
  models:
    default-llm: gpt-4o-mini
    llms:
      cheapest: gpt-4o-mini
      best: gpt-4o
      reasoning: o1-preview
```

**Java**

```java
@LlmCall(llm = "#best")
String myAction();

ai.withLlmByRole("best")
    .create("Analyze this data", Report.class);
```

**Kotlin**

```kotlin
@LlmCall(llm = "#best")
fun myAction(): String

ai.withLlmByRole("best")
    .create("Analyze this data", Report::class.java)
```

---

## Roles Across Providers (v1.5.1)

The flat `llms` map above binds each role to one model of one provider. For BYOK applications or provider failover, `embabel.models.roles` gives each role a provider dimension — see `references/configuration.md` for the YAML shape and startup validation rules. Call sites are unchanged: `ai.withLlmByRole("cheapest")` resolves against whichever provider is active for the call, and an unsatisfiable role throws `NoSuitableModelException` at the point of use (never a silent fallback to `default-llm`).

## The Deployment Default as a Role (v1.5.3)

`default-llm` may name a role rather than a model:

```yaml
embabel:
  models:
    default-llm: cheapest       # <1>
    roles:
      cheapest:
        openai:
          model: gpt-4.1-nano
        anthropic:
          model: claude-haiku-4-5
```
<1> A role from `roles` or from the flat `llms` map, not a model name

A model name is resolved once, against the services registered at startup. A role is resolved per call, through the same resolver chain as any other role — so the default follows whichever provider key is active, and a deployment whose first key arrives after startup gets a working default without a restart. That is the case this exists for: under BYOK nothing is registered before the first key, so a default fixed at construction can only be the placeholder until the process restarts.

A model name that nothing registers falls back to the role chain as well, so `default-llm` keeps working whichever way it was meant. Nothing existing changes: a name that does resolve against a registered service is still resolved once and never sent through the resolvers.

A default that nothing can satisfy does not throw the way an unsatisfied role does. It falls back to whatever `default-llm` resolved to at startup — the placeholder, in a deployment awaiting a key — so the caller sees the same actionable "no LLM configured" error. Startup is stricter: with no placeholder to fall back to, a `default-llm` that names nothing this deployment has registered — whether a typo or a role whose every model is missing — is still fatal, and the message says which of the two it is.

## Embedding Roles Under Bring-Your-Own-Key (v1.5.3)

`default-embedding-model` may name a **role** rather than a model. A model name is resolved once, against the services registered at startup; a role is resolved on every call, through the same resolver chain roles use for chat. That difference matters only for a deployment whose provider key arrives after boot.

```yaml
embabel:
  models:
    default-embedding-model: documents   # <1>
    embedding-services:
      documents: text-embedding-3-small  # <2>
```
<1> A role, because `documents` is a key in `embedding-services`. Resolved per call.
<2> What the role means for a single-provider deployment.

Where the provider is not known until a key arrives, `embedding-roles` gives a role a model per provider — the embedding counterpart of `embabel.models.roles`:

```yaml
embabel:
  models:
    default-embedding-model: documents
    embedding-roles:
      documents:
        openai: text-embedding-3-small
        mistral: mistral-embed
```

It is flatter than `roles`, which maps to a whole `LlmOptions`. An embedding model takes text and returns a vector: there is no temperature to set, and the one parameter that does vary — the width — is the model's to state and the index's to follow.

Entries naming a model this deployment has not registered are reported at startup and tolerated, because that is what the shape is for: the model is built from a key supplied at runtime. Which column is read comes from the active key. Where there is no active key, it comes from the registered embedding services — but only where they agree on one provider. A deployment with two registered providers and no key reads `embedding-services` instead of picking a column, because the alternative would be a choice made by bean registration order.

### Resolving Roles in Code

Register a `RoleResolver` bean to decide what a role means per user rather than per deployment. Resolvers are consulted in `Ordered` order, ahead of the configuration, and returning `null` delegates to the next one:

```kotlin
@Component
class ByokRoleResolver(
    private val userKeyStore: UserKeyStore,
) : RoleResolver {

    override fun resolve(role: String, context: ModelSelectionContext): RoleResolution? =
        context.userId
            ?.let { userKeyStore.activeKeyFor(it) }
            ?.let { RoleResolution.Credential(ProviderCredential(it.provider, it.apiKey)) }
}
```

| Resolution Type | Behavior |
|---|---|
| `RoleResolution.Credential` | Hands back a key; the platform looks up the role under that provider, builds the service, and caches it per (provider, key, model) |
| `RoleResolution.Options` | Names a model and its tuning directly |
| `RoleResolution.Service` | Supplies an `LlmService` you built yourself |

`embabel-agent-starter-byok` already knows where to send a key for every provider BYOK supports — Anthropic, OpenAI, DeepSeek, Mistral, Gemini and Atlas Cloud. The resolver above is all the code a deployment using any of them needs.

The `ModelSelectionContext` a resolver receives comes from `ModelSelectionContextHolder`, which the application sets at its request boundary:

```kotlin
ModelSelectionContextHolder.with(ModelSelectionContext(userId = currentUserId)) {
    agentPlatform.runAgent(...)
}
```

The platform carries the context across the threads it starts itself (background agent runs, parallel actions, `OperationContext.parallelMap`), so setting it once at the request boundary is enough. It does not reach threads your own code spawns — capture it there with `ModelSelectionContextHolder.get()` and re-establish it inside the new thread.

### Adding a Provider of Your Own

For a provider the starter does not cover — a gateway, a proxy, a self-hosted endpoint — register a `CredentialEndpointResolver`. It says where a key goes and what the service built from it should call itself; the platform builds the client:

```kotlin
@Bean
fun ourGatewayEndpoint() = CredentialEndpointResolver { credential, _ ->
    if (!credential.provider.equals("OurGateway", ignoreCase = true)) null
    else CredentialEndpoint.OpenAiCompatible(provider = "OurGateway", baseUrl = GATEWAY_URL)
}
```

The case names the wire protocol, which decides the client: `OpenAiCompatible` (OpenAI itself, DeepSeek, Mistral, Gemini, Atlas Cloud and most gateways) or `Anthropic`. Both carry `pricingModel` (defaults to `PricingModel.ALL_YOU_CAN_EAT` — a BYOK call is billed to the user's key, not the deployment) and `knowledgeCutoffDate` (null unless you know it for the model the role named).

Your resolvers are asked first, in `Ordered` order, and the starter's built-in endpoints are the fallback — so answering for a provider the framework handles overrides it (that's how you route OpenAI through your own proxy). Return `null` for a provider you do not handle: a resolver that answers for everything would point someone else's key at your endpoint. If nothing handles the provider, the role fails with `NoSuitableModelException`.

For a wire protocol the framework has no client for, register a `CredentialLlmServiceFactory` instead and build the service yourself. That one names `LlmService` (an SPI type application code should not otherwise depend on), which is why it is the second choice.

### Reading What a Role Is Configured to Mean

`ModelProvider.configuredOptionsForRole(role)` answers "what does this deployment declare this role to be" — against the deployment's own provider, ignoring user keys and application resolvers. Use `resolveLlmOptions` when you need to know what a specific call will really do.

---

## The Ai Interface

`Ai` is the top-level entry point for all Embabel AI operations. Inject it like any Spring bean via constructor injection, or access it through `OperationContext.ai()`.

**Java**

```java
@Component
public record MyService(Ai ai) {
    public String tellJoke(String topic) {
        return ai.withDefaultLlm().generateText("Tell me a joke about " + topic);
    }
}
```

**Kotlin**

```kotlin
@Component
class MyService(private val ai: Ai) {
    fun tellJoke(topic: String): String =
        ai.withDefaultLlm().generateText("Tell me a joke about $topic")
}
```

| Method | Returns | Purpose |
|---|---|---|
| `ai.withDefaultLlm()` | `PromptRunner` | Runner using the configured default model |
| `ai.withLlm(LlmOptions)` | `PromptRunner` | Runner with custom model / hyperparameters |
| `ai.withLlm(String)` | `PromptRunner` | Runner with a model name (shortcut) |
| `ai.withLlmByRole(String)` | `PromptRunner` | Runner resolved by role name |

---

## PromptRunner Fluent API

`PromptRunner` is the interface used to send prompts to the LLM. Build it via `Ai` or `OperationContext.promptRunner()`.

| Method | Description |
|---|---|
| `createObject(String, Class<T>)` | Create a typed object; throws on persistent failure (with retry) |
| `createObjectIfPossible(String, Class<T>)` | Create a typed object; returns `null` on failure |
| `generateText(String)` | Simple text response |

### Fluent Chaining

```java
Story story = context.ai()
    .withDefaultLlm()
    .withToolGroup(CoreToolGroups.WEB)
    .withId("create-story")
    .creating(Story.class)
    .withExample("A children's story",
        new Story("Once upon a time..."))
    .fromPrompt("Create a story about: " + input);
```

```kotlin
val story = context.ai()
    .withDefaultLlm()
    .withToolGroup(CoreToolGroups.WEB)
    .withId("create-story")
    .creating(Story::class.java)
    .withExample("A children's story",
        Story("Once upon a time..."))
    .fromPrompt("Create a story about: ${input.content}")
```

| Method | Purpose |
|---|---|
| `creating(Class<T>)` | Enter the `Creating` sub-API for typed output |
| `withExample(String prompt, T example)` | Add a few-shot example (rendered as JSON) |
| `fromPrompt(String)` | Execute the call with the given prompt |

| Method | Purpose |
|---|---|
| `withImage(AgentImage)` | Attach an image for vision-capable models |
| `withDocument(AgentDocument)` | Attach a document (PDF, Office, etc.) |
| `withMessage(Message)` | Attach a raw Embabel `Message` |
| `withTool(Subagent.ofClass(...))` | Enable handoff to another agent |
| `withToolObject(Object)` | Expose `@Tool` methods on a domain object |
| `rendering(String)` | Use Jinja templates for the prompt |
| `withToolNotFoundPolicy(ToolNotFoundPolicy)` | Control tool-name recovery strategy |

### Full Chain Example

```java
String answer = context.ai()
    .withLlm(LlmOptions.fromCriteria(
            ModelSelectionCriteria.getAuto())
            .withThinking(Thinking.withTokenBudget(2048)))
    .withToolGroup(CoreToolGroups.WEB)
    .withId("research-topic")
    .createObject("Research and summarize: " + topic, Answer.class);
```

```kotlin
val answer = context.ai()
    .withLlm(LlmOptions.fromCriteria(
        ModelSelectionCriteria.getAuto())
        .withThinking(Thinking.withTokenBudget(2048)))
    .withToolGroup(CoreToolGroups.WEB)
    .withId("research-topic")
    .createObject("Research and summarize: $topic", Answer::class.java)
```

---

## Model Constants

| Class | Example Constants |
|---|---|
| `OpenAiModels` | `GPT_4O`, `GPT_4O_MINI`, `GPT_41`, `GPT_41_MINI`, `GPT_41_NANO`, `PROVIDER` |
| `AnthropicModels` | `CLAUDE_SONNET_4_5`, `CLAUDE_35_HAIKU`, `CLAUDE_35_SONNET`, `PROVIDER` |

```java
ai.withLlm(OpenAiModels.GPT_4O)
    .createObject("Analyze this", Report.class);

ai.withLlm(AnthropicModels.CLAUDE_35_HAIKU)
    .withImage(image)
    .generateText("Describe this image");
```

---

## Model Selection Criteria

Pick models by capability rather than name:

```java
LlmOptions.fromCriteria(ModelSelectionCriteria.getAuto());
LlmOptions.fromCriteria(ModelSelectionCriteria.getCheapest());
```

```kotlin
LlmOptions.fromCriteria(ModelSelectionCriteria.getAuto())
LlmOptions.fromCriteria(ModelSelectionCriteria.getCheapest())
```

---

## Quick Reference: Choosing an LLM

| Consideration | Guidance |
|---|---|
| **Complex return type** | Deeply nested structures need a stronger model |
| **Task nature** | Review docs — different models have different strengths |
| **Tool calling complexity** | Simple calls work on small models; complex orchestration needs a strong LLM |
| **Cost** | Try the cheapest model that works; switch if it fails |
| **Privacy** | Local models via Ollama or Docker are an option |

---

## Anthropic Prompt Caching

Anthropic exposes public APIs for explicit prompt caching control, providing significant cost and latency savings for multi-turn conversations.

### Caching Strategies

Embabel provides `AnthropicCachingConfig` with granular control over what gets cached:

| Strategy | What It Caches | Best For |
|----------|---------------|----------|
| `systemPrompt` | System prompt | Multi-turn conversations with fixed system prompt |
| `tools` | Tool definitions | Applications with large tool schemas |
| `conversationHistory` | Message history | Long conversations with many turns |
| `messageType` | Specific message content | Large documents or data blocks |

### Usage

**Java**

```java
import static com.embabel.agent.core.llm.AnthropicCachingConfigKt.withAnthropicCaching;

AnthropicCachingConfig cachingConfig = new AnthropicCachingConfig();
cachingConfig.setSystemPrompt(true);
cachingConfig.setTools(true);

LlmOptions options = LlmOptions.withModel(AnthropicModels.CLAUDE_SONNET_4_5);
options = withAnthropicCaching(options, cachingConfig);

var story = context.ai()
    .withLlm(options)
    .creating(Story.class)
    .fromPrompt("Write a story about: " + input);
```

**Kotlin**

```kotlin
val cachingConfig = AnthropicCachingConfig()
cachingConfig.systemPrompt = true
cachingConfig.tools = true

val options = LlmOptions.withModel(AnthropicModels.CLAUDE_SONNET_4_5)
    .withAnthropicCaching(
        AnthropicCachingConfig(
            systemPrompt = true,
            tools = true
        )
    )
```

### Advanced: Message-Type Caching

Cache specific message content (e.g., large reference documents):

```kotlin
val cachingConfig = AnthropicCachingConfig(
    messageType = listOf(0)  // Cache message at index 0
)
```

### Best Practices

- **System prompt caching** gives the biggest ROI — it's cached across all turns
- **Tool caching** helps when tool schemas are large (> 50 tools)
- **Conversation history** caching is useful for long conversations but can be expensive
- **Cache metrics** are available in the LLM response — monitor hit rates
- **TTL** can be configured per-cache entry for time-sensitive content

> **Tip:** Start with `systemPrompt = true` and `tools = true` for the best cost/latency ratio. Add conversation history caching as needed.

---

## Native Structured Output

Native structured output enables the model provider to enforce a JSON Schema directly, rather than relying only on prompt instructions and Embabel's object construction.

### How It Works

1. Embabel generates a JSON Schema from the target class
2. The provider (e.g., OpenAI) validates the output against the schema natively
3. Output is guaranteed to match the accepted schema

### Usage

```java
var options = withNativeStructuredOutput(
    LlmOptions.fromModel(OpenAiModels.GPT_4O),
    NativeStructuredOutputMode.ENABLED
);
```

```kotlin
val options = LlmOptions.fromModel(OpenAiModels.GPT_4O)
    .withNativeStructuredOutput(NativeStructuredOutputMode.ENABLED)
```

### Modes

| Mode | Behavior |
|------|----------|
| `NativeStructuredOutputMode.ENABLED` | Try the native path when model capability and schema compatibility allow it |
| `NativeStructuredOutputMode.DISABLED` | Force Embabel's normal object construction path |
| `NativeStructuredOutputMode.DEFAULT` | Let Embabel decide from model capability, API shape, and schema compatibility |

### Limitations

- **Required fields**: OpenAI native structured output requires `required` to include every property. Optional Java reference fields are not treated as required unless annotated (e.g., `@NotNull` or `@JsonProperty(required = true)`)
- **Arrays of objects**: May not be supported in all providers
- **Streaming**: Native structured output is not supported for streaming by Spring AI currently
- **Provider coverage**: OpenAI-compatible `response_format` is the primary supported path. Anthropic native structured output is currently disabled by default until its semantics are verified

---

## Smaller and Local Model Tuning

Smaller chat models behave differently from frontier models. Embabel compensates for common issues:

### Empty Response Handling

Weaker open-weights models (e.g., `gpt-oss-20b`, some Qwen variants) sometimes return blank text with no further tool calls. Activate empty-response retries:

```yaml
embabel:
  agent:
    platform:
      toolloop:
        empty-response:
          max-retries: 1  # Feed a synthetic nudge back to the model
          nudge-message: "Please continue with your response."
```

### Tool-Name Confusion

Smaller models more frequently call tools by approximate names. The default `AutoCorrectionPolicy` handles this by feeding back a "did you mean X?" suggestion. Tune retries if needed:

```yaml
embabel:
  agent:
    platform:
      toolloop:
        tool-not-found:
          max-retries: 3  # Default: 3
```

### ToolNotFoundPolicy

Control tool-name recovery strategy:

| Policy | Behavior |
|--------|----------|
| `AutoCorrectionPolicy` (default) | Feeds back "did you mean X?" suggestion to the model |
| `ImmediateThrowPolicy` | Throws immediately on unknown tool name — no retry |

Set programmatically:

```kotlin
ai.withDefaultLlm()
    .withToolNotFoundPolicy(ImmediateThrowPolicy())
    .createObject("...", Result::class.java)
```

### Iteration Headroom

Recovery costs LLM calls. If you enable retry policies, raise `max-iterations` so a turn that needs an extra round trip doesn't run out of budget:

```yaml
embabel:
  agent:
    platform:
      toolloop:
        max-iterations: 30  # Default: 20
```

These settings are off-by-default so existing deployments using strong models behave exactly as before. Turn them on per-deployment when the model you've picked benefits from them.

---

## Custom Embedding Service

Implement a custom embedding service independent of Spring AI for providers not supported by Spring AI, custom pre/post-processing, or proprietary embedding APIs.

### The EmbeddingService Interface

```java
public interface EmbeddingService {
    float[] embed(String text);
    List<float[]> embed(List<String> texts);
    int getDimensions();
    String getName();
    String getProvider();
}
```

```kotlin
interface EmbeddingService {
    fun embed(text: String): FloatArray
    fun embed(texts: List<String>): List<FloatArray>
    val dimensions: Int
}
```

Register as a Spring bean and it will be automatically discovered by the RAG subsystem.

---

*Source: Embabel Agent v1.5.3 documentation — `references/llms` and `references/types`*
