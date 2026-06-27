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