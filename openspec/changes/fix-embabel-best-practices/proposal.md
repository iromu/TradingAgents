# Proposal: Fix Embabel Best Practices Violations

## Why

An audit of the TradingAgents codebase against Embabel v0.5.0 best practices revealed one critical functional bug (broken Jinja variable in RiskManager prompt), several architectural concerns (dead fields, startup-blocking validation), and multiple medium/low severity issues. The critical bug degrades the risk debate output quality by rendering a variable as literal text. The startup failure blocks the entire application when Alpha Vantage isn't configured, even though not all agents need it. These issues need fixing before production deployment.

## What Changes

### Critical Fixes
1. **Fix Jinja variable syntax** in `RiskManager.jinja` — `{trader_decision}` → `{{ trader_decision }}`
2. **Remove dead fields** from `InvestmentDebateState` — 5 unused fields from Python port leftover
3. **Make Alpha Vantage validation graceful** — change from `IllegalStateException` at startup to `log.warn()` + fail-on-first-call

### Architectural Improvements
4. **Add `.withId()` to all LLM calls** — verify completeness (audit found all have `.withId()`)
5. **Use `StringBuilder` in `RiskDebateAgent`** debate history concatenation
6. **Use `BooleanStringUtils` or explicit check** for approval boolean parsing

### Medium Improvements
7. **Extract shared `ResearchPlanService`** from controller duplication
8. **Add `@ConditionalOnProperty`** to `AlphaVantageService` so it doesn't block startup
9. **Document ReDoS risk** in `sanitizeForPrompt()` — pre-compile regex patterns
10. **Add default `maxEntries`** to `DecisionMemoryRepository` (e.g., 1000)

### Low Priority
11. **Migrate `FileCache` to `java.nio.file.Path`** for consistency
12. **Add connection pooling** to `RestTemplate` instances
13. **Make `HitlService` executor a daemon thread**

## Scope

- **Modified:**
  - `src/main/resources/prompts/managers/RiskManager.jinja`
  - `src/main/java/com/embabel/gekko/domain/ResearchTypes.java`
  - `src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java`
  - `src/main/java/com/embabel/gekko/agent/RiskDebateAgent.java`
  - `src/main/java/com/embabel/gekko/web/TradingHtmxController.java`
  - `src/main/java/com/embabel/gekko/web/TradingApiController.java`
  - `src/main/java/com/embabel/gekko/util/AgentUtils.java`
  - `src/main/java/com/embabel/gekko/util/FileCache.java`
  - `src/main/java/com/embabel/gekko/agent/memory/DecisionMemoryRepository.java`
  - `src/main/java/com/embabel/gekko/htmx/HitlService.java`
  - `src/main/resources/application.yaml`
- **New:**
  - `src/main/java/com/embabel/gekko/web/ResearchPlanService.java`
- **Removed:**
  - Dead fields from `InvestmentDebateState`

## Success Criteria

- [ ] RiskManager.jinja renders trader_decision correctly (verified by reading prompt output)
- [ ] Application starts without Alpha Vantage API key configured (warns, doesn't fail)
- [ ] InvestmentDebateState has 7 fields instead of 12 (dead fields removed)
- [ ] All tests pass
- [ ] No regressions in existing agent behavior

## Risks

- Removing dead fields from `InvestmentDebateState` could break serialization if cached data exists on disk. Migration path: keep fields but mark as deprecated, or add a versioned cache migration.
- Extracting `ResearchPlanService` introduces a new dependency between controllers — need to ensure no circular dependencies.
- Making Alpha Vantage optional means some agent actions (fundamentals, news, insider data) will fail gracefully but others (market analysis via Yahoo Finance) will still work.