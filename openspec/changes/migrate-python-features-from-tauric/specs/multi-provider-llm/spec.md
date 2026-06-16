# Spec: Multi-Provider LLM

## ADDED Requirements

### Requirement: Provider selection via configuration

The system SHALL support selecting an LLM provider via configuration.

The `TraderAgentConfig` SHALL have a new property `provider` with the following options:
- `openai-compat` (default) — OpenAI-compatible endpoint (LiteLLM)
- `openai` — Direct OpenAI API
- `anthropic` — Anthropic Claude
- `google` — Google Gemini
- `azure` — Azure OpenAI
- `bedrock` — AWS Bedrock
- `ollama` — Local Ollama

The provider SHALL be configurable via `app.llm-options.provider` and the corresponding Embabel starter dependency SHALL be active.

#### Scenario: Select Anthropic provider
- **WHEN** `app.llm-options.provider` is `anthropic`
- **THEN** LLM calls use the Anthropic provider

#### Scenario: Select Google provider
- **WHEN** `app.llm-options.provider` is `google`
- **THEN** LLM calls use the Google Gemini provider

#### Scenario: Default to OpenAI-compatible
- **WHEN** `app.llm-options.provider` is not set
- **THEN** LLM calls use the OpenAI-compatible endpoint (LiteLLM)

### Requirement: Provider-specific model configuration

The system SHALL support configuring provider-specific model names.

The `TraderAgentConfig` SHALL have the following model configuration:
- `best-model` — Model for critical decisions (Research Manager, Portfolio Manager)
- `cheapest-model` — Model for report generation (analysts, researchers)

The model names SHALL be provider-specific:
- OpenAI: `gpt-5.5`, `gpt-5.4-mini`
- Anthropic: `claude-opus-4`, `claude-sonnet-4`
- Google: `gemini-2.5-pro`, `gemini-2.5-flash`
- Azure: Azure deployment name
- Bedrock: Amazon model ID
- Ollama: Ollama model name

#### Scenario: Configure Anthropic models
- **WHEN** `best-model` is `claude-opus-4` and provider is `anthropic`
- **THEN** the Portfolio Manager uses Claude Opus 4

#### Scenario: Configure Ollama models
- **WHEN** `best-model` is `llama3.1` and provider is `ollama`
- **THEN** the Portfolio Manager uses the local llama3.1 model

### Requirement: Provider-specific settings

The system SHALL support provider-specific settings forwarded to the LLM API.

| Setting | Provider | Config key |
|---|---|---|
| Thinking level | Google | `app.llm-options.google.thinking-level` |
| Reasoning effort | OpenAI | `app.llm-options.openai.reasoning-effort` |
| Effort | Anthropic | `app.llm-options.anthropic.effort` |

The settings SHALL be forwarded to the LLM client via the Embabel `LlmOptions` API.

#### Scenario: Set Google thinking level
- **WHEN** `app.llm-options.google.thinking-level` is `high`
- **THEN** Google Gemini calls use high thinking level

#### Scenario: Set Anthropic effort
- **WHEN** `app.llm-options.anthropic.effort` is `high`
- **THEN** Anthropic Claude calls use high effort

### Requirement: Provider-specific API key configuration

Each provider SHALL use its own API key environment variable:

| Provider | Environment variable |
|---|---|
| OpenAI | `OPENAI_API_KEY` |
| Anthropic | `ANTHROPIC_API_KEY` |
| Google | `GOOGLE_API_KEY` |
| Azure | `AZURE_OPENAI_API_KEY` |
| Bedrock | AWS credentials (env / IAM / ~/.aws/credentials) |
| Ollama | No key required |

The API key SHALL be read by the Embabel starter (not by the application code).

#### Scenario: Missing API key
- **WHEN** the required API key is not set for the selected provider
- **THEN** LLM calls fail with a clear error message

#### Scenario: Ollama requires no key
- **WHEN** provider is `ollama`
- **THEN** no API key is required

### Requirement: Multi-provider Maven dependencies

The system SHALL include Embabel starter dependencies for each supported provider.

The `pom.xml` SHALL include the following optional dependencies:
- `embabel-agent-starter-openai` — OpenAI provider
- `embabel-agent-starter-anthropic` — Anthropic provider
- `embabel-agent-starter-google` — Google Gemini provider
- `embabel-agent-starter-azure` — Azure OpenAI provider
- `embabel-agent-starter-bedrock` — AWS Bedrock provider
- `embabel-agent-starter-ollama` — Ollama provider

The existing `embabel-agent-starter-openai-custom` SHALL remain for OpenAI-compatible (LiteLLM) support.

#### Scenario: Add Anthropic starter
- **WHEN** `embabel-agent-starter-anthropic` is added to `pom.xml`
- **THEN** the Anthropic provider is available for selection

#### Scenario: Unused starters don't cause errors
- **WHEN** multiple starters are present but only one is selected
- **THEN** unused starters do not cause import errors or startup failures

## REMOVED Requirements

None.
