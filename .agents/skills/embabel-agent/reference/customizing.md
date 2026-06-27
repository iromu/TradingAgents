# Customizing Reference

Custom LLM integration, options converters, and knowledge cutoff. See SKILL.md for the core workflow.

## Adding Custom LLMs

Implement `LlmService` or use `SpringAiLlmService` to wrap Spring AI `ChatModel` instances.

### Using SpringAiLlmService

```java
@Configuration
public class LlmsConfig {
    @Bean
    public LlmService<?> myLlm() {
        org.springframework.ai.chat.model.ChatModel chatModel = ...;
        return new SpringAiLlmService(
                "myChatModel",              // name (used for model selection)
                "myChatModelProvider",      // provider (e.g., "OpenAI", "Anthropic")
                chatModel
            )
            .withOptionsConverter(new MyLlmOptionsConverter())  // convert LlmOptions → ChatOptions
            .withKnowledgeCutoffDate(LocalDate.of(2025, 4, 1)); // knowledge cutoff
    }
}
```

### Configuration Options

- **name** (required) — used for model selection
- **provider** (required) — e.g., "OpenAI", "Anthropic"
- **OptionsConverter** — convert Embabel `LlmOptions` to provider-specific options
- **knowledge cutoff date** — if available, adds `KnowledgeCutoffDate` prompt contributor
- **pricing model** — if available, for cost tracking
- **additional PromptContributor** objects — applied to all LLM calls

### OpenAI-Compatible LLMs

Extend `OpenAiCompatibleModelFactory` to add OpenAI-compatible providers:

```java
public class MyProviderFactory extends OpenAiCompatibleModelFactory {
    @Override
    public String getProvider() {
        return "my-provider";
    }

    @Override
    protected ChatModel createChatModel(LlmOptions options) {
        // Configure your OpenAI-compatible client
        return new ChatModelBuilder()
            .baseUrl(getBaseUrl())
            .apiKey(getApiKey())
            .build();
    }
}
```

## Key Points

- `SpringAiLlmService` wraps any Spring AI `ChatModel`
- Use `OptionsConverter` to map `LlmOptions` to provider-specific options
- Add knowledge cutoff date for temporal awareness
- Extend `OpenAiCompatibleModelFactory` for OpenAI-compatible providers