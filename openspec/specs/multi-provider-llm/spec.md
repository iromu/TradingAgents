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