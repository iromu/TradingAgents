# Starters, providers, and what changed in v1.5.3

Moved out of `SKILL.md` so the main guide stays under the per-file token budget.
Verify against the pinned upstream docs before trusting any symbol here.

## Starters

| Starter | Use Case |
|---------|----------|
| `embabel-agent-starter` | Basic agent platform (web/console/microservice) |
| `embabel-agent-starter-shell` | Interactive CLI shell |
| `embabel-agent-starter-mcpserver` | MCP server (Streamable HTTP default, SSE deprecated — v1.5.3) |
| `embabel-agent-starter-jdbc` | Relational-database agent process persistence (v1.5.3) |
| `embabel-agent-starter-cache` | Cache-backed (Redis/Hazelcast) agent process persistence (v1.5.3) |

Embabel release binaries are published to **Maven Central** — no snapshot
repository needed for stable releases.

## LLM providers

| Provider | Starter | Key Env Var |
|----------|---------|-------------|
| OpenAI | `embabel-agent-starter-openai` | `OPENAI_API_KEY` |
| OpenAI Custom (Groq, OpenRouter) | `embabel-agent-starter-openai-custom` | `OPENAI_CUSTOM_API_KEY` |
| Anthropic | `embabel-agent-starter-anthropic` | `ANTHROPIC_API_KEY` |
| Google Gemini (OpenAI-compatible) | `embabel-agent-starter-gemini` | `GEMINI_API_KEY` |
| Google GenAI (Native, Gemini 3.x) | `embabel-agent-starter-google-genai` | `GOOGLE_API_KEY` |
| DeepSeek | `embabel-agent-starter-deepseek` | `DEEPSEEK_API_KEY` |
| OCI Generative AI | `embabel-agent-starter-oci-genai` | `~/.oci/config` |
| Mistral AI | `embabel-agent-starter-mistral-ai` | `MISTRAL_API_KEY` |
| LM Studio | `embabel-agent-starter-lmstudio` | _(none)_ |
| Ollama | `embabel-agent-starter-ollama` | _(none)_ |
| AWS Bedrock | `embabel-agent-starter-bedrock` | AWS credentials (standard Spring AI Bedrock) |
| Z.ai (Zhipu GLM, native client) | `embabel-agent-starter-zai` | `ZAI_API_KEY` |
| DashScope (Alibaba Qwen) | `embabel-agent-starter-dashscope` | `DASHSCOPE_API_KEY` |
| Docker Models | `embabel-agent-starter-dockermodels` | _(none)_ |
| MiniMax | `embabel-agent-starter-minimax` | `MINIMAX_API_KEY` |
| BYOK (user-supplied keys) | `embabel-agent-starter-byok` (Incubating) | _(runtime)_ |

Provider-specific notes:

- **Z.ai** — native `spring-ai-zhipuai` client (not OpenAI-compatible). Supports
  GLM 5.2, native reasoning/thinking, temperature clamping `(0.0, 1.0]`.
  See `zai.md`.
- **DashScope** — Alibaba Cloud Qwen 3.7 family (Max/Plus/Flash). OpenAI-compatible
  with parameter clamping. See `dashscope.md`.
- **Atlas Cloud** — OpenAI-compatible endpoint for BYOK deployments, built via
  `OpenAiCompatibleModelFactory.atlasCloud(userKey)`. See `customizing.md`.

Full provider configuration details: `configuration.md`.

## New in v1.5.3

- **MCP transport: Streamable HTTP** — the MCP server now uses Streamable HTTP
  transport by default (`http://localhost:8080/mcp`); SSE
  (`http://localhost:8080/sse`) is `@Deprecated(forRemoval=true)`.
  `protocol: STREAMABLE` (default) or `STATELESS`. See `integrations.md`.
- **Agent process persistence** — checkpoint/restore for processes parked for
  external input (HITL). `AgentProcessSnapshotStore` SPI,
  `embabel-agent-starter-jdbc` (relational) and `embabel-agent-starter-cache`
  (Redis/Hazelcast via `AgentCacheProvider`), `WAITING`/`LIFECYCLE` checkpoint
  policies, `BlackboardEntrySerializer` for blackboard values. See
  `persistence.md`, `cache.md`.
