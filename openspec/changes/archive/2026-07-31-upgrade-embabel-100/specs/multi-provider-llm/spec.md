## ADDED Requirements

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
