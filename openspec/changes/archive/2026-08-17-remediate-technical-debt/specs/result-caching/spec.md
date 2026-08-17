## Purpose

A single result-caching contract for the trading pipeline so that LLM-generated and external-HTTP results are cached correctly — keys cover every result-affecting input, error payloads are never cached, and stale results expire.

## ADDED Requirements

### Requirement: Cache keys cover all result-affecting inputs

The system SHALL derive every result-cache key from ALL inputs that affect the cached result, so that a change to any input produces a different key and never serves a stale cached result. This applies to LLM-generated results (debate reports, the research plan, the final investment plan) and to external-HTTP results (market data).

#### Scenario: Final plan cache key changes with risk assessment
- **WHEN** the final decision step is invoked for the same ticker with a different risk assessment
- **THEN** the cache key differs from the prior invocation
- **AND** a new result is generated rather than a stale cached plan being served

#### Scenario: Research plan cache key includes identity
- **WHEN** the research plan is generated for a ticker whose resolved identity changed since the prior plan
- **THEN** the cache key reflects the identity inputs
- **AND** the prior plan is not served for the new identity

#### Scenario: Report cache key includes iteration and inputs
- **WHEN** a debate report is generated at a given iteration with a given input set
- **THEN** the cache key includes the ticker, the iteration, and the report-affecting inputs
- **AND** a distinct input set produces a distinct key

### Requirement: Cache keys are case-normalized

The system SHALL normalize ticker and other symbol inputs to a canonical case before computing any cache key, so that case variations of the same symbol map to the same cache entry.

#### Scenario: Same ticker in different cases hits the same entry
- **WHEN** a result is requested for `nvda` and then for `NVDA`
- **THEN** both requests resolve to the same cache entry
- **AND** no second fetch or LLM call is made for the second request

### Requirement: Error and rate-limit payloads are not cached

The system MUST NOT persist error responses, rate-limit responses, or empty/failed payloads to the result cache. A failed fetch SHALL be retried on the next request rather than served from cache, while a successful result SHALL be cached.

#### Scenario: Rate-limit response is not cached
- **WHEN** an external data call returns a rate-limit or error payload
- **THEN** that payload is not written to the cache
- **AND** the next request retries the external call

#### Scenario: Successful response is cached
- **WHEN** an external data call returns a valid result
- **THEN** the result is written to the cache
- **AND** a subsequent identical request is served from the cache

### Requirement: Time-sensitive results carry a TTL

The system SHALL apply a time-to-live (TTL) or eviction policy to time-sensitive results (e.g. market quotes) so that a cached value older than its TTL is treated as a miss and refetched. The TTL SHALL be configurable per result category.

#### Scenario: Stale quote is refetched
- **WHEN** a cached quote is older than its configured TTL
- **THEN** the cache is treated as a miss
- **AND** a fresh value is fetched and cached

#### Scenario: Fresh quote is served from cache
- **WHEN** a cached quote is within its configured TTL
- **THEN** the cached value is served without a fresh fetch
