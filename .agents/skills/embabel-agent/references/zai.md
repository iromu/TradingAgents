# Z.ai (Zhipu GLM) Provider

[Z.ai](https://z.ai) (Zhipu AI) offers the GLM family of high-performance LLMs. Embabel integrates Z.ai as a first-class provider using the native Spring AI ZhiPuAI client (`spring-ai-zhipuai`) — pointed at Z.ai's international "PaaS v4" endpoint. This native integration unlocks GLM-specific features such as reasoning ("thinking"), rather than relying on the OpenAI-compatible wire protocol.

Model definitions (ids, pricing, knowledge cutoff, reasoning support) are loaded from `classpath:models/zai-models.yml` and each is registered as an `LlmService` bean.

## Add the Dependency

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-zai</artifactId>
</dependency>
```

```kotlin
implementation("com.embabel.agent:embabel-agent-starter-zai")
```

## API Key Configuration

```bash
export ZAI_API_KEY=your-api-key
```

Or in `application.yml`:

```yaml
embabel:
  agent:
    platform:
      models:
        zai:
          api-key: your-api-key
```

The environment variable `ZAI_API_KEY` takes precedence over the property value.

## Available Models

| Model Name | Model ID | Input (per 1M tokens) | Output (per 1M tokens) |
|------------|----------|-----------------------|------------------------|
| `GLM-5.2` | `glm-5.2` | $1.40 | $4.40 |
| `GLM-4.7` | `glm-4.7` | $0.60 | $2.20 |
| `GLM-4.6` | `glm-4.6` | $0.60 | $2.20 |
| `GLM-4.5-Air` | `glm-4.5-air` | $0.20 | $1.10 |
| `GLM-4.7-Flash` | `glm-4.7-flash` | Free | Free |

- `glm-5.2` — latest flagship, up to 1M token context; best default for demanding reasoning and coding tasks
- `glm-4.7` / `glm-4.6` — strong, cost-effective general-purpose models
- `glm-4.5-air` — lower latency and cost; good for intermediate steps in a multi-action agent flow
- `glm-4.7-flash` — free; well-suited to high-volume extraction and classification steps

**Mixed-strategy tip:** Embabel's per-step LLM selection makes Z.ai models well-suited to mixed strategies — use `glm-4.7-flash` or `glm-4.5-air` for extraction/classification steps, and reserve `glm-5.2` (or a premium model) for the final reasoning step.

## Using Z.ai Models

```kotlin
// Declarative
@LlmCall(llm = "glm-5.2")
fun summarize(article: Article): Summary

// Programmatic
ai.withLlm("glm-4.5-air")
    .create<Classification>("Classify this input")
```

Or map Z.ai models to roles:

```yaml
embabel:
  models:
    llms:
      cheapest: glm-4.7-flash
      best: glm-5.2
```

Then reference by role with the `#` prefix: `@LlmCall(llm = "#cheapest")`.

## Temperature Clamping

GLM models require temperature in the range `(0.0, 1.0]` — a value of exactly `0.0` is not permitted. Embabel's `ZaiOptionsConverter` clamps automatically:

- Values `<= 0.0` are raised to `0.01`
- Values `> 1.0` are lowered to `1.0`

A `DEBUG` log message is emitted whenever clamping occurs. No action required — handled transparently.

## Reasoning (Thinking)

GLM models support native reasoning. Because Embabel uses the native ZhiPuAI client, enabling `Thinking` on an `LlmOptions` turns on GLM's native reasoning mode rather than relying on prompt conventions:

```kotlin
ai.withLlm(
    LlmOptions.withModel("glm-4.7").withThinking(Thinking.withTokenBudget(2048))
).create<Answer>("Solve this step by step")
```

## Configuration Reference

```yaml
embabel:
  agent:
    platform:
      models:
        zai:
          api-key: your-api-key                   # Alternative to ZAI_API_KEY env var
          base-url: https://api.z.ai/api/paas       # Default; host + /api/paas prefix (no /v4)
          max-attempts: 4                          # Retry attempts (default: 4)
          backoff-millis: 1500                     # Initial backoff ms (default: 1500)
          backoff-multiplier: 2.0                  # Backoff multiplier (default: 2.0)
          backoff-max-interval: 60000              # Max backoff ms (default: 60000)
```

*Source: Embabel Agent v1.5.3 documentation — `references/zai`*
