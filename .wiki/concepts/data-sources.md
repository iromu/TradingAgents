---
title: "Data Sources"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
  - "src/main/java/com/embabel/gekko/dataflows/YFinService.java"
  - "src/main/java/com/embabel/gekko/dataflows/FredService.java"
  - "src/main/java/com/embabel/gekko/dataflows/PolymarketService.java"
  - "src/main/java/com/embabel/gekko/dataflows/VendorRouter.java"
updated_at: "2026-07-06"
---

# Data Sources

Gekko fetches financial and market data from multiple external sources, abstracted behind services so the system can switch between them.

## Alpha Vantage

The primary data source for fundamental and news data.

- **API:** `https://www.alphavantage.co/`
- **Key endpoints:** OVERVIEW, BALANCE_SHEET, CASH_FLOW, INCOME_STATEMENT, NEWS_SENTIMENT, INSIDER_SENTIMENT, INSIDER_TRANSACTIONS
- **Rate limit:** Free tier allows 25 requests/day
- **Caching:** All responses cached to `data/alphavantage/` as JSON files
- **Config:** API key from `app.alphavantage.api-key`
- **Conditional:** Disabled via `@ConditionalOnProperty` when `app.alphavantage.enabled: false`

## Yahoo Finance

Used for stock price data, technical indicator calculations, and company identity resolution.

- **Library:** `YahooFinanceAPI`
- **Key data:** Historical OHLCV prices, company stats, ticker info
- **Used by:** `YFinService` → `MarketDataTools` → LLM agents
- **Identity resolution:** `InstrumentIdentityAgent` uses Yahoo Finance to resolve ticker → company identity

## FRED (Federal Reserve Economic Data)

Macroeconomic indicators for broader market context.

- **API:** `https://api.stlouisfed.org/fred/`
- **Key data:** GDP, CPI, unemployment, federal funds rate, Treasury yields
- **Dashboard:** Pre-configured set of default macro indicators (GDP, CPIAUCSL, UNRATE, FEDFUNDS, TB3MS)
- **Config:** API key from `app.fred.api-key`
- **Tools:** `FredDataTools` exposes FRED data as LLM tools
- **Caching:** Results cached via `FileCache` with `fred:` prefix

## Polymarket

Prediction market data for sentiment and probability signals.

- **API:** `https://clob.polymarket.com/` (no API key required)
- **Key data:** Market probabilities, outcome prices, trading volume
- **Tools:** `PolymarketDataTools` exposes prediction market data as LLM tools
- **Caching:** Results cached via `FileCache` with `polymarket:` prefix

## Vendor Router

`VendorRouter` is the abstraction layer that routes tool calls to the appropriate data source. It allows the system to:

- Switch between Alpha Vantage and Yahoo Finance without changing caller code
- Potentially add more data sources in the future
- Centralize error handling and retry logic

## Caching

All data sources use `[[file-cache]]` for local disk caching. Cached files are stored in:

- **Alpha Vantage:** `data/alphavantage/` (raw JSON responses)
- **LLM responses:** `data/llm/cache/` (cached LLM outputs)
- **FRED:** `data/llm/cache/` (markdown tables)
- **Polymarket:** `data/llm/cache/` (markdown tables)