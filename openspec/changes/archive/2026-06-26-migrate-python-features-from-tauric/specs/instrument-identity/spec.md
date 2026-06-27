# Spec: Instrument Identity

## ADDED Requirements

### Requirement: InstrumentIdentityAgent resolves company identity

The system SHALL resolve a ticker to its real company identity (name, sector, industry, exchange) before any agent runs.

The `InstrumentIdentityAgent` SHALL have an action `resolveIdentity(Ticker ticker)` that:
- Calls `YFinService.getTickerInfo(ticker)` to fetch company metadata from Yahoo Finance
- Returns an `InstrumentContext` record containing: `ticker`, `companyName`, `sector`, `industry`, `exchange`, `currency`
- Caches the result via `FileCache` with a 24-hour TTL
- Has a `@Condition(pre = "tickerFormatIsValid(ticker)")` guard

The resolution SHALL happen at the start of the `OrchestratorAgent.start()` action, before any other actions execute.

#### Scenario: Resolve a valid stock ticker
- **WHEN** the ticker is `AAPL`
- **THEN** `resolveIdentity` returns `InstrumentContext("AAPL", "Apple Inc.", "Technology", "Consumer Electronics", "NASDAQ", "USD")`

#### Scenario: Resolve a crypto ticker
- **WHEN** the ticker is `BTC-USD`
- **THEN** `resolveIdentity` returns `InstrumentContext("BTC-USD", "Bitcoin", "Cryptocurrency", "Digital Asset", "CRYPTO", "USD")`

#### Scenario: Resolve with cached result
- **WHEN** the ticker was resolved previously and the cache is still valid
- **THEN** `resolveIdentity` returns the cached `InstrumentContext` without making an API call

### Requirement: InstrumentContext is injected into prompts

The system SHALL inject the resolved instrument identity into every agent's prompt to prevent LLM hallucination.

A `PromptContributor` implementation SHALL:
- Read `InstrumentContext` from the blackboard
- Inject a system message with the company name, sector, industry, and exchange
- Include a warning: "You are analyzing {companyName} ({ticker}). Do not confuse it with any other company."
- Run for every LLM call (not just specific agents)

The `InstrumentContextPromptContributor` SHALL be registered as a Spring `@Component` and automatically discovered by the Embabel framework.

#### Scenario: Inject instrument context into a prompt
- **WHEN** an LLM call is made for NVDA
- **THEN** the prompt includes: "You are analyzing NVIDIA Corporation (NVDA). Sector: Semiconductors. Industry: Semiconductor. Exchange: NASDAQ."

#### Scenario: Skip injection when no context
- **WHEN** `InstrumentContext` is not on the blackboard (resolution failed)
- **THEN** the prompt contributor does not inject any context (fail-open)

### Requirement: Instrument identity validation

The system SHALL validate ticker format before attempting identity resolution.

The `InstrumentIdentityAgent` SHALL have an action `validateTicker(Ticker input)` that:
- Has a `@Condition(pre = "input is not null")` guard
- Validates the ticker format (non-empty, alphanumeric with allowed special characters)
- Returns `true` for valid tickers, `false` for invalid tickers
- The `resolveIdentity` action SHALL have `@Condition(pre = "validateTicker(ticker)")`

#### Scenario: Validate a valid ticker
- **WHEN** the ticker is `AAPL`
- **THEN** `validateTicker` returns `true`

#### Scenario: Validate an invalid ticker
- **WHEN** the ticker is `""` (empty string)
- **THEN** `validateTicker` returns `false`

#### Scenario: Resolve only valid tickers
- **WHEN** `validateTicker` returns `false`
- **THEN** `resolveIdentity` does not execute (condition not met)

### Requirement: Instrument identity graceful degradation

If the identity resolution fails (Yahoo Finance unavailable, ticker not found, etc.), the system SHALL continue without the instrument context.

The `resolveIdentity` action SHALL:
- Catch all exceptions from `YFinService.getTickerInfo()`
- Log a warning but return `null`
- Not throw an exception (fail-open design)
- Allow the pipeline to continue without `InstrumentContext` on the blackboard

#### Scenario: Handle Yahoo Finance failure
- **WHEN** `YFinService.getTickerInfo()` throws an exception
- **THEN** `resolveIdentity` returns `null` and the pipeline continues

#### Scenario: Handle unknown ticker
- **WHEN** `YFinService.getTickerInfo()` returns `null` for an unknown ticker
- **THEN** `resolveIdentity` returns `null` and the pipeline continues

## REMOVED Requirements

None.
