---
title: "Cache Key Bug"
type: "risk"
status: "mitigated"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
updated_at: "2026-08-02"
---

# Cache Key Bug

> **Status: Mitigated** — All methods include relevant query parameters in their cache keys. The FileCache uses SHA-256 hashing to prevent path traversal and hash collisions.

## Current State

All Alpha Vantage methods include relevant parameters in cache keys:

| Method | Cache Key |
|--------|-----------|
| `getNews()` | `ticker_NEWS_startDate_endDate` |
| `getGlobalNews()` | `GLOBAL_NEWS_topic_limit_page` |
| `getInsiderSentiment()` | `ticker_INSIDER_SENTIMENT_interval` |
| `getBalanceSheet()` | `ticker_BALANCE_SHEET_freq` |
| `getCashflow()` | `ticker_CASH_FLOW_freq` |
| `getIncomeStatement()` | `ticker_INCOME_STATEMENT_freq` |

The FileCache hashes all keys via SHA-256, so special characters in keys cannot cause path traversal or filename collisions.

## Remaining Risk

- **No TTL or invalidation** — cached data persists indefinitely. If Alpha Vantage updates their data, the cache won't reflect changes until manually cleared
- **Cache grows unbounded** — no size limit on the cache directory
