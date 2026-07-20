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
| `withThinking(Thinking)` | Enable native reasoning mode (e.g. GLM) |
| `withStreaming(Boolean)` | Enable streaming responses |
| `withToolNotFoundPolicy(ToolNotFoundPolicy)` | Control tool-name recovery strategy |

### Full Chain Example

```java
String answer = context.ai()
    .withLlm(LlmOptions.fromCriteria(
            ModelSelectionCriteria.getAuto()))
    .withToolGroup(CoreToolGroups.WEB)
    .withId("research-topic")
    .withThinking(Thinking.withTokenBudget(2048))
    .createObject("Research and summarize: " + topic, Answer.class);
```

```kotlin
val answer = context.ai()
    .withLlm(LlmOptions.fromCriteria(
        ModelSelectionCriteria.getAuto()))
    .withToolGroup(CoreToolGroups.WEB)
    .withId("research-topic")
    .withThinking(Thinking.withTokenBudget(2048))
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
var options = LlmOptions.withModel(OpenAiModels.GPT_4O)
    .withNativeStructuredOutput(NativeStructuredOutputMode.ENABLED);
```

```kotlin
val options = LlmOptions.withModel(OpenAiModels.GPT_4O)
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

*Source: Embabel Agent v1.0.0 documentation — `reference/llms` and `reference/types`*
