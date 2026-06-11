---
title: "Agent Configuration"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/config/TraderAgentConfig.java"
  - "src/main/resources/application.yaml"
  - "src/main/resources/application-app.yaml"
updated_at: "2026-06-11"
---

# Agent Configuration

## File

`src/main/java/com/embabel/gekko/config/TraderAgentConfig.java`

## Record Definition

```java
@ConfigurationProperties("app.llm-options")
public record TraderAgentConfig(
    LlmOptions tickerLlm,
    LlmOptions writerLlm,
    int maxConcurrency,
    RoleGoalBackstory researcher,
    RoleGoalBackstory outliner,
    RoleGoalBackstory writer,
    String outputDirectory
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

## LLM Options

`LlmOptions` is an Embabel data class that controls model selection:

| Field | Purpose |
|-------|---------|
| `model` | Specific model name |
| `role` | Model role (CHEAPEST_ROLE or BEST_ROLE) |
| `maxTokens` | Token limit for responses |

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
