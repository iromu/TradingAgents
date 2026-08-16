## 1. Shared foundations (D4, D5)

- [ ] 1.1 Add a shared `PromptSanitizer` util exposing `sanitizeForPrompt(String)` that neutralizes Jinja syntax (`{{ }}`/`{% %}`), HTML/script content, and control characters; add unit tests (security spec: Shared prompt-input sanitization)
- [ ] 1.2 Add a `ResultCache` contract over the existing disk `FileCache`: case-normalized symbol keys, an error/rate-limit/empty payload guard (never persisted), and a per-category TTL (result-caching spec)
- [ ] 1.3 Add `ResultCache` unit tests: key completeness, case normalization, error payloads not cached, TTL expiry (result-caching spec)

## 2. Final-step funnel (A) (agent-orchestration spec)

- [ ] 2.1 Set `past_memory_str` from real `generatePastContext(ticker)` in `buildResearchManagerModel` (replace the hard-coded `NO_PAST_MEMORY`)
- [ ] 2.2 Ensure `portfolio_decision`, `ticker`, and identity fields (company name, sector, industry, exchange) are set in the `researchManager` model and sanitized via `PromptSanitizer`
- [ ] 2.3 Add additive, guarded sections/slots to `managers/ResearchManager.jinja` for risk (level + recommendation + reasoning), portfolio decision, ticker, and identity; a failed identity degrades to a placeholder (does not fail)
- [ ] 2.4 Rebuild the `research_manager` cache key to include ticker + identity + risk + feedback + approval via `ResultCache` (currently `ticker + "_research_manager"`)
- [ ] 2.5 Rebuild the research-plan cache key to include identity via `ResultCache`
- [ ] 2.6 Update tests asserting `past_memory_str == "No past memories found."` and the old key shape; add tests that the final prompt includes risk, portfolio, and identity (agent-orchestration spec)
- [ ] 2.7 Make the risk-debate step robust to structured-output failure: do not classify RISKY by substring keyword matching on the transcript (which always contains the "Aggressive"/"Conservative" speaker labels); re-request structured output or record an explicit undetermined/neutral level (agent-orchestration spec)

## 3. Decision-memory loop (B) (decision-memory spec)

- [ ] 3.1 Make `storeFinalDecision` write the real `TradingDate` (a valid `YYYY-MM-DD`), never the `"auto"` placeholder, so the pending-match regex succeeds
- [ ] 3.2 Wire a resolution step into the pipeline that runs for the current ticker (guarded by `hasPendingEntriesFor(ticker)`) after the ticker is on the blackboard and before the research plan
- [ ] 3.3 Make `fetchReturns` compute alpha against a real benchmark's same-window return (default `SPY`, configurable); on missing benchmark data, fall back to the raw return with an explicit benchmark-less marker (never silent 0)
- [ ] 3.4 Sanitize and structurally validate the LLM reflection via `PromptSanitizer` before it is persisted to the memory log
- [ ] 3.5 Standardize the memory entry separator: write the canonical `<!-- ENTRY_END -->`, and have the parser also tolerate the legacy control-char form so existing files keep parsing; ensure write/read round-trip
- [ ] 3.6 Update decision-memory tests: matchable pending date, resolution actually triggered, real past context in the final prompt, alpha vs benchmark, separator round-trip (decision-memory spec)
- [ ] 3.7 Guard `fetchReturns` against incomplete windows: validate the end-date close exists before recording a return; on a missing close or a data-source error string, leave the entry pending or mark it unresolvable (never a fabricated 0%/0 alpha) (decision-memory spec)

## 4. Caching consolidation (C) (result-caching spec)

- [ ] 4.1 Migrate the existing `FileCache.getOrCompute` LLM call sites onto `ResultCache` with case-normalized, input-complete keys
- [ ] 4.2 Migrate `AlphaVantageService`'s ad-hoc cache onto `ResultCache` (all 8 endpoints, case-normalized keys, error guard); remove the dead `currDate` param and the plaintext key layout
- [ ] 4.3 Move the unlocked manual `FileCache` identity-cache site onto the locked `ResultCache` contract
- [ ] 4.4 Configure a TTL for time-sensitive (quote) result categories in `application.yaml`
- [ ] 4.5 Add tests: error/rate-limit payloads not cached, TTL expiry, and AlphaVantage cache-key completeness (result-caching spec)

## 5. Sanitization breadth + REST validation (D) (security + instrument-identity specs)

- [ ] 5.1 Apply `PromptSanitizer` at every prompt-building site (orchestrator plan model, `InstrumentContextPromptContributor`, risk debators, tool/data output) and replace `DebateAgent`'s private `sanitizeValue`/`sanitizeForPrompt` with delegation to it
- [ ] 5.2 Sanitize resolved identity metadata before it is written to the on-disk identity cache (cache-poisoning defense) (instrument-identity spec)
- [ ] 5.3 Enforce the HTMX ticker format (`^[A-Z0-9.]+$`) on the REST `/api/trading/*` path and reject invalid tickers with 400 (security spec: REST ticker path validation)
- [ ] 5.4 Add tests: each prompt-building site is sanitized, identity is sanitized before caching, and the REST path rejects an invalid ticker
- [ ] 5.5 Apply the ticker format validation to the HITL resubmit path so a raw resubmitted value is not injected into the pipeline as a ticker (security spec: REST ticker path validation)

## 6. Scaffolding + config (E) — behavior-neutral, no spec delta

- [ ] 6.1 Extract a `ReportGenerator` template method the four analyst-report actions delegate to (common prompt-build → cache → sanitize → parse); lock behavior with existing tests
- [ ] 6.2 Extract a shared risk-debator base for the three debators (common debate mechanics); lock behavior with existing tests
- [ ] 6.3 Extract a shared data-service base for the two sibling data services; lock behavior with existing tests
- [ ] 6.4 Make the model-role → model mapping explicit and validated in `TraderAgentConfig` (log a warning when `cheapest`/`best`/`default-llm` collapse to one model; support per-role overrides) — no behavior change
- [ ] 6.5 Document the `app.checkpoint.enabled=false` default as an intentional override (do not flip it in this change)

## 7. Verification

- [ ] 7.1 Run `./mvnw verify` and confirm the full build + test suite is green
- [ ] 7.2 Update the `.wiki` pages whose behavior changed (funnel inputs, memory loop, caching contract, sanitization) and run `ingest wiki`
- [ ] 7.3 Document that `data/llm/cache` and `data/alphavantage` may be safely cleared after deployment (stale-key invalidation)
