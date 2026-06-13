---
title: "Cache Key Bug"
type: "risk"
status: "stale"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
updated_at: "2026-06-13"
---

# Alpha Vantage Cache Key Bug

> **Status: Stale** — This bug was fixed. All methods now include relevant query parameters in their cache keys.

## What Was Fixed

The following methods previously ignored important parameters in their cache keys:

| Method | Was Ignoring | Now Uses |
|--------|-------------|----------|
| `getNews()` | Date range | `ticker_NEWS_startDate_endDate` |
| `getGlobalNews()` | topic, limit, page | `GLOBAL_NEWS_topic_limit_page` |
| `getInsiderSentiment()` | interval | `ticker_INSIDER_SENTIMENT_interval` |
| `getBalanceSheet()` | frequency | `ticker_BALANCE_SHEET_freq` |
| `getCashflow()` | frequency | `ticker_CASH_FLOW_freq` |
| `getIncomeStatement()` | frequency | `ticker_INCOME_STATEMENT_freq` |

## Remaining Risk

- **No TTL or invalidation** — cached data persists indefinitely. If Alpha Vantage updates their data, the cache won't reflect changes until manually cleared
- **Cache grows unbounded** — no size limit on the cache directory
