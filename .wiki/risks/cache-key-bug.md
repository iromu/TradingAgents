---
title: "Cache Key Bug"
type: "risk"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java"
updated_at: "2026-06-11"
---

# Alpha Vantage Cache Key Bug

## The Problem

Several methods in `AlphaVantageService` use simplified cache keys that ignore important query parameters:

### `getNews()` — Date range ignored

```java
public String getNews(String ticker, String startDate, String endDate) {
    String cacheKey = String.format("%s_NEWS_%s_%s", ticker, startDate, endDate);
    return getDataWithCache("NEWS_SENTIMENT", cacheKey, ...);
}
```

This method **does** include the date range in the cache key — so it's actually correct.

### `getGlobalNews()` — Topic and limit ignored

```java
public String getGlobalNews(String topic, Integer limit, Integer page) {
    String cacheKey = "GLOBAL_NEWS";  // ← ignores topic, limit, page
    return getDataWithCache("NEWS_SENTIMENT", cacheKey, ...);
}
```

All calls to `getGlobalNews()` return the same cached result, regardless of topic, limit, or page.

### `getInsiderSentiment()` — Interval ignored

```java
public String getInsiderSentiment(String ticker, String interval) {
    String cacheKey = String.format("%s_INSIDER_SENTIMENT", ticker);  // ← ignores interval
    return getDataWithCache("INSIDER_SENTIMENT", cacheKey, ...);
}
```

Different intervals for the same ticker return the same cached result.

## Impact

- **Stale data** — users may see outdated news or insider sentiment
- **Incorrect analysis** — analysts may base reports on cached data from different time periods
- **No cache invalidation** — there's no TTL or invalidation mechanism

## Fix

Include all relevant parameters in the cache key:

```java
// getGlobalNews
String cacheKey = String.format("GLOBAL_NEWS_%s_%d_%d", topic, limit, page);

// getInsiderSentiment
String cacheKey = String.format("%s_INSIDER_SENTIMENT_%s", ticker, interval);
```
