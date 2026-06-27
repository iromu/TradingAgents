# Spec: Startup Reliability

## Purpose

Ensure the application starts reliably even when optional data sources are not configured, and that background services shut down cleanly.

## Requirements

### SR-1: Alpha Vantage Service Must Not Block Application Startup

The system SHALL conditionally load `AlphaVantageService` so that missing API keys do not prevent startup.

When the Alpha Vantage API key is not configured or the service is disabled:
- Log a warning at startup
- Return `"NO_DATA_AVAILABLE"` from all methods (matching `FredService` behavior)
- Do NOT throw an exception

**Implementation:**
- Add `@ConditionalOnProperty(prefix = "app.alphavantage", name = "enabled", havingValue = "true", matchIfMissing = false)`
- When disabled, all methods return `"NO_DATA_AVAILABLE"` instead of making HTTP calls
- `FredService` and `PolymarketService` already follow this pattern — `AlphaVantageService` should match

#### Scenario: Service disabled by config
- **WHEN** `app.alphavantage.enabled` is `false` or not set
- **THEN** `AlphaVantageService` is not created by Spring

#### Scenario: Service enabled with valid key
- **WHEN** `app.alphavantage.enabled` is `true` and a valid API key is configured
- **THEN** `AlphaVantageService` is created and makes HTTP calls normally

#### Scenario: Service enabled without API key
- **WHEN** `app.alphavantage.enabled` is `true` but no API key is configured
- **THEN** the service uses a dummy key and returns `"NO_DATA_AVAILABLE"` gracefully

### SR-2: Decision Memory Repository Must Have a Default Max Entries

The system SHALL limit the number of resolved entries in the decision memory log to prevent unbounded file growth.

The default max entries SHALL be 1000 (configurable). When the limit is reached, `rotate()` prunes oldest resolved entries while keeping pending entries intact.

**Implementation:**
- Change default from `0` to `1000` in `application.yaml`
- Document that `0` means "unlimited" (existing behavior for users who want it)

#### Scenario: Default max entries applied
- **WHEN** `app.memory.log-max-entries` is not set (defaults to 1000)
- **THEN** the system prunes oldest resolved entries when the count exceeds 1000

#### Scenario: Unlimited entries
- **WHEN** `app.memory.log-max-entries` is set to `0`
- **THEN** the system does not prune resolved entries (unlimited)

#### Scenario: Pending entries never pruned
- **WHEN** the memory log has both pending and resolved entries
- **THEN** only resolved entries are pruned; pending entries are preserved

### SR-3: HitlService Executor Must Be a Daemon Thread

The system SHALL use daemon threads for background executors to allow clean JVM shutdown.

**Implementation:**
- Use `Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; })`
- The executor's `destroy()` method SHALL still be called on Spring shutdown

#### Scenario: Daemon thread created
- **WHEN** `HitlService` is instantiated
- **THEN** the scheduled executor uses daemon threads

#### Scenario: Clean shutdown
- **WHEN** Spring context closes without calling `destroy()`
- **THEN** the JVM exits cleanly (daemon threads do not block shutdown)