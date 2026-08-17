---
title: "Cache Key Bug"
type: "risk"
status: "mitigated"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
  - "src/main/java/com/embabel/gekko/util/FileCache.java"
  - "src/main/java/com/embabel/gekko/util/ResultCache.java"
updated_at: "2026-08-17"
---

# Cache Key Bug

> **Status: Mitigated** — All methods include relevant query parameters in their cache keys. `ResultCache.canonicalKey()` normalizes case across all key parts. `FileCache` uses SHA-256 hashing to prevent path traversal and hash collisions. `ResultCache` adds per-category TTL to prevent stale data.

## Current State

All Alpha Vantage methods use `ResultCache` with case-normalized, input-complete cache keys:

| Method | Cache Key Pattern |
|--------|-------------------|
| `getNews()` | `external_http␟ALPHAVANTAGE␟NEWS␟TICKER␟STARTDATE␟ENDDATE` |
| `getGlobalNews()` | `external_http␟ALPHAVANTAGE␟GLOBAL_NEWS␟TOPIC␟LIMIT␟PAGE` |
| `getInsiderSentiment()` | `external_http␟ALPHAVANTAGE␟INSIDER_SENTIMENT␟TICKER␟INTERVAL` |
| `getBalanceSheet()` | `external_http␟ALPHAVANTAGE␟BALANCE_SHEET␟TICKER␟FREQ` |
| `getCashflow()` | `external_http␟ALPHAVANTAGE␟CASH_FLOW␟TICKER␟FREQ` |
| `getIncomeStatement()` | `external_http␟ALPHAVANTAGE␟INCOME_STATEMENT␟TICKER␟FREQ` |

(`␟` = ASCII unit separator `\u001F`)

The `ResultCache` also guards against caching error/rate-limit payloads, and applies per-category TTLs (quote: 5m, external-http: 1h).

## Remaining Risk

- **Cache grows unbounded** — no size limit on the cache directory. Safe to clear `data/llm/cache/` and `data/alphavantage/` at any time; stale keys are invalidated by TTL or by the key shape changing on upgrade.
