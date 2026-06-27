# Embabel Configuration Reference

## Enabling Embabel

Annotate your Spring Boot application class:

```java
@SpringBootApplication
public class MyAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyAgentApplication.class, args);
    }
}
```

## Configuration Properties

### Default LLM and Roles

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.models.default-llm` | String | `gpt-4.1-mini` | Default LLM name |
| `embabel.models.default-embedding-model` | String | `null` | Default embedding model |
| `embabel.models.llms` | Map<String, String> | `{}` | Map of role to LLM name |
| `embabel.models.embedding-services` | Map<String, String> | `{}` | Map of role to embedding service |

```yaml
embabel:
  models:
    default-llm: gpt-4.1-mini
    default-embedding-model: text-embedding-3-small
    llms:
      cheapest: gpt-4o-mini
      best: gpt-4o
      reasoning: o1-preview
    embedding-services:
      fast: text-embedding-3-small
      accurate: text-embedding-3-large
```

### Platform Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.name` | String | `embabel-default` | Core platform identity name |
| `embabel.agent.platform.description` | String | `Embabel Default Agent Platform` | Platform description |

### Logging Personality

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.logging.personality` | String | _(none)_ | Themed logging messages |

Available values: `starwars`, `severance`, `colossus`, `hitchhiker`, `montypython`

### Agent Scanning

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.scanning.annotation` | Boolean | `true` | Auto-register beans with `@Agent` and `@Agentic` |
| `embabel.agent.platform.scanning.bean` | Boolean | `false` | Auto-register Spring beans of type `Agent` |

### Tool Loop

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.toolloop.type` | String | `default` | `default` (sequential) or `parallel` (experimental) |
| `embabel.agent.platform.toolloop.max-iterations` | Int | `20` | Max tool loop iterations |
| `embabel.agent.platform.toolloop.parallel.per-tool-timeout` | Duration | `30s` | Timeout per tool in parallel mode |
| `embabel.agent.platform.toolloop.parallel.batch-timeout` | Duration | `60s` | Timeout for entire batch in parallel mode |
| `embabel.agent.platform.toolloop.empty-response.max-retries` | Int | `0` | Max retries for empty responses (weak models) |
| `embabel.agent.platform.toolloop.empty-response.nudge-message` | String | _(none)_ | Message appended when LLM goes silent |
| `embabel.agent.platform.toolloop.tool-not-found.max-retries` | Int | `3` | Max retries when LLM calls unknown tool name |

### Execution Mode

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.process-type` | String | `SIMPLE` | `SIMPLE` (sequential) or `CONCURRENT` (parallel actions) |

### Autonomy

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.autonomy.agent-confidence-cut-off` | Double | `0.6` | Confidence threshold for agent operations |
| `embabel.agent.platform.autonomy.goal-confidence-cut-off` | Double | `0.6` | Confidence threshold for goal achievement |

### HTTP Client

Add `embabel-agent-netty-client-autoconfigure` for Netty:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.http-client.connect-timeout` | Duration | `25s` | Connection timeout |
| `embabel.agent.platform.http-client.read-timeout` | Duration | `1m` | Read timeout |

### REST Endpoints

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.rest.process-status-enabled` | Boolean | `true` | `GET /api/v1/process/{id}` |
| `embabel.agent.platform.rest.process-kill-enabled` | Boolean | `true` | `DELETE /api/v1/process/{id}` |
| `embabel.agent.platform.rest.process-events-enabled` | Boolean | `true` | `GET /events/process/{id}` |

### Process Repository

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.process-repository.window-size` | Int | `1000` | Max processes kept in memory |

### Test Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.test.mock-mode` | Boolean | `true` | Enable mock mode for testing |

## LLM Provider Configuration

### OpenAI

```yaml
embabel:
  agent:
    platform:
      models:
        openai:
          api-key: ${OPENAI_API_KEY}
          base-url: ${OPENAI_BASE_URL:}  # Azure: https://{resource}.openai.azure.com/openai
```

Environment variables:
- `OPENAI_API_KEY` (required)
- `OPENAI_BASE_URL` (optional, for Azure)
- `OPENAI_COMPLETIONS_PATH` (optional, default: `/v1/completions`)
- `OPENAI_EMBEDDINGS_PATH` (optional, default: `/v1/embeddings`)

Add: `embabel-agent-starter-openai`

### OpenAI Custom (Groq, Together AI, Z.AI, etc.)

```yaml
embabel:
  agent:
    platform:
      models:
        openai:
          custom:
            api-key: ${OPENAI_CUSTOM_API_KEY}
            base-url: https://api.groq.com/openai
            models: llama-3.3-70b-versatile,mixtral-8x7b-32768
```

