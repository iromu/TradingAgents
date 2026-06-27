# Spec: Startup Reliability

## Requirements

### SR-1: Alpha Vantage Service Must Not Block Application Startup

**Current behavior:** `AlphaVantageService.validateApiKey()` throws `IllegalStateException` at `@PostConstruct` if the API key is `"dummy_key"` (the default). This prevents the entire Spring application from starting.

**Required behavior:** `AlphaVantageService` must be conditionally loaded. When the API key is not configured:
- Log a warning at startup
- Return `"NO_DATA_AVAILABLE"` from all methods (matching `FredService` behavior)
- Do NOT throw an exception

**Implementation:**
- Add `@ConditionalOnProperty(prefix = "app.alphavantage", name = "api-key", havingValue = "dummy_key", matchIfMissing = false)` — or better, use a separate property `app.alphavantage.enabled`
- When disabled, all methods return `"NO_DATA_AVAILABLE"` instead of making HTTP calls
- `FredService` and `PolymarketService` already follow this pattern — `AlphaVantageService` should match

### SR-2: Decision Memory Repository Must Have a Default Max Entries

**Current behavior:** `app.memory.log-max-entries: 0` means unlimited. The memory log file grows indefinitely.

**Required behavior:** Default max entries should be 1000 (configurable). When the limit is reached, `rotate()` prunes oldest resolved entries while keeping pending entries intact.

**Implementation:**
- Change default from `0` to `1000` in `application.yaml`
- Document that `0` means "unlimited" (existing behavior for users who want it)

### SR-3: HitlService Executor Must Be a Daemon Thread

**Current behavior:** `Executors.newSingleThreadScheduledExecutor()` creates a non-daemon thread. If Spring shuts down without calling `destroy()`, the JVM hangs.

**Required behavior:** Use `Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; })` to allow clean JVM shutdown.