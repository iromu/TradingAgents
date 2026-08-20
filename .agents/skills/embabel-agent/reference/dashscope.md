# DashScope Provider (Alibaba Cloud Qwen)

Alibaba Cloud DashScope offers the Qwen family of high-performance LLMs via an OpenAI-compatible API. Embabel integrates DashScope as a first-class provider.

## Setup

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-dashscope</artifactId>
</dependency>
```

## API Key

```bash
export DASHSCOPE_API_KEY=your-api-key
```

Or in `application.yml`:

```yaml
embabel:
  agent:
    platform:
      models:
        dashscope:
          api-key: your-api-key
```

The environment variable `DASHSCOPE_API_KEY` takes precedence.

## Models

| Model | ID | Input (per 1M) | Output (per 1M) |
|-------|----|----------------|-----------------|
| Qwen3.7-Max (flagship reasoning) | `qwen3.7-max` | $2.50 | $7.50 |
| Qwen3.7-Plus (general-purpose) | `qwen3.7-plus` | $0.40 | $1.60 |
| Qwen3.7-Flash (default, fastest) | `qwen3.7-flash` | $0.03 | $0.13 |

Use `qwen3.7-flash` for extraction/classification steps and reserve `qwen3.7-max` for final reasoning.

## Usage

```kotlin
// By model ID
@LlmCall(llm = "qwen3.7-max")
fun summarize(article: Article): Summary

// Programmatic
ai.withLlm("qwen3.7-flash").create<Classification>("Classify this input")

// Via DashScopeModels constants
import com.embabel.agent.api.models.DashScopeModels

@LlmCall(llm = DashScopeModels.QWEN3_7_MAX)
fun reason(complex: ComplexProblem): Solution

// Via role mapping
ai.withLlmByRole("best").createObject("Analyze", Report::class.java)
```

```yaml
embabel:
  models:
    llms:
      cheapest: qwen3.7-flash
      best: qwen3.7-max
```

## Parameter Clamping

Embabel automatically clamps out-of-range parameters:

- **temperature**: `[0.0, 2.0)` — values `< 0.0` raised to `0.0`, `>= 2.0` lowered to `1.99`
- **top_p**: `(0.0, 1.0]` — values `<= 0.0` raised to `0.01`, `> 1.0` lowered to `1.0`

A `DEBUG` log message is emitted on clamping.

## Regional Endpoints

| Region | Base URL |
|--------|----------|
| China (Beijing) *(default)* | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| Singapore | `https://dashscope-intl.aliyuncs.com/compatible-mode/v1` |
| US (Virginia) | `https://dashscope-us.aliyuncs.com/compatible-mode/v1` |
| China (Hong Kong) | `https://cn-hongkong.dashscope.aliyuncs.com/compatible-mode/v1` |

```bash
export DASHSCOPE_BASE_URL=https://dashscope-intl.aliyuncs.com/compatible-mode/v1
```

## Configuration Reference

```yaml
embabel:
  agent:
    platform:
      models:
        dashscope:
          api-key: your-api-key
          base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
          max-attempts: 4
          backoff-millis: 1500
          backoff-multiplier: 2.0
          backoff-max-interval: 60000
```

---
*Source: Embabel Agent v1.5.0 documentation*
