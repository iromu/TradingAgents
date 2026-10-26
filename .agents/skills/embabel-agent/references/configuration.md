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

## Default LLM and Roles

Decouples code from specific models:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.models.default-llm` | String | `gpt-5.6-luna` | Deployment default: an LLM name, or a role from `embabel.models.llms`/`embabel.models.roles`. A role is resolved per call, so a deployment whose key arrives at runtime has a working default without a restart |
| `embabel.models.default-embedding-model` | String | `null` | Default embedding model, or a role from `embedding-services`/`embedding-roles` (resolved per call) |
| `embabel.models.llms` | Map\<String, String\> | `{}` | Role to LLM name map |
| `embabel.models.roles` | Map\<String, Map\<String, LlmRoleConfig\>\> | `{}` | Roles with a provider dimension (nested) |
| `embabel.models.embedding-services` | Map\<String, String\> | `{}` | Role to embedding service map |
| `embabel.models.embedding-roles` | Map\<String, Map\<String, String\>\> | `{}` | Embedding roles with a provider dimension (v1.5.3) — the embedding counterpart of `roles` |

> **Default as a role (v1.5.3):** A model name is resolved once, against the services registered at startup. A role is resolved per call through the same resolver chain as any other role — so the default follows whichever provider key is active, and a deployment whose first key arrives after startup gets a working default without a restart. A model name that nothing registers falls back to the role chain. A default that nothing can satisfy does not throw like an unsatisfied role — it falls back to whatever `default-llm` resolved to at startup (the placeholder, in a deployment awaiting a key), so the caller sees the same actionable "no LLM configured" error. Startup is stricter: with no placeholder to fall back to, a `default-llm` that names nothing this deployment has registered is fatal, and the message says which of the two it is.

```yaml
embabel:
  models:
    default-llm: gpt-4o-mini
    default-embedding-model: text-embedding-3-small
    llms:
      cheapest: gpt-4o-mini
      best: gpt-4o
      reasoning: o1-preview
    embedding-services:
      fast: text-embedding-3-small
      accurate: text-embedding-3-large
```

### Roles Across Providers

The flat `llms` map names one model per role, and a model belongs to one provider — right for a deployment keyed for one provider. For BYOK apps or provider failover, give the role a provider dimension with `roles`:

```yaml
embabel:
  models:
    default-llm: gpt-4.1-mini
    roles:
      cheapest:
        openai:
          model: gpt-4.1-nano
        anthropic:
          model: claude-haiku-4-5
          temperature: 0.3   # tuning configured against the role+provider
      best:
        openai:
          model: gpt-4.1
        anthropic:
          model: claude-sonnet-4-6
```

Call sites do not change: `ai.withLlmByRole("cheapest")` resolves against whichever provider is active for the call. With no user key in play, that is the provider supplying the `default-llm`, so a single-provider deployment needs nothing further. Anything the caller sets explicitly still wins over role configuration.

A role that cannot be satisfied — the active provider has no entry for it, or the model it names is not registered — throws `NoSuitableModelException` at the point of use. It never silently falls back to the `default-llm`.

Startup validation:

| Configuration | At startup | Notes |
|---|---|---|
| Keyed deployment, `llms` names an unregistered model | Fails | A typo — the message names the role and available models |
| Keyed deployment, `roles` names an unregistered model under the deployment's own provider | Fails | Same rule as the flat map |
| Keyed deployment, `roles` names a model under *any other* provider | Not checked | That entry applies when a user brings a key for that provider |
| Deployment awaiting a key (BYOK, `default-llm` resolving to the placeholder) | Warns | Roles resolve to the placeholder until a key arrives; the error names the role and model it wanted |

A role reached through a user's own credential is resolved per call against that provider's entry only — it never falls back to the flat `llms` map, so a user's key must not silently select a model the deployment pays for.

To read what a role is *declared* to mean (for a settings screen or diagnostics), use `ModelProvider.configuredOptionsForRole(role)` — it answers against the deployment's own provider, ignoring user keys and application resolvers. Use `resolveLlmOptions` when you need to know what a specific call will really do.

See `references/llm-integration.md` for `RoleResolver` beans and `CredentialEndpointResolver` for custom providers.

## Platform Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.name` | String | `embabel-default` | Platform identity name |
| `embabel.agent.platform.description` | String | `Embabel Default Agent Platform` | Platform description |

