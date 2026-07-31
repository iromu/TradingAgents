# Spec: Multi-Provider LLM

## Purpose

Support multiple LLM providers (OpenAI-compatible, LiteLLM, etc.) with provider-specific configuration and model selection.

## Requirements

### Requirement: LLM provider abstraction

The system SHALL abstract LLM calls behind a provider-agnostic interface that supports multiple backends.

The `LlmOptions` configuration SHALL support:
- `provider` — The LLM provider name (e.g., `openai-custom`, `litellm`)
- `model` — The model name to use
- `baseUrl` — The API endpoint URL
- `apiKey` — The API key (from environment or config)

#### Scenario: Configure OpenAI-compatible provider
- **WHEN** `provider=openai-custom`, `baseUrl=http://spark.local:4000`, `model=gpt-4o`
- **THEN** LLM calls are routed to the LiteLLM endpoint

#### Scenario: Configure different provider
- **WHEN** `provider=openai-custom`, `baseUrl=https://api.openai.com`, `model=gpt-4`
- **THEN** LLM calls are routed to OpenAI's API directly

### Requirement: Additional LLM providers are available

The system SHALL be able to connect to additional LLM providers beyond OpenAI-compatible endpoints. Embabel 1.0.0 adds native support for:
- AWS Bedrock (`embabel-agent-starter-bedrock`)
- MiniMax (`embabel-agent-starter-minimax`)
- Z.ai / Zhipu GLM (`embabel-agent-starter-zai`)
- Docker Models (`embabel-agent-starter-dockermodels`)
- Google GenAI / Gemini 3.x (`embabel-agent-starter-google-genai`)

These providers are available as optional dependencies and can be added to the project's `pom.xml` when needed. No code changes are required for this upgrade — the providers are available for future use.

#### Scenario: Bedrock provider can be added
- **WHEN** `embabel-agent-starter-bedrock` is added as a dependency
- **THEN** the application can connect to AWS Bedrock models via the standard `LlmOptions` API

#### Scenario: MiniMax provider can be added
- **WHEN** `embabel-agent-starter-minimax` is added as a dependency
- **THEN** the application can connect to MiniMax models via the standard `LlmOptions` API

### Requirement: Few-shot prompting with withExample()

The system SHALL support few-shot prompting via the `.withExample()` method on the LLM fluent API. This improves output quality for structured tasks by providing examples.

#### Scenario: Example improves structured output
- **WHEN** `.withExample("description", exampleObject)` is called on an LLM call
- **THEN** the example is rendered as JSON and included in the prompt as a few-shot example

### Requirement: Native structured output support

The system SHALL support native structured output via `NativeStructuredOutputMode`, enabling model providers to enforce JSON Schema validation directly.

#### Scenario: Native structured output enabled
- **WHEN** `.withNativeStructuredOutput(NativeStructuredOutputMode.ENABLED)` is set on `LlmOptions`
- **THEN** the model provider validates output against the target class's JSON Schema natively