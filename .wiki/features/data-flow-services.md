---
title: "Data Flow Services"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
  - "src/main/java/com/embabel/gekko/dataflows/YFinService.java"
  - "src/main/java/com/embabel/gekko/dataflows/FredService.java"
  - "src/main/java/com/embabel/gekko/dataflows/PolymarketService.java"
  - "src/main/java/com/embabel/gekko/dataflows/VendorRouter.java"
  - "src/main/java/com/embabel/gekko/tools/MarketDataTools.java"
  - "src/main/java/com/embabel/gekko/tools/FredDataTools.java"
  - "src/main/java/com/embabel/gekko/tools/PolymarketDataTools.java"
updated_at: "2026-07-06"
---

# Data Flow Services

The data layer fetches financial and market data from external APIs and caches results locally.

## Alpha Vantage Service

`AlphaVantageService` is the primary data provider. It wraps the Alpha Vantage REST API with:

- **HTTP timeouts**: 10s connect, 30s read (configurable)
- **File-based caching**: Responses saved to `data/alphavantage/` as JSON
- **API key**: Injected from `app.alphavantage.api-key` (default: `dummy_key`)
- **Conditional:** Behind `@ConditionalOnProperty` — disabled when `app.alphavantage.enabled: false`

### Endpoints

| Method | Alpha Vantage Function | Purpose |
|--------|----------------------|---------|
| `getFundamentals()` | OVERVIEW | Company financial overview |
| `getBalanceSheet()` | BALANCE_SHEET | Balance sheet data |
| `getCashflow()` | CASH_FLOW | Cash flow statement |
| `getIncomeStatement()` | INCOME_STATEMENT | Income statement |
| `getNews()` | NEWS_SENTIMENT | News with sentiment scores |
| `getGlobalNews()` | NEWS_SENTIMENT | Global news by topic |
| `getInsiderSentiment()` | INSIDER_SENTIMENT | Insider sentiment data |
| `getInsiderTransactions()` | INSIDER_TRANSACTIONS | Insider buying/selling |

## Yahoo Finance Service

`YFinService` provides an alternative data source using the Yahoo Finance API via the `YahooFinanceAPI` library. It's used for:
- Stock price data
- Technical indicator calculations
- Company identity resolution (name, sector, exchange)

## FRED Service

`FredService` fetches macroeconomic indicators from the Federal Reserve Economic Data API.

- **API:** `https://api.stlouisfed.org/fred/`
- **Key methods:** `getSeries()`, `getMultipleSeries()`, `getDashboard()`
- **Dashboard defaults:** GDP, CPIAUCSL, UNRATE, FEDFUNDS, TB3MS
- **Output:** Markdown tables with date, value, change, change percent
- **Caching:** `fred:{seriesId}:{limit}` key prefix

## Polymarket Service

`PolymarketService` fetches prediction market data for sentiment and probability signals.

- **API:** `https://clob.polymarket.com/` (no API key required)
- **Key methods:** `searchMarkets()`, `getMarket()`
- **Output:** Markdown tables with market, outcome, probability
- **Caching:** `polymarket:search:{query}` and `polymarket:market:{slug}` key prefixes

## Vendor Router

`VendorRouter` abstracts which data source to use, allowing the system to switch between Alpha Vantage and Yahoo Finance without changing caller code.

## Tool Classes

- **`MarketDataTools`** — Exposes stock data and technical indicator methods as LLM tools
- **`FredDataTools`** — Exposes FRED macroeconomic data methods as LLM tools
- **`PolymarketDataTools`** — Exposes prediction market methods as LLM tools

## Cache Behavior

All data service methods use file-based caching → `[[file-cache]]`.

## Configuration

| Service | Config Property | Required |
|---------|----------------|----------|
| Alpha Vantage | `app.alphavantage.api-key` | Yes (for data) |
| FRED | `app.fred.api-key` | Yes (for data) |
| Polymarket | None | No |
| Yahoo Finance | None | No |