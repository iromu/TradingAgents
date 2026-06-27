# Spec: API Hardening

## Why
The REST API (`TradingApiController`) has no rate limiting, authentication, or input size limits. An attacker could flood the API with research requests, consuming LLM tokens. The ticker validation regex `^[A-Z0-9.]+$` allows arbitrarily long strings.

## What Changes
- Add input size limit to ticker field (max 20 characters — standard ticker length)
- Add rate limiting to `/api/plan` endpoint (configurable via `app.api.rate-limit-per-minute`)
- Add optional authentication guard (configurable via `app.api.auth.enabled`)
- Add `@ConditionalOnProperty` to `AlphaVantageService` so the app can start without an API key (graceful degradation, matching `FredService` behavior)

## Acceptance Criteria
- [ ] Ticker input is validated for max length (20 chars) in `TradingApiController.TickerRequest`
- [ ] Rate limiting is configurable and defaults to 10 requests/minute per IP
- [ ] Authentication is configurable and disabled by default (opt-in)
- [ ] `AlphaVantageService` uses `@ConditionalOnProperty(name = "app.alphavantage.required", havingValue = "true", matchIfMissing = false)` — app starts without API key
- [ ] `AlphaVantageService` methods return `"NO_DATA_AVAILABLE"` (matching `FredService`) when API key is missing, instead of throwing `IllegalStateException`
- [ ] Build passes (`./mvnw verify`)