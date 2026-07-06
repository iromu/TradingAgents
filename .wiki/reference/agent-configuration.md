---
title: "Agent Configuration"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/config/TraderAgentConfig.java"
  - "src/main/java/com/embabel/gekko/config/AgentScanningConfiguration.java"
  - "src/main/resources/application.yaml"
  - "src/main/resources/application-app.yaml"
updated_at: "2026-07-06"
---

# Agent Configuration

## TraderAgentConfig

`src/main/java/com/embabel/gekko/config/TraderAgentConfig.java`

```java
@ConfigurationProperties("app.llm-options")
public record TraderAgentConfig(
    LlmOptions tickerLlm,
    LlmOptions writerLlm,
    int maxConcurrency,
    RoleGoalBackstory researcher,
    RoleGoalBackstory outliner,
    RoleGoalBackstory writer,
    String outputDirectory,
    double similarityThreshold,
    int maxDebateIterations,
    String provider,
    String bestModel,
    String cheapestModel,
    AnthropicProviderConfig anthropic,
    GoogleProviderConfig google,
    OpenAiProviderConfig openai
)
```

## Configuration Properties

| Property | Type | Default | Purpose |
|----------|------|---------|---------|
| `app.llm-options.ticker-llm` | `LlmOptions` | Auto-selected | LLM for ticker extraction |
| `app.llm-options.writer-llm` | `LlmOptions` | Auto-selected | LLM for report writing |
| `app.llm-options.max-concurrency` | `int` | (configurable) | Max parallel agent threads |
| `app.llm-options.researcher` | `RoleGoalBackstory` | (configurable) | Personality for researchers |
| `app.llm-options.outliner` | `RoleGoalBackstory` | (configurable) | Personality for outliners |
| `app.llm-options.writer` | `RoleGoalBackstory` | (configurable) | Personality for writers |
| `app.llm-options.output-directory` | `String` | (configurable) | Where to save outputs |
| `app.llm-options.similarity-threshold` | `double` | 0.8 | Jaccard similarity threshold for debate convergence |
| `app.llm-options.max-debate-iterations` | `int` | 5 | Max rounds for investment debate |
| `app.llm-options.provider` | `String` | (configurable) | LLM provider name |
| `app.llm-options.best-model` | `String` | (configurable) | Best model name |
| `app.llm-options.cheapest-model` | `String` | (configurable) | Cheapest model name |

## Provider-Specific Configuration

The config supports provider-specific settings for Anthropic, Google, and OpenAI:

### Anthropic

| Property | Type | Purpose |
|----------|------|---------|
| `app.llm-options.anthropic.effort` | `String` | Thinking effort level |

### Google

| Property | Type | Purpose |
|----------|------|---------|
| `app.llm-options.google.thinking-level` | `String` | Thinking level configuration |

### OpenAI

| Property | Type | Purpose |
|----------|------|---------|
| `app.llm-options.openai.reasoning-effort` | `String` | Reasoning effort level |

## LLM Options

`LlmOptions` is an Embabel data class that controls model selection:

| Field | Purpose |
|-------|---------|
| `model` | Specific model name |
| `role` | Model role (CHEAPEST_ROLE or BEST_ROLE) |
| `maxTokens` | Token limit for responses |

## Default Validation

The constructor validates and sets defaults:
- `tickerLlm` and `writerLlm` default to `LlmOptions.withDefaultLlm()` if null
- `similarityThreshold` defaults to 0.8 if ≤ 0 or > 1
- `maxDebateIterations` defaults to 5 if ≤ 0

## Agent Scanning Configuration

`AgentScanningConfiguration` registers the Embabel agent scanning BeanPostProcessor so that `@Agent`-annotated classes are automatically registered as agent beans.

- **Conditional:** Enabled when `embabel.agent.platform.scanning.annotation: true` (default: true)
- **Beans:** `DelegatingAgentScanningBeanPostProcessor` and `AgentScanningPostProcessorDelegate`

## Global LLM Configuration

In `application.yaml`:

```yaml
embabel:
  models:
    default-llm: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
    llms:
      cheapest: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
      best: ${OPENAI_MODEL:Qwen3.6-35B-A3B}
```

## Profiles

| Profile | File | Purpose |
|---------|------|---------|
| `base` | `application.yaml` | Default config |
| `app` | `application-app.yaml` | App-specific overrides |
| `observability` | `application-observability.yaml` | Tracing config |
| `local` | `application-local.yaml` | Local dev (gitignored) |

## Native Image Support

`TraderAgentRuntimeHintsRegistrar` registers reflection hints for GraalVM native image compilation, ensuring all agent state records are accessible at runtime.