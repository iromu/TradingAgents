## ADDED Requirements

### Requirement: Identity metadata sanitization

The system SHALL sanitize resolved instrument identity metadata (company name, sector, industry, exchange) before it is injected into any agent prompt and before it is written to the on-disk identity cache, so that a malicious or malformed identity value cannot persist a prompt-injection payload through the cache.

#### Scenario: Identity metadata is sanitized before prompt injection
- **GIVEN** a resolved `InstrumentContext` whose fields may contain template syntax or control characters
- **WHEN** the identity is injected into an agent prompt
- **THEN** the fields have been passed through the shared sanitizer first

#### Scenario: Identity metadata is sanitized before caching
- **WHEN** a resolved identity is written to the on-disk cache
- **THEN** the stored value is the sanitized form, so a poisoned value cannot be persisted and later replayed