## Logging Personality

| Value | Description |
|-------|-------------|
| `starwars` | Star Wars themed messages |
| `severance` | Severance — "Praise Kier" |
| `colossus` | Colossus: The Forbin Project |
| `hitchhiker` | Hitchhiker's Guide to the Galaxy |
| `montypython` | Monty Python |

```yaml
embabel:
  agent:
    logging:
      personality: hitchhiker
```

## Agent Scanning

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.scanning.annotation` | Boolean | `true` | Auto-register `@Agent` / `@Agentic` beans |
| `embabel.agent.platform.scanning.bean` | Boolean | `false` | Auto-register Spring beans of type `Agent` |

## Validation Manager

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.api.validation.manager.skip-agent-deployment-on-error` | Boolean | `false` | When `false` (default), the validation manager only logs errors for `@Agent`s. When `true`, any invalid `@Agent` reported by the validation manager is rejected at startup (v1.5.2) |

## Planner Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.planner.restricted-goals` | Boolean | `false` | When `true`, each `@Agent` must have a single unique return type across all `@AchievesGoal` actions; violations rejected at startup |

```yaml
embabel:
  agent:
    platform:
      planner:
        restricted-goals: true
```

## Execution Mode

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.process-type` | String | `SIMPLE` | `SIMPLE` (sequential) or `CONCURRENT` (parallel actions) |

## Autonomy Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.autonomy.agent-confidence-cut-off` | Double | `0.6` | Confidence threshold for agent selection |
| `embabel.agent.platform.autonomy.goal-confidence-cut-off` | Double | `0.6` | Confidence threshold for goal achievement |

```yaml
embabel:
  agent:
    platform:
      autonomy:
        agent-confidence-cut-off: 0.7
        goal-confidence-cut-off: 0.8
```

Certainty below these thresholds causes failure to choose an agent or goal.

## LLM Provider Configuration

### Provider Summary

| Provider | Starter | Key Env Var |
|----------|---------|-------------|
| OpenAI | `embabel-agent-starter-openai` | `OPENAI_API_KEY` |
| OpenAI Custom (Groq, Z.AI) | `embabel-agent-starter-openai-custom` | `OPENAI_CUSTOM_API_KEY` |
| Anthropic | `embabel-agent-starter-anthropic` | `ANTHROPIC_API_KEY` |
| Google Gemini (OpenAI-compatible) | `embabel-agent-starter-gemini` | `GEMINI_API_KEY` |
| Google GenAI (Native) | `embabel-agent-starter-google-genai` | `GOOGLE_API_KEY` |
| DeepSeek | `embabel-agent-starter-deepseek` | `DEEPSEEK_API_KEY` |
| OCI GenAI | `embabel-agent-starter-oci-genai` | OCI config file |
| Mistral AI | `embabel-agent-starter-mistral-ai` | `MISTRAL_API_KEY` |
| LM Studio | `embabel-agent-starter-lmstudio` | (none, local) |
| Ollama | `embabel-agent-starter-ollama` | (none, local) |
| AWS Bedrock | `embabel-agent-starter-bedrock` | AWS credentials |
| Docker Local Models | `embabel-agent-starter-docker` | (none, local) |
| ONNX Embeddings | `embabel-agent-starter-onnx` | (none, local) |

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

Env vars: `OPENAI_API_KEY` (required), `OPENAI_BASE_URL` (optional, for Azure), `OPENAI_COMPLETIONS_PATH`, `OPENAI_EMBEDDINGS_PATH`.

### Provider Timeouts (v1.5.3)

