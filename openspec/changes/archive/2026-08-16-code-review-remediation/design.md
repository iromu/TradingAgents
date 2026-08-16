## Context

The TradingAgents codebase has accumulated technical debt from rapid feature development. The code review identified issues across test infrastructure, security, input validation, debate logic, AOT hints, and data quality. See proposal.md for motivation and scope.

Current state:
- `TraderAgentConfig` is a 16-field record, but tests still use 13-field constructor calls
- No Spring Security dependency; all endpoints are unprotected
- `RiskDebateAgent` hardcodes `MAX_RISK_DEBATE_ROUNDS = 3`
- `DebateLoopAgent` computes similarity twice per iteration and uses collision-prone cache keys
- Missing AOT hints for 5 classes used in structured output
- `YFinService` passes `Double.NaN` to `DecimalNum` on null data

## Goals / Non-Goals

**Goals:**
- Unbreak test compilation by fixing constructor signatures
- Add minimal security (CSRF + input validation) without breaking existing HTMX workflow
- Harden debate loop against null inputs and cache key collisions
- Make risk debate rounds configurable
- Register all structured output types for AOT
- Fix data quality issues (NaN, record conversion, typed returns)

**Non-Goals:**
- Full authentication/authorization system (out of scope — this is minimal CSRF + input validation)
- VendorRouter refactor (string dispatch is a refactor, not a bugfix)
- ASCII art removal (cosmetic)
- Convergence algorithm improvement (character bigrams are a known limitation, not a bug)
- ActionContext state leakage investigation (framework-level concern)

## Decisions

### Decision 1: Minimal Spring Security configuration
**Choice:** Add `spring-boot-starter-security` with a minimal `SecurityFilterChain` that enables CSRF and permits all GET requests, requires CSRF token on POST.

**Rationale:** The current system has no auth at all. Adding full auth is a separate concern. CSRF protection is the minimum viable security for state-changing endpoints. The HTMX templates already support hidden form fields, so adding CSRF token inputs is straightforward.

**Alternatives considered:**
- No security (current state) — unacceptable risk
- Full session-based auth — too much scope for this change
- API key auth — not appropriate for HITL workflow

### Decision 2: Process ownership via HTTP Session
**Choice:** Store `processId` → `sessionId` mapping in `HitlService` and verify on subsequent requests.

**Rationale:** The simplest ownership model is binding a process to the HTTP session that created it. This prevents cross-session process manipulation without requiring full authentication.

**Alternatives considered:**
- Token-based ownership — requires token generation and storage
- Database-backed ownership — over-engineered for current scope

### Decision 3: Configurable risk debate rounds via TraderAgentConfig
**Choice:** Add `maxRiskDebateRounds` field to `TraderAgentConfig` with default 3, inject into `RiskDebateAgent`.

**Rationale:** Consistent with existing pattern (`maxDebateIterations`, `similarityThreshold` already configurable). Minimal code change — replace `private static final` with config injection.

### Decision 4: Cache key namespace with colon delimiter
**Choice:** Change cache keys from `{ticker}_debate_{count}_bull` to `debate:{ticker}:bull:{count}`.

**Rationale:** Colon is not a valid character in ticker symbols, preventing collision. The namespace prefix (`debate:`) makes the key purpose clear. Consistent with Redis key naming conventions.

### Decision 5: Similarity caching via local variable
**Choice:** Store similarity result in a local variable before the `repeating()` block and reuse in `until()`.

**Rationale:** The similarity is computed on the same inputs in both places. Storing in a local variable eliminates duplicate computation without changing the loop structure.

**Alternatives considered:**
- Extract to a separate method — adds indirection for a simple fix
- Use a map to cache results — over-engineered for two calls

### Decision 6: BudgetExceededException as record
**Choice:** Convert from `class BudgetExceededException extends RuntimeException { private final int budget; ... }` to `record BudgetExceededException(int budget, String message) extends RuntimeException`.

**Rationale:** Records are the idiomatic way to model immutable data in Java 14+. The exception carries immutable state (budget amount, message), making it a perfect record candidate.

### Decision 7: YFinService null handling
**Choice:** Replace `DecimalNum.valueOf(h.getOpen() == null ? Double.NaN : h.getOpen().doubleValue())` with `Optional.ofNullable(h.getOpen()).map(Ohlc::doubleValue).map(DecimalNum::valueOf).orElse(null)`.

**Rationale:** TA4J's `DecimalNum` does not handle NaN well. Using `null` allows downstream code to handle missing data explicitly.

**Alternatives considered:**
- Use `DecimalNum.ZERO` as sentinel — loses the distinction between "zero" and "missing"
- Keep NaN but document it — doesn't fix the underlying issue

### Decision 8: HitlService executor as Spring bean
**Choice:** Declare `ScheduledExecutorService` as a `@Bean` in `HitlConfig` and inject into `HitlService`.

**Rationale:** Spring-managed beans get proper lifecycle handling (shutdown on context close). The current `Executors.newSingleThreadScheduledExecutor()` is never shut down.

## Risks / Trade-offs

### Risk: CSRF tokens break existing HTMX workflow
**Mitigation:** Thymeleaf's Spring Security integration automatically provides CSRF tokens via `${_csrf.token}` and `${_csrf.parameterName}`. Add hidden inputs to all POST forms. Test all HTMX endpoints after the change.

### Risk: Process ownership breaks multi-tab usage
**Mitigation:** If a user opens multiple tabs, each tab has its own session. The process is bound to the creating session. This is acceptable — the user should use one tab per process.

### Risk: Cache key format change invalidates existing cache
**Mitigation:** The old cache keys will become orphaned but harmless. New keys will be generated on next run. Acceptable one-time cache invalidation.

### Risk: NaN → null change breaks TA4J indicator calculations
**Mitigation:** TA4J's `BarSeries` handles null values gracefully (skips them). Test indicator calculations with null data to verify.

### Risk: Spring Security dependency increases build size
**Mitigation:** The dependency is ~2MB. Negligible for a Spring Boot application. The security benefit far outweighs the size cost.

## Migration Plan

### Phase 1: Test fixes (no behavioral change)
1. Update constructor calls in 3 test files
2. Run `./mvnw verify` to confirm all tests pass

### Phase 2: Security (requires testing)
1. Add `spring-boot-starter-security` dependency
2. Create `SecurityConfig.java` with CSRF + session ownership
3. Update HTMX templates with CSRF token inputs
4. Test all POST endpoints

### Phase 3: Input validation (low risk)
1. Add null checks in `DebateLoopAgent`, `Trader`, `CheckpointStore`
2. Add `@Valid` on `TraderAgentConfig` nested properties
3. Validate feedback length in controllers

### Phase 4: Debate hardening (medium risk)
1. Add `maxRiskDebateRounds` to config
2. Fix cache key format
3. Cache similarity computation
4. Improve rating/thesis extraction

### Phase 5: AOT hints (no runtime change)
1. Register missing types in `TraderAgentRuntimeHintsRegistrar`
2. Verify native image build includes new hints

### Phase 6: Data quality (low risk)
1. Fix NaN handling in `YFinService`
2. Convert `BudgetExceededException` to record
3. Add toggle to `FundamentalDataTools`
4. Replace `Thread.sleep()` in `InstrumentIdentityAgent`
5. Fix `PolymarketService` fallback
6. Type `CheckpointAgent` return
7. Fix `HitlService` eviction and executor

### Rollback strategy
Each phase is independent and can be rolled back individually. The only phase with significant rollback risk is Phase 2 (security) — if CSRF breaks the HTMX workflow, revert the security dependency and template changes.
