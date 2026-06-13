---
title: "Data Flow Services"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
  - "src/main/java/com/embabel/gekko/dataflows/YFinService.java"
  - "src/main/java/com/embabel/gekko/dataflows/VendorRouter.java"
  - "src/main/java/com/embabel/gekko/tools/FundamentalDataTools.java"
  - "src/main/java/com/embabel/gekko/tools/NewsDataTools.java"
  - "src/main/java/com/embabel/gekko/tools/MarketDataTools.java"
updated_at: "2026-06-13"
---

# Data Flow Services

The data layer fetches stock market data from external APIs and caches results locally.

## Alpha Vantage Service

`AlphaVantageService` is the primary data provider. It wraps the Alpha Vantage REST API with:

- **HTTP timeouts**: 10s connect, 30s read (configurable)
- **File-based caching**: Responses saved to `data/alphavantage/` as JSON
- **API key**: Injected from `app.alphavantage.api-key` (default: `dummy_key`)

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

## Vendor Router

`VendorRouter` abstracts which data source to use, allowing the system to switch between Alpha Vantage and Yahoo Finance without changing caller code.

## Tool Classes

- **`FundamentalDataTools`** — Exposes financial data methods as LLM tools (callable by agent prompts)
- **`NewsDataTools`** — Exposes news and sentiment methods as LLM tools
- **`MarketDataTools`** — Exposes stock data and technical indicator methods as LLM tools → `[[market-data-tools]]`

## Cache Behavior

All data service methods use file-based caching → `[[file-cache]]`.