OpenAI-compatible and Docker model clients take configurable timeouts (v1.5.3):

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.models.openai.connect-timeout` | Duration | `1m` | How long to wait to connect |
| `embabel.agent.platform.models.openai.read-timeout` | Duration | _unset_ | Per-attempt response timeout. Unset keeps 60 seconds for chat completion and embedding calls and 10 minutes for Responses API models. The client retries a timed-out call twice, so a caller waits up to three times this value |

The same `connect-timeout` and `read-timeout` properties apply to every OpenAI-compatible provider, under its own prefix: `openai.custom`, `lmstudio`, `gemini`, `minimax`, `dashscope`, `zai` and `docker`, each under `embabel.agent.platform.models`.

A per-user (BYOK) key uses the same properties as the deployment's key for that provider (e.g., `embabel.agent.platform.models.openai.read-timeout`). A provider added by a `CredentialEndpointResolver` is configured under its name in lower case with spaces and punctuation removed: `Our Gateway` reads `embabel.agent.platform.models.ourgateway.read-timeout`. BYOK Gemini reads the `gemini` prefix.

> **Important:** These providers use their own `read-timeout`. The generic `embabel.agent.platform.http-client.read-timeout` does **not** apply to them.

### OpenAI Custom (Groq, Z.AI, etc.)

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

Embedding: `gemini_embedding_001` (3072 dims, $0.15/1M tokens).

### Ollama

```yaml
embabel:
  agent:
    platform:
      models:
        ollama:
          base-url: http://localhost:11434
```

Alternatively, use the OpenAI-compatible API: add `embabel-agent-starter-openai-custom` with `OPENAI_CUSTOM_BASE_URL=http://localhost:11434/v1`.

### LM Studio

```yaml
embabel:
  agent:
    platform:
      models:
        lmstudio:
          base-url: http://localhost:1234/v1
```

### OCI Generative AI

```yaml
embabel:
  agent:
    platform:
      models:
        ocigenai:
          compartment-id: ocid1.compartment.oc1...
          region: us-chicago-1
          # authentication-type: INSTANCE_PRINCIPAL | FILE | SESSION_TOKEN | SIMPLE
```

Defaults to `~/.oci/config` with profile `DEFAULT`. When the OpenAI provider is absent from the classpath, OCI supplies defaults for Embabel's default LLM and embedding model.

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

### AWS Bedrock

```yaml
embabel:
  models:
    bedrock:
      models:
        - name: anthropic.claude-3-5-sonnet-20241022-v2:0
          input-price: 3.0
          output-price: 15.0
```

### Docker Local Models

```yaml
embabel:
  docker:
    models:
      base-url: http://localhost:12434/engines
```

### ONNX Embeddings

```yaml
embabel:
  agent:
    platform:
      models:
        onnx:
          embeddings:
            enabled: true
            model-uri: file://./models/all-MiniLM-L6-v2/model.onnx
            tokenizer-uri: file://./models/all-MiniLM-L6-v2/tokenizer.json
            dimensions: 384
            model-name: all-MiniLM-L6-v2
```

### Retry / Backoff (All Cloud Providers)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `max-attempts` | Int | `10` | Max retry attempts |
| `backoff-millis` | Long | `5000` | Initial backoff (ms) |
| `backoff-multiplier` | Double | `5.0` | Backoff multiplier |
| `backoff-max-interval` | Long | `180000` | Max backoff interval (ms) |

Example — customizing OpenAI retries:

```yaml
embabel:
  agent:
    platform:
      models:
        openai:
          max-attempts: 5
          backoff-millis: 2000
          backoff-multiplier: 3.0
```

## Tool Loop

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.toolloop.type` | String | `default` | `default` (sequential) or `parallel` (experimental) |
| `embabel.agent.platform.toolloop.max-iterations` | Int | `20` | Max iterations |
| `embabel.agent.platform.toolloop.parallel.per-tool-timeout` | Duration | `30s` | Per-tool timeout in parallel mode |
| `embabel.agent.platform.toolloop.parallel.batch-timeout` | Duration | `60s` | Batch timeout in parallel mode |
| `embabel.agent.platform.toolloop.empty-response.max-retries` | Int | `0` | Retries for empty responses (weak models) |
| `embabel.agent.platform.toolloop.empty-response.nudge-message` | String | _(none)_ | Nudge message when LLM goes silent |

```yaml
embabel:
  agent:
    platform:
      toolloop:
        max-iterations: 30
        empty-response:
          max-retries: 1