- **Action delays** — `@Agent(delay = ...)` sets a default delay tier
  (`Delay.NONE`/`MEDIUM`/`LONG`); `@Action(delayMs = ...)` overrides per action
  for rate-limited APIs. See `annotations.md`.
- **Deployment default as a role** — `embabel.models.default-llm` may name a
  role, resolved per call, so a BYOK deployment whose key arrives at runtime
  has a working default without a restart. New deployment default model:
  `gpt-5.6-luna`. See `llm-integration.md`, `configuration.md`.
- **Embedding roles under BYOK** — `default-embedding-model` may name a role;
  new `embabel.models.embedding-roles` map gives a role a model per provider.
  See `llm-integration.md`.
- **Durable asset content** — `MaterializableAsset`, `AssetStore`
  (built-in `FileSystemAssetStore`), `DurableAsset`;
  `conversation.getAssetTracker().addDurablyReturnedAssets(...)`. Explicitly
  opt-in. See `chatbots.md`.
- **Entity-only RAG results** — `entitiesOnly` on `ToolishRag` selects only
  matching entities from mixed results. See `rag.md`.
- **Provider timeouts** — `connect-timeout` / `read-timeout` under each
  OpenAI-compatible provider prefix (`openai`, `openai.custom`, `lmstudio`,
  `gemini`, `minimax`, `dashscope`, `zai`, `docker`); the generic
  `http-client.read-timeout` does not apply to them. See `configuration.md`.
- **Template renderer config** — `embabel.agent.platform.template.*`
  (prefix, suffix, `fail-on-unknown-tokens`,
  `nested-interpretation-enabled`). Substituted values are literal data by
  default. See `configuration.md`.
- **Retry policy** — `NonTransientAiException` reporting HTTP 429 (rate limit)
  is now retried; `NullPointerException` added to the non-retryable list. See
  `annotations.md`.
- **LM Studio** — no longer ships a stale OpenAI model catalog.
- **Structured output** — native structured output refinements: strict mode,
  schema fixes; OpenAI reasoning effort forwarded from `LlmOptions`.

## New in v1.5.2

- **Boot 4.1.1** baseline.
- **Validation manager exit policy** —
  `embabel.agent.api.validation.manager.skip-agent-deployment-on-error`
  rejects invalid `@Agent`s at startup when `true`. See `configuration.md`.
- **LLM retry events** — events raised when an LLM call is retried or fails;
  improved LLM retry policy and rate-limit detection.
- **Agent and Action delay policy** — foundation for the v1.5.3 `delay`
  attributes.
- **Nested Jinja interpretation** — configurable nested template
  interpretation (see `template.nested-interpretation-enabled`).
- **Agent process snapshot/restore foundation** — `AgentProcessRepository`
  assembly entry point, `AgentProcessSnapshotStore` SPI, JCache-backed
  reference implementation (shipped as the v1.5.3 persistence features).

## New in v1.5.1

- **Roles across providers** — `embabel.models.roles` gives each role a provider
  dimension (BYOK, failover). `RoleResolver` beans decide per user;
  `ModelSelectionContextHolder` carries the user context across threads.
  Unsatisfiable roles throw `NoSuitableModelException`, never a silent fallback.
  See `llm-integration.md`, `configuration.md`.
- **Embedding-based skill selection** — `EmbeddingSkillSelector` picks up to 2
  skills by embedding similarity (frontmatter `metadata: activation: embedding`,
  default threshold 0.30, fail-open). See `agent-skills.md`.
- **Atlas Cloud** — built-in OpenAI-compatible factory:
  `OpenAiCompatibleModelFactory.atlasCloud(userKey)`. See `customizing.md`.
- **Streaming scalar types** — `StringResult` wrapper for streaming plain-text
  scalars. See `streaming.md`.
- **Thinking tag control** — `Thinking.withIncludedTags(...)` /
  `withExcludedTags(...)`. See `thinking.md`.
- **Z.ai native provider** — first-class `embabel-agent-starter-zai` (GLM family,
  native thinking). See `zai.md`.
