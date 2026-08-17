# Spec: Instrument Identity

## Purpose

Resolve ticker symbols to real company identity information before any agent runs, providing context about company name, sector, industry, and exchange.

## Requirements

### Requirement: InstrumentIdentityAgent resolves company identity

The system SHALL resolve a ticker to its real company identity (name, sector, industry, exchange) before any agent runs.

The `InstrumentIdentityAgent` SHALL have an action `resolveIdentity(Ticker ticker)` that:
- Calls `YFinService.getTickerInfo(ticker)` to fetch company metadata from Yahoo Finance
- Returns an `InstrumentContext` record containing: `ticker`, `companyName`, `sector`, `industry`, `exchange`, `resolutionStatus`

#### Scenario: Resolve a known ticker
- **WHEN** the pipeline runs for NVDA
- **THEN** the agent returns `InstrumentContext` with `companyName="NVIDIA Corporation"`, `sector="Technology"`, `industry="Semiconductors"`, `exchange="NASDAQ"`

#### Scenario: Resolve an unknown ticker
- **WHEN** the pipeline runs for an invalid ticker
- **THEN** the agent returns `InstrumentContext` with `resolutionStatus="FAILED"` and empty metadata

### Requirement: Identity metadata sanitization

The system SHALL sanitize resolved instrument identity metadata (company name, sector, industry, exchange) before it is injected into any agent prompt and before it is written to the on-disk identity cache, so that a malicious or malformed identity value cannot persist a prompt-injection payload through the cache.

#### Scenario: Identity metadata is sanitized before prompt injection
- **GIVEN** a resolved `InstrumentContext` whose fields may contain template syntax or control characters
- **WHEN** the identity is injected into an agent prompt
- **THEN** the fields have been passed through the shared sanitizer first

#### Scenario: Identity metadata is sanitized before caching
- **WHEN** a resolved identity is written to the on-disk cache
- **THEN** the stored value is the sanitized form, so a poisoned value cannot be persisted and later replayed