# Reliability Fixes

## Purpose

Improve the reliability and robustness of the TradingAgents system by fixing cache key bugs, adding HTTP timeouts, and preventing concurrent duplicate computation in the caching layer.

## Requirements

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

### Requirement: YFinService handles null values without NaN
The system SHALL handle null values from Yahoo Finance data without propagating `Double.NaN` to `DecimalNum` values.

#### Scenario: Null open price handled
- **WHEN** `YFinService` processes historical data with a null `getOpen()` value
- **THEN** the system uses `null` or a sentinel value instead of `Double.NaN`
- **AND** the `DecimalNum` value does not contain NaN

#### Scenario: NaN values do not propagate to indicators
- **WHEN** indicator calculations receive data with missing values
- **THEN** the indicators handle the missing values gracefully
- **AND** no NaN values propagate through the indicator chain

### Requirement: BudgetExceededException is immutable
The system SHALL ensure `BudgetExceededException` is immutable: all fields are `final`, exposed via getters only, with no setters. (Note: Java records cannot extend `RuntimeException`, so this is an immutable class, not a record.)

#### Scenario: BudgetExceededException is immutable
- **WHEN** `BudgetExceededException` is instantiated
- **THEN** all fields (`ticker`, `callCount`, `budget`) are final
- **AND** access is via getter methods only
- **AND** no setters or mutation methods exist

### Requirement: FundamentalDataTools has enable/disable toggle
The system SHALL allow `FundamentalDataTools` to be enabled or disabled via configuration, matching the pattern used by `FredDataTools` and `PolymarketDataTools`.

#### Scenario: FundamentalDataTools disabled by configuration
- **WHEN** `app.tools.fundamental.enabled` is set to `false`
- **THEN** the `FundamentalDataTools` bean is not created
- **AND** fundamental data tools are not available to LLM calls

#### Scenario: FundamentalDataTools enabled by default
- **WHEN** no configuration is provided for `app.tools.fundamental.enabled`
- **THEN** the `FundamentalDataTools` bean is created
- **AND** fundamental data tools are available

### Requirement: InstrumentIdentityAgent uses interruptible exponential backoff
The system SHALL replace bare `Thread.sleep()` calls in `InstrumentIdentityAgent` with interruptible `TimeUnit.MILLISECONDS.sleep()` and exponential backoff between retries. (Full async rewrite is out of scope for this change.)

#### Scenario: Retry uses exponential backoff
- **WHEN** `InstrumentIdentityAgent` needs to retry after a failure
- **THEN** the backoff doubles between attempts (e.g., 2s → 4s → 8s)
- **AND** sleep is interruptible via `TimeUnit.MILLISECONDS.sleep()`

#### Scenario: Retry respects thread interruption
- **WHEN** the thread is interrupted during backoff
- **THEN** the interruption is propagated (thread interrupt flag restored)
- **AND** a `RuntimeException` is thrown to terminate the retry loop

### Requirement: PolymarketService fallback probability is typed
The system SHALL return a properly typed fallback value when probability data is missing from Polymarket, instead of returning "N/A" as a string in a numeric field.

#### Scenario: Missing probability returns null
- **WHEN** Polymarket data is missing probability or price
- **THEN** the system returns `null` or a typed sentinel instead of the string "N/A"
- **AND** downstream consumers can check for null instead of parsing strings

### Requirement: CheckpointAgent returns typed record
The system SHALL convert `CheckpointAgent.restoreCheckpoint()` return type from `Map<String, Object>` to a typed record.

#### Scenario: Restore returns typed record
- **WHEN** `restoreCheckpoint()` is called
- **THEN** it returns a typed record (e.g., `CheckpointData`) instead of `Map<String, Object>`
- **AND** the record contains type-safe accessors for checkpoint fields

### Requirement: HitlService LRU eviction handles full map
The system SHALL evict the least recently used session when the session map is full and no sessions are expired, preventing unbounded growth.

#### Scenario: LRU eviction when no expired sessions
- **WHEN** the session map reaches `maxSessions` and no sessions are expired
- **THEN** the least recently used session is evicted
- **AND** the new session is added

#### Scenario: Expired sessions preferred for eviction
- **WHEN** the session map is full and some sessions are expired
- **THEN** an expired session is evicted (not an active session)
- **AND** the new session is added

### Requirement: HitlService executor is Spring-managed
The system SHALL manage the `HitlService` scheduled executor as a Spring bean for proper lifecycle management.

#### Scenario: Executor is Spring-managed
- **WHEN** the application context is refreshed
- **THEN** the scheduled executor is created as a Spring bean
- **AND** the executor is shut down when the context closes