For Z.AI with non-standard paths:
```bash
export OPENAI_CUSTOM_BASE_URL="https://api.z.ai/api/coding/paas"
export OPENAI_CUSTOM_COMPLETIONS_PATH="/v4/chat/completions"
```

Add: `embabel-agent-starter-openai-custom`

### Anthropic

```yaml
embabel:
  agent:
    platform:
      models:
        anthropic:
          api-key: ${ANTHROPIC_API_KEY}
          base-url: ${ANTHROPIC_BASE_URL:}
```

Add: `embabel-agent-starter-anthropic`

### Google Gemini (OpenAI-compatible)

```yaml
embabel:
  agent:
    platform:
      models:
        gemini:
          api-key: ${GEMINI_API_KEY}
          base-url: ${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta/openai}
```

Add: `embabel-agent-starter-gemini`

### Google GenAI (Native — Gemini 3.x)

Uses the native Google GenAI SDK with full feature support including thinking mode.

```yaml
embabel:
  models:
    default-llm: gemini-3.5-flash
    default-embedding-model: gemini-embedding-001
    llms:
      fast: gemini-2.5-flash
      best: gemini-2.5-pro
      reasoning: gemini-3.1-pro-preview
  agent:
    platform:
      models:
        googlegenai:
          api-key: ${GOOGLE_API_KEY}
          # Or Vertex AI:
          # project-id: ${GOOGLE_PROJECT_ID}
          # location: ${GOOGLE_LOCATION}  # Must be 'global' for Gemini 3
          max-attempts: 10
          backoff-millis: 5000
```

Available models: `gemini-3.5-flash`, `gemini-3.1-flash-lite`, `gemini-3.1-pro-preview`, `gemini-2.5-pro`, `gemini-2.5-flash`, `gemini-2.5-flash-lite`, `gemini-2.0-flash`, `gemini-2.0-flash-lite`.

Add: `embabel-agent-starter-google-genai`

### DeepSeek

```yaml
embabel:
  agent:
    platform:
      models:
        deepseek:
          api-key: ${DEEPSEEK_API_KEY}
          base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
```

Add: `embabel-agent-starter-deepseek`

### OCI Generative AI

Add: `embabel-agent-starter-oci-genai`

Defaults to `~/.oci/config` with profile `DEFAULT`. When the OpenAI provider is not on the classpath, OCI supplies defaults for Embabel's default LLM and embedding model.

```yaml
embabel:
  agent:
    platform:
      models:
        ocigenai:
          compartment-id: ocid1.compartment.oc1...
          region: us-chicago-1
          # authentication-type: INSTANCE_PRINCIPAL | RESOURCE_PRINCIPAL | WORKLOAD_IDENTITY | SESSION_TOKEN | SIMPLE
```

### Mistral AI

```yaml
embabel:
  agent:
    platform:
      models:
        mistralai:
          api-key: ${MISTRAL_API_KEY}
          base-url: ${MISTRAL_BASE_URL:https://api.mistral.ai}
```

Add: `embabel-agent-starter-mistral-ai`

### LM Studio

```yaml
embabel:
  agent:
    platform:
      models:
        lmstudio:
          base-url: http://localhost:1234/v1
```

Add: `embabel-agent-starter-lmstudio`

### Ollama

Add: `embabel-agent-starter-ollama`

Or use the OpenAI-compatible API: add `embabel-agent-starter-openai-custom` with `OPENAI_CUSTOM_BASE_URL=http://localhost:11434/v1`.

## LlmOptions (Per-Call Configuration)

For per-call LLM configuration, use the fluent `LlmOptions` API:

```java
var options = LlmOptions.withModel("gpt-4o")
    .withTemperature(0.8)
    .withTopP(0.9)
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

## Anthropic Prompt Caching

```java
var caching = AnthropicCachingConfig.builder()
    .systemPrompt()
    .tools()
    .conversationHistory()
    .build();
```

Cache reads cost 90% less than regular tokens. Minimum size: 1024 tokens (older models) or 4096 tokens (Claude Sonnet 4.5+).

## Custom LLM Integration

Implement `LlmMessageSender` for unsupported providers:

```java
public class CustomLlmMessageSender implements LlmMessageSender {
    @Override
    public LlmMessageResponse sendMessage(List<Message> messages) {
        // Make HTTP call to your provider
        return new LlmMessageResponse(message, textContent, usage);
    }
}
```

Register as a Spring bean with `LlmService` for model discovery.

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
