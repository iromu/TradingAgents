## ADDED Requirements

### Requirement: Cache keys include all query parameters
The system SHALL include all relevant query parameters in cache keys for every Alpha Vantage API method to prevent stale data from being served across different queries.

#### Scenario: Global news cache key includes date range
- **WHEN** `getGlobalNews()` is called with `dateFrom=2026-01-01` and `dateTo=2026-01-31`
- **THEN** the cache key includes both date parameters (e.g., `TICKER_global_news_2026-01-01_2026-01-31`)
- **AND** a subsequent call with different dates produces a different cache key

#### Scenario: Insider sentiment cache key includes query parameters
- **WHEN** `getInsiderSentiment()` is called with any query parameters
- **THEN** the cache key includes those parameters
- **AND** different parameter values produce different cache entries

#### Scenario: Existing methods retain correct cache keys
- **WHEN** methods that already had correct cache keys (e.g., `getOverview`, `getBalanceSheet`) are called
- **THEN** their cache keys remain unchanged and correct
- **AND** no regression in caching behavior occurs

### Requirement: RestTemplate has connect and read timeouts
The system SHALL configure both connect timeout and read timeout on the RestTemplate used by `AlphaVantageService` to prevent indefinite blocking.

#### Scenario: Connect timeout is configurable
- **WHEN** `AlphaVantageService` creates its RestTemplate
- **THEN** the connect timeout is set to the value of `app.alphavantage.connect-timeout-ms` (default: 10000ms)
- **AND** the timeout is configurable via application properties

#### Scenario: Read timeout is configured
- **WHEN** `AlphaVantageService` creates its RestTemplate
- **THEN** the read timeout is set to a configurable value (default: 30000ms)
- **AND** the timeout prevents indefinite blocking on slow API responses

#### Scenario: Timeout exceptions are handled gracefully
- **WHEN** a RestTemplate call times out
- **THEN** a descriptive exception is thrown (not a silent hang)
- **AND** the exception message indicates which endpoint timed out

### Requirement: FileCache prevents concurrent duplicate computation
The system SHALL prevent multiple threads from computing the same cache entry simultaneously by using proper locking in `FileCache.getOrCompute()`.

#### Scenario: Concurrent requests for same key compute once
- **WHEN** two threads call `getOrCompute()` with the same key simultaneously
- **THEN** the supplier function is executed exactly once
- **AND** both threads receive the result of that single computation

#### Scenario: Different keys compute independently
- **WHEN** two threads call `getOrCompute()` with different keys simultaneously
- **THEN** both supplier functions execute independently
- **AND** both results are cached correctly

#### Scenario: Lock is released on exception
- **WHEN** the supplier function throws an exception while holding the write lock
- **THEN** the write lock is released so subsequent calls can proceed
- **AND** the cache does not remain locked indefinitely
