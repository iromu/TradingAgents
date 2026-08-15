## 1. Unbreak tests

- [x] 1.1 Read `TraderAgentConfig` record to confirm all 16 field names and types
- [x] 1.2 Fix `DebateLoopAgentTest.java` — update `TraderAgentConfig` constructor call from 15 to 16 args (add `researchTokenBudget` at position 9)
- [x] 1.3 Fix `TraderAgentConfigTest.java` — update `makeConfig()` helper to pass 16 args
- [x] 1.4 Fix `DebateLoopAgentIntegrationTest.java` — update all 5 test methods (lines 66, 102, 137, 169, 201) to pass 16 args
- [x] 1.5 Run `./mvnw test-compile` to verify all tests compile
- [x] 1.6 Run `./mvnw verify` to confirm all tests pass (594 tests, 0 failures — TemplateParsingTest fixed with mock _csrf)

## 2. Security hardening

- [x] 2.1 Add `spring-boot-starter-security` dependency to `pom.xml` (already present)
- [x] 2.2 Create `SecurityConfig.java` with `SecurityFilterChain` bean: permit all GET, require CSRF on POST (already exists)
- [x] 2.3 Create `CsrfTokenCookieServletFilter` bean to expose CSRF token in cookie for HTMX (already via `CookieCsrfTokenRepository.withHttpOnlyFalse()`)
- [x] 2.4 Update HTMX form templates to include CSRF token hidden input (all 4 templates already have `_csrf` inputs)
- [x] 2.5 Add feedback length validation (max 10,000 chars) in `TradingApiController` (already present)
- [x] 2.6 Add feedback length validation in `TradingHtmxController` (already present)
- [x] 2.7 Add feedback length validation in `ProcessStatusController` (already present)
- [x] 2.8 Sanitize flash messages in `TradingHtmxController` (already uses `HtmlUtils.htmlEscape()`)
- [x] 2.9 Add process ownership verification in `HitlService` (bind `processId` to HTTP session on creation, verify on POST endpoints)
- [x] 2.10 Test CSRF endpoints — TemplateParsingTest now passes with mock `_csrf` token

## 3. Input validation & null safety

- [x] 3.1 Add null `briefs` validation throwing `IllegalArgumentException` in `DebateLoopAgent.debate()` (review fix: replaced `Objects.requireNonNull` NPE with explicit IAE per spec)
- [x] 3.2 Add null/blank validation on `ticker` and `researchPlan` in `Trader.traderProposal()` (already present)
- [x] 3.3 Add null-safe cast in `CheckpointStore.restore()` — use `instanceof String s ? s : null` (already present in `getCheckpoint()`)
- [x] 3.4 Add `@Valid` annotations on `TraderAgentConfig` nested provider config properties (already present)
- [x] 3.5 Add `@Validated` annotation on `TraderAgentConfig` class (already present)

## 4. DebateLoopAgent hardening

- [x] 4.1 Add `maxRiskDebateRounds` field to `TraderAgentConfig` record with default value 3 (already exists from prior work)
- [x] 4.2 Inject `TraderAgentConfig` into `RiskDebateAgent` and replace `MAX_RISK_DEBATE_ROUNDS` constant with config value (already done, tests confirm)
- [x] 4.3 Update `DebateLoopAgent` cache keys from `{ticker}_debate_{count}_bull` to `debate:{ticker}:bull:{count}` format (already done)
- [x] 4.4 Cache `computeSimilarity()` result in local variable and reuse for logging and convergence check (already done)
- [x] 4.5 Improve `extractRating()` in `DebateAgent` to check for contextual cues when both "buy" and "sell" are present
- [x] 4.6 Improve `extractThesis()` in `DebateAgent` with fallback to first paragraph when no keyword found
- [x] 4.7 Fix `CODE_FENCE_UNCLOSED` regex to use explicit non-greedy: `Pattern.compile("(?s)\\\`\`\`.*?$", Pattern.MULTILINE)` → verify non-greedy behavior

## 5. AOT hints

- [x] 5.1 Add `TraderProposalOutput.class` to `@RegisterReflectionForBinding` in `TraderAgentRuntimeHintsRegistrar`
- [x] 5.2 Add `PortfolioDecisionOutput.class` to `@RegisterReflectionForBinding`
- [x] 5.3 Add `BudgetExceededException.class` to `@RegisterReflectionForBinding`
- [x] 5.4 Add `RiskAssessmentOutput.class` to `@RegisterReflectionForBinding`
- [x] 5.5 Add `SubtractIndicator.class` to `@RegisterReflectionForBinding` (or register via `TraderAgentRuntimeHintsRegistrar`)

## 6. Data quality & miscellaneous

- [x] 6.1 Fix `YFinService` NaN propagation — replace `Double.NaN` with `null` in `DecimalNum.valueOf()` calls (lines 163-167)
- [x] 6.2 Ensure `BudgetExceededException` is immutable (spec updated: Java records cannot extend RuntimeException; implemented as immutable class with final fields + getters)
- [x] 6.3 Add `@ConditionalOnProperty(name = "app.tools.fundamental.enabled", matchIfMissing = true)` to `FundamentalDataTools`
- [x] 6.4 Replace `Thread.sleep(backoff)` in `InstrumentIdentityAgent.fetchWithRetry()` with `TimeUnit.MILLISECONDS.sleep()` (full async rewrite out of scope)
- [x] 6.5 Fix `PolymarketService` fallback probability — return `null` instead of `"N/A"` string when probability/price missing (review fix: also fixed `formatMarketDetail` path)
- [x] 6.6 Create `CheckpointData` record and convert `CheckpointAgent.restoreCheckpoint()` return type from `Map<String, Object>` to `CheckpointData`
- [x] 6.7 Fix `HitlService.evictIfFull()` — when map is full and no sessions expired, evict LRU session instead of doing nothing
- [x] 6.8 Declare `ScheduledExecutorService` as `@Bean` in `HitlConfig` and inject into `HitlService` (replace `Executors.newSingleThreadScheduledExecutor()`)
- [x] 6.9 Remove dead code in `YFinService` (unused single-indicator wrapper method confirmed dead)

## 7. Verification

- [x] 7.1 Run `./mvnw verify` to confirm all tests pass (594 tests, 0 failures)
- [x] 7.2 Run `./mvnw compile` to check for any compilation warnings (clean)
- [ ] 7.3 Verify native image build includes new AOT hints (requires manual native build)
- [ ] 7.4 Test HTMX workflow end-to-end with CSRF protection enabled (requires manual browser testing)
