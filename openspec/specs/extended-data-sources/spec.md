# Spec: Extended Data Sources

## Purpose

Extend the trading agent's data sources beyond market data to include macroeconomic indicators (FRED) and prediction markets (Polymarket).

## Requirements

### Requirement: FredDataTools exposes FRED data to the LLM

The system SHALL provide `FredDataTools` as an `@LlmTool`-annotated component that exposes FRED macroeconomic data to the LLM.

The `FredDataTools` SHALL have the following `@LlmTool` methods:
- `getMacroIndicators(String seriesId)` — Fetch a single FRED series (e.g., GDP, CPI, UNRATE)
- `getMacroDashboard(String seriesIds)` — Fetch multiple series at once (comma-separated IDs)

Each method SHALL:
- Accept a `@ToolParam` describing what to fetch
- Return formatted markdown tables (matching existing tool output format)
- Use `FredService` for the actual HTTP requests
- Handle errors gracefully (return "NO_DATA_AVAILABLE" message, not exceptions)

The `FredDataTools` SHALL be annotated with `@EmbelabelComponent` for auto-discovery by Spring.

#### Scenario: LLM calls getMacroIndicators
- **WHEN** the LLM calls `getMacroIndicators("CPIAUCSL")`
- **THEN** the tool returns a markdown table with CPI data

#### Scenario: LLM calls getMacroDashboard
- **WHEN** the LLM calls `getMacroDashboard("UNRATE,GDP,FEDFUNDS")`
- **THEN** the tool returns markdown tables for all three series

### Requirement: PolymarketDataTools exposes prediction market data to the LLM

The system SHALL provide `PolymarketDataTools` as an `@LlmTool`-annotated component that exposes Polymarket prediction market data to the LLM.

The `PolymarketDataTools` SHALL have the following `@LlmTool` method:
- `getPredictionMarkets(String query)` — Search for prediction markets by topic

The method SHALL:
- Accept a `@ToolParam` describing the market topic
- Return formatted markdown tables with market outcomes and probabilities
- Use `PolymarketService` for the actual HTTP requests
- Handle errors gracefully (return "NO_DATA_AVAILABLE" message, not exceptions)

Polymarket requires no API key.

#### Scenario: LLM calls getPredictionMarkets
- **WHEN** the LLM calls `getPredictionMarkets("Fed rate cut")`
- **THEN** the tool returns a markdown table with Polymarket probabilities

### Requirement: FredService fetches from FRED API

The system SHALL provide a `FredService` that fetches macroeconomic data from the FRED API.

The `FredService` SHALL have the following methods:
- `getSeries(String seriesId, int limit)` — Fetch observations for a single series
- `getMultipleSeries(List<String> seriesIds)` — Fetch multiple series
- `getDashboard()` — Fetch a standard set of macro indicators

The service SHALL:
- Use `RestTemplate` for HTTP requests
- Cache responses via `FileCache` (keyed by series ID + limit)
- Format results as markdown tables
- Handle API errors gracefully (return empty results, not exceptions)

The FRED API key SHALL be read from `app.fred.api-key` (environment variable: `FRED_API_KEY`).

#### Scenario: Fetch GDP series
- **WHEN** `getSeries("GDP", 100)` is called
- **THEN** the service returns a markdown table with GDP data

#### Scenario: Cache hit for series
- **WHEN** `getSeries("CPIAUCSL", 365)` is called twice
- **THEN** the second call uses the cached response (no HTTP request)

#### Scenario: Missing API key
- **WHEN** `app.fred.api-key` is not set
- **THEN** `getSeries` returns an empty markdown table

### Requirement: PolymarketService fetches from Polymarket API

The system SHALL provide a `PolymarketService` that fetches prediction market data from the Polymarket API.

The `PolymarketService` SHALL have the following methods:
- `searchMarkets(String query)` — Search markets by topic
- `getMarket(String slug)` — Get a specific market by slug

The service SHALL:
- Use `RestTemplate` for HTTP requests
- Cache responses via `FileCache` (keyed by query + timestamp)
- Format results as markdown tables
- Handle API errors gracefully (return empty results, not exceptions)

No API key is required for Polymarket.

#### Scenario: Search Polymarket markets
- **WHEN** `searchMarkets("Fed rate cut")` is called
- **THEN** the service returns a markdown table with matching markets and probabilities

#### Scenario: Cache hit for search
- **WHEN** `searchMarkets("Fed rate cut")` is called twice
- **THEN** the second call uses the cached response (no HTTP request)

### Requirement: VendorRouter supports new data categories

The `VendorRouter` SHALL be extended with new data source categories:

| Category | Tool | Vendor |
|---|---|---|
| `MACRO_DATA` | `getMacroIndicators`, `getMacroDashboard` | FRED |
| `PREDICTION_MARKETS` | `getPredictionMarkets` | Polymarket |
| `SENTIMENT_DATA` | (reserved) | (future: StockTwits, Reddit) |

The `VendorRouter` SHALL route requests to the appropriate service based on the category.

#### Scenario: Route macro data to FRED
- **WHEN** the LLM calls a tool in the `MACRO_DATA` category
- **THEN** the request is routed to `FredService`

#### Scenario: Route prediction markets to Polymarket
- **WHEN** the LLM calls a tool in the `PREDICTION_MARKETS` category
- **THEN** the request is routed to `PolymarketService`

### Requirement: Data source configuration

Data source configuration SHALL be controlled by application properties:

| Config key | Default | Description |
|---|---|---|
| `app.fred.api-key` | (empty) | FRED API key |
| `app.fred.enabled` | `true` | Enable/disable FRED data |
| `app.polymarket.enabled` | `true` | Enable/disable Polymarket data |

When a data source is disabled, its tools return "NO_DATA_AVAILABLE" messages.

#### Scenario: FRED disabled
- **WHEN** `app.fred.enabled` is `false`
- **THEN** `FredDataTools` methods return "NO_DATA_AVAILABLE"