---
title: "Data Sources"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
  - "src/main/java/com/embabel/gekko/dataflows/YFinService.java"
  - "src/main/java/com/embabel/gekko/dataflows/VendorRouter.java"
updated_at: "2026-06-13"
---

# Data Sources

Gekko fetches financial and market data from two external sources, abstracted behind a `VendorRouter` so the system can switch between them.

## Alpha Vantage

The primary data source for fundamental and news data.

- **API:** `https://www.alphavantage.co/`
- **Key endpoints:** OVERVIEW, BALANCE_SHEET, CASH_FLOW, INCOME_STATEMENT, NEWS_SENTIMENT, INSIDER_SENTIMENT, INSIDER_TRANSACTIONS
- **Rate limit:** Free tier allows 25 requests/day
- **Caching:** All responses cached to `data/alphavantage/` as JSON files
- **Config:** API key from `app.alphavantage.api-key`

## Yahoo Finance

Used for stock price data and technical indicator calculations.

- **Library:** `YahooFinanceAPI` (version 3.17.0)
- **Key data:** Historical OHLCV prices, company stats
- **Used by:** `YFinService` → `MarketDataTools` → LLM agents

## Vendor Router

`VendorRouter` is the abstraction layer that routes tool calls to the appropriate data source. It allows the system to:

- Switch between Alpha Vantage and Yahoo Finance without changing caller code
- Potentially add more data sources in the future
- Centralize error handling and retry logic

## Caching

Both data sources use `[[file-cache]]` for local disk caching. Cached files are stored in:

- **Alpha Vantage:** `data/alphavantage/` (raw JSON responses)
- **LLM responses:** `data/llm/cache/` (cached LLM outputs)
