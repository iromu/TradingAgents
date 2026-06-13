# LLM Integration Reference

Full details for LLM integration with Embabel. See SKILL.md for the core workflow.

## LlmOptions — Per-Call Configuration

```java
var options = LlmOptions.withModel("gpt-4o")
    .withTemperature(0.8)
    .withTopP(0.9)
    .withTopK(10)
    .withPersona("You are a creative storyteller");
```

Key methods:
- `withModel(String)` — specific model name
- `withRole(String)` — role defined in config (e.g., `#best`)
- `withTemperature(Double)` — 0.0–1.0
- `withTopP(Double)` — nucleus sampling
- `withTopK(Integer)` — top-K sampling
- `withPersona(String)` — system message persona

`LlmOptions` is serializable — can be set in `application.yml` for externalized configuration.

## Mixing Models

```java
var writer = LlmOptions.withModel(OpenAiModels.GPT_4O_MINI)
    .withTemperature(0.8);

var reviewer = LlmOptions.withModel(OpenAiModels.GPT_4O)
    .withTemperature(0.2);
```

Use different models and temperatures for different actions in the same agent.

## Using the Ai Interface

```java
public record InjectedComponent(Ai ai) {
    public Joke createJoke(String topic1, String topic2, String voice) {
        return ai.withLlm(LlmOptions.withDefaultLlm().withTemperature(.8))
            .withId("tell-joke")
            .creating(Joke.class)
            .fromPrompt("Tell me a joke about %s and %s. Voice: %s".formatted(topic1, topic2, voice));
    }
}
```

## PromptRunner Methods

| Method | Purpose |
|--------|---------|
| `createObject(prompt, Class<T>)` | Create typed object (throws on failure, triggers retry) |
| `createObjectIfPossible(prompt, Class<T>)` | Try to create, return null on failure |
| `generateText(prompt)` | Simple text response |
| `withLlm(LlmOptions)` | Set LLM config |
| `withToolGroup(String)` | Add tool group |
| `withToolObject(obj)` | Add domain object with @Tool methods |
| `withId("id")` | Tag interaction for test verification |
| `withExample(text, obj)` | Few-shot example |
| `withPromptContributor(pc)` | Add prompt content |
| `withReference(LlmReference)` | Add LlmReference with content and tools |
| `withTool(Tool)` | Add a single tool |
| `withTools(Tool...)` | Add multiple tools |
| `withToolChainingFrom(Class)` | Enable tool chaining from returned type |
| `withToolCallContext(Map)` | Set per-interaction tool call context |
| `withGuardRails(GuardRail...)` | Add guardrails for this call |
| `withSkills(Skills)` | Add agent skills |
| `withImage(AgentImage)` | Add image for vision |
| `rendering("template-name")` | Use Jinja template |
| `streaming()` | Enable streaming mode |

## Vision Support

```java
var image = AgentImage.fromFile(new File("diagram.png"));
context.ai().withDefaultLlm()
    .withImage(image)
    .generateText("What is in this image?");
```

## Streaming

### Object Streaming

```java
context.ai().withDefaultLlm()
    .streaming()
    .createObject(Report.class)
    .fromPrompt("Generate report")
    .doOnNext(event -> {
        if (event instanceof Thinking thinking) {
            System.out.println("Thinking: " + thinking.getContent());
        } else if (event instanceof ObjectCreated obj) {
            System.out.println("Created: " + obj.getObject());
        }
    })
    .doOnComplete(() -> {
        System.out.println("Stream complete");
    })
    .subscribe();
```

### Raw Text Streaming

```java
context.ai().withDefaultLlm()
    .streaming()
    .generateText("Write a story")
    .doOnNext(event -> {
        if (event instanceof TextChunk chunk) {
            System.out.print(chunk.getText());
        }
    })
    .doOnComplete(() -> System.out.println())
    .subscribe();
```

## Custom LLM Provider

Implement `LlmMessageSender` for unsupported providers:

```java
public class CustomLlmMessageSender implements LlmMessageSender {
    @Override
    public LlmMessageResponse sendMessage(List<Message> messages) {
        // HTTP call to your provider
        return new LlmMessageResponse(message, textContent, usage);
    }
}
```

Register as a Spring bean with `LlmService` for model discovery:

```java
@Bean
LlmService customLlm() {
    return new LlmService() {
        @Override public String getName() { return "my-custom-llm"; }
        @Override public LlmMessageSender createMessageSender() { return new CustomLlmMessageSender(); }
    };
}
```

## Custom Embedding Service

Implement `EmbeddingService` for custom embeddings:

```java
public class CustomEmbeddingService implements EmbeddingService {
    @Override
    public List<float[]> embed(List<String> texts) {
        // Call your embedding API
        return results;
    }
}
```

Register as a Spring bean for auto-discovery.

## Callbacks / Interceptors

```java
@Bean
ToolLoopInspector toolLoopLoggingInspector() {
    return new ToolLoopLoggingInspector();
}

@Bean
ToolResultTruncatingTransformer truncatingTransformer() {
    return new ToolResultTruncatingTransformer(5000);
}
```

## Anthropic Prompt Caching

```java
var caching = AnthropicCachingConfig.builder()
    .systemPrompt()
    .tools()
    .conversationHistory()
    .build();
```

Cache reads cost 90% less than regular tokens. Minimum size: 1024 tokens (older models) or 4096 tokens (Claude Sonnet 4.5+).

## Key Points

- Use `LlmOptions.withModel()` for per-call model/temperature configuration
- Different actions in the same agent can use different models and temperatures
- `withId()` is essential for test verification and debugging
- `withExample()` enables few-shot prompting
- `withReference()` combines content injection with tool exposure
- Custom LLM providers require implementing `LlmMessageSender`
- Custom embedding services implement `EmbeddingService`
- Callbacks/interceptors can log, transform, or truncate results
- Anthropic prompt caching significantly reduces costs for repeated content
