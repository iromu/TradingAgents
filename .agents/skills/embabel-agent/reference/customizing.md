# Customizing Reference

Custom LLM integration, options converters, knowledge cutoff, and embedding models. See SKILL.md for the core workflow.

## Custom LLM Integration

Implement the `LlmService` interface or use `SpringAiLlmService` to wrap any Spring AI `ChatModel`.

### SpringAiLlmService

`SpringAiLlmService` implements `LlmService` and provides framework-agnostic LLM operations, including tool-loop and message-sender support.

```java
@Configuration
public class LlmsConfig {
    @Bean
    public LlmService<?> myLlm() {
        org.springframework.ai.chat.model.ChatModel chatModel = ...;
        return new SpringAiLlmService(
                "myChatModel",              // name — used for model selection
                "myChatModelProvider",      // provider — e.g. "OpenAI", "Anthropic"
                chatModel
            )
            .withOptionsConverter(new MyLlmOptionsConverter())  // convert LlmOptions → ChatOptions
            .withKnowledgeCutoffDate(LocalDate.of(2025, 4, 1)); // temporal awareness
    }
}
```

### Configuration Options

| Option | Required | Description |
|---|---|---|
| `name` | Yes | Model name used for selection |
| `provider` | Yes | Provider name (e.g. "OpenAI", "Anthropic", "Mistral") |
| `OptionsConverter` | No | Converts Embabel `LlmOptions` to provider-specific `ChatOptions` |
| `knowledge cutoff date` | No | Adds `KnowledgeCutoffDate` prompt contributor |
| `pricing model` | No | Enables cost tracking |
| `PromptContributor` | No | Additional contributors applied to every LLM call |

### OpenAI-Compatible LLMs

Extend `OpenAiCompatibleModelFactory` to register OpenAI-compatible providers as beans:

```java
@Configuration
public class CustomOpenAiCompatibleModels extends OpenAiCompatibleModelFactory {

    public CustomOpenAiCompatibleModels(
            @Value("${MY_BASE_URL:#{null}}") String baseUrl,
            @Value("${MY_API_KEY}") String apiKey,
            ObservationRegistry observationRegistry) {
        super(baseUrl, apiKey, observationRegistry);
    }

    @Bean
    public LlmService<?> myGreatModel() {
        return openAiCompatibleLlm(
            "my-great-model",
            "me",
            LocalDate.of(2025, 1, 1),
            new PerTokenPricingModel(0.40, 1.6)
        );
    }
}
```

## Options Converters

Use an `OptionsConverter` to translate Embabel's `LlmOptions` into provider-specific options (e.g., Spring AI's `ChatOptions`). Chain it via `.withOptionsConverter()`:

```java
public class MyLlmOptionsConverter implements OptionsConverter {
    @Override
    public ChatOptions convert(LlmOptions options) {
        var builder = ChatOptions.builder()
            .model(options.getModel())
            .temperature(options.getTemperature());
        // Add provider-specific options here
        return builder.build();
    }
}
```

## Knowledge Cutoff

Set a knowledge cutoff date so the LLM knows its training data boundary:

```java
return new SpringAiLlmService("model", "provider", chatModel)
    .withKnowledgeCutoffDate(LocalDate.of(2025, 4, 1));
```

When a cutoff date is provided, Embabel automatically adds the `KnowledgeCutoffDate` prompt contributor to every call.

## Embedding Models

Embedding models are registered as beans of type `EmbeddingService`, wrapped via `SpringAiEmbeddingService`:

```java
@Configuration
public class EmbeddingModelsConfig {
    @Bean
    public EmbeddingService myEmbeddingModel() {
        org.springframework.ai.embedding.EmbeddingModel embeddingModel = ...;
        return new SpringAiEmbeddingService(
                "myEmbeddingModel",
                "myEmbeddingModelProvider",
                embeddingModel);
    }
}
```

## Key Points

- `SpringAiLlmService` wraps any Spring AI `ChatModel`
- Use `OptionsConverter` to map `LlmOptions` → provider-specific options
- Add a knowledge cutoff date for temporal awareness
- Extend `OpenAiCompatibleModelFactory` for OpenAI-compatible providers
- Embedding models use `SpringAiEmbeddingService` with the same pattern

*Source: Embabel Agent v1.0.0 documentation — `reference/customizing`*