```

For weak open-weights models (e.g., `gpt-oss-20b`, some Qwen variants), set `max-retries: 1` to re-invoke the LLM with a nudge. Strong frontier models should keep `0`.

## HTTP Client

Add `embabel-agent-netty-client-autoconfigure` for Reactor Netty:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.http-client.connect-timeout` | Duration | `25s` | Connection timeout |
| `embabel.agent.platform.http-client.read-timeout` | Duration | `1m` | Read timeout (increase for long responses / thinking mode) |

```yaml
embabel:
  agent:
    platform:
      http-client:
        connect-timeout: 10s
        read-timeout: 10m
```

## Template Renderer (v1.5.3)

Jinja template renderer configuration, nested in `AgentPlatformProperties`:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.template.prefix` | String | `classpath:/prompts/` | Spring resource prefix used to load templates |
| `embabel.agent.platform.template.suffix` | String | `.jinja` | Suffix added to template names that do not already include it |
| `embabel.agent.platform.template.fail-on-unknown-tokens` | Boolean | `false` | Whether rendering fails when a template contains unknown tokens |
| `embabel.agent.platform.template.nested-interpretation-enabled` | Boolean | `false` | Whether template syntax inside substituted values is evaluated recursively. Do not enable this for untrusted or user-provided values |

Template syntax inside substituted values is treated as literal data by default — this prevents values such as user-provided text containing `{{ expression }}` from being evaluated recursively. Enabling `nested-interpretation-enabled` re-evaluates template syntax found inside substituted values; do not enable it for templates that interpolate untrusted or user-provided text.

## REST Endpoints

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.rest.process-status-enabled` | Boolean | `true` | `GET /api/v1/process/{id}` |
| `embabel.agent.platform.rest.process-kill-enabled` | Boolean | `true` | `DELETE /api/v1/process/{id}` |
| `embabel.agent.platform.rest.process-events-enabled` | Boolean | `true` | `GET /events/process/{id}` (SSE) |

## Process Repository

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.process-repository.window-size` | Int | `1000` | Max processes kept in memory (default `InMemoryAgentProcessRepository`) |

## Test Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.test.mock-mode` | Boolean | `true` | Enable mock mode for testing |

## LLM Operations (Prompts & Data Binding)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.llm-operations.prompts.maybe-prompt-template` | String | `maybe_prompt_contribution` | "Maybe" prompt template (enables failure result) |
| `embabel.agent.platform.llm-operations.prompts.generate-examples-by-default` | Boolean | `true` | Generate examples by default |
| `embabel.agent.platform.llm-operations.data-binding.max-attempts` | Int | `10` | Max data-binding retry attempts |
| `embabel.agent.platform.llm-operations.data-binding.fixed-backoff-millis` | Long | `30` | Fixed backoff between retries (ms) |

## Standalone LLM Operations

Separate config prefix for LLM operations that are not agent-platform specific:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.llm-operations.prompts.default-timeout` | Duration | `60s` | Default timeout for LLM prompt operations |

## SSE Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.sse.max-buffer-size` | Int | _(default)_ | Max SSE buffer size |
| `embabel.agent.platform.sse.max-process-buffers` | Int | _(default)_ | Max process buffers for SSE |

## Process ID Generation

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.process-id-generation.include-version` | Boolean | `false` | Whether to include version in process ID generation |
| `embabel.agent.platform.process-id-generation.include-agent-name` | Boolean | `false` | Whether to include agent name in process ID generation |

## Empty Response Policy

For weak models that return empty responses after tool calls, configure an `EmptyResponsePolicy` bean or use the `toolloop.empty-response` properties above (see `references/llm-integration.md` for details).

## Module Stability

Embabel modules are classified by stability level:

| Level | Description |
|-------|-------------|
| **Stable** | Production-ready, no breaking changes without major version bump |
| **Incubating** | Under active development, may have breaking changes |
| **Experimental** | Early-stage, may be removed or significantly changed |

Key incubating modules: `embabel-agent-onnx` (ONNX embeddings), `embabel-agent-eval` (evaluation framework).
Experimental modules: `embabel-agent-discord`, `embabel-agent-remote`, `embabel-agent-skills`, `embabel-agent-spec`.

---

*Source: Embabel Agent v1.5.3 documentation — `references/configuration`, `references/llms`, `references/asynch-mode`*
