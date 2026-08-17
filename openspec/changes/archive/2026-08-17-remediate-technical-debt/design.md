## Context

See `proposal.md` (Why) for motivation. The approach is shaped by five current-state facts verified against the code:

- The final decision step (`DebateAgent.researchManager` → `buildResearchManagerModel`) already *sets* most model variables (`risk_level`, `risk_reasoning`, `ticker`, `portfolio_decision`, `history`, `user_feedback`) but the `ResearchManager.jinja` template only renders `history`, `past_memory_str`, and `user_feedback`; and `past_memory_str` is hard-coded to `AgentUtils.NO_PAST_MEMORY`. The other stages' output is computed, then dropped.
- The decision-memory loop is open at four seams: `storeFinalDecision` writes a placeholder date (`"auto"`) that `hasPendingEntriesFor`'s regex never matches; `resolvePending`/`storeAndResolveWithReflection` have **no caller** (a guard on an action that nothing invokes never runs); only the research-plan step injects real `generatePastContext`; and the LLM reflection is the one place persisted text is unsanitized.
- Caching is fragmented across three strategies (locked `FileCache.getOrCompute` ×~10, unlocked manual `FileCache` get/save ×1, and AlphaVantage's ad-hoc plaintext cache ×8) with **no TTL** anywhere and two known keys that ignore inputs.
- Sanitization is a single method (`sanitizeValue`/`sanitizeForPrompt`) private to `DebateAgent`, applied to exactly three variables; every other prompt-building site and the identity cache write are raw.
- The memory file is Python-coupled: the `decision-memory` spec SHALLs "regex patterns matching the Python project's `_DECISION_RE` and `_REFLECTION_RE`," and the spec's documented separator (`<!-- ENTRY_END -->`) does not match the control-char separator the code currently writes — so read and write do not round-trip.

Constraints: Java 25 / Spring Boot 3.5 / Embabel 1.0.0; `./mvnw verify` is the gate; the memory file and cache dirs are durable on-disk state that other tools (the Python project, manual inspection) read; no new external dependencies are wanted for the cache fix.

## Goals / Non-Goals

**Goals:**
- Make the final `InvestmentPlan` prompt actually consume the risk assessment, portfolio decision, and identity (and real past context), with cache keys that reflect those inputs.
- Close the four memory-loop seams while keeping the on-disk format Python-compatible and self-round-tripping.
- One result-caching contract (complete + case-normalized keys, no error-payload caching, TTL) applied to LLM and external-HTTP results.
- One shared sanitizer applied at every prompt-building site and before any disk cache write.
- Behavior-neutral consolidation of the four report generators, three risk debators, and two sibling data services.

**Non-Goals:**
- No change to the debate algorithm (rounds, convergence, similarity) or the agent topology.
- No new cache library; no in-memory cache replacing the disk-backed one.
- No change to the multi-provider LLM abstraction (provider config stays as-is).
- Not a re-architecture: the `asSubProcess` boundary, HITL checkpoints, and prompt file layout are preserved.

## Decisions

### D1 — Funnel: render the already-computed inputs; make the key match the inputs
`researchManager` builds its prompt model in `buildResearchManagerModel`. Decision: keep building the model there, but (a) set `past_memory_str` from `generatePastContext(ticker)` instead of `NO_PAST_MEMORY`, (b) confirm `portfolio_decision`, `ticker`, and the identity fields (company name, sector, industry, exchange) are set and sanitized, and (c) add matching sections/slots to `ResearchManager.jinja` for risk (level + recommendation + reasoning), portfolio decision, ticker, and identity. The template changes are **additive** (new guarded sections), so existing sections are untouched and a missing identity degrades to a placeholder rather than failing.
The `research_manager` cache key is rebuilt to include ticker + identity + risk + feedback + approval (currently it is `ticker + "_research_manager"`, ignoring the other four). The research-plan cache key likewise includes identity. This is the same key rule as the `result-caching` contract — see D4.
- *Alternative considered:* re-wire the topology so the final step re-runs a risk/portfolio sub-process. Rejected — the data is already on the blackboard; re-running would double LLM cost for zero new information.

### D2 — Memory: trigger resolution on the next run *for the ticker*; reconcile the separator
- **Trigger:** resolution is wired into the orchestration at the start of a run for a ticker — a pipeline step (guarded by the existing `hasPendingEntriesFor(ticker)` condition) that runs after the ticker is on the blackboard and before the research plan. This matches the spec's "on the next run for the same ticker," reuses the ticker already in scope, and lets the resolved outcome inform `past_context` for the same run.
  - *Alternatives rejected:* `@Scheduled` (no ticker context, wrong cadence); embedding resolution in `researchManager` (too late — resolution must inform the run's past context, and it is the final step).
- **Date:** `storeFinalDecision` writes the real trading date (the decision's `TradingDate`), never `"auto"`, so the pending-match regex succeeds.
- **Alpha:** `fetchReturns` computes alpha against a real benchmark's same-window return (stock − benchmark). If benchmark data is genuinely unavailable for the window, it falls back to the raw return **and marks the entry as benchmark-less** rather than silently recording 0 alpha.
- **Separator / format:** standardize on a single separator used identically for write and parse, and Python-compatible. Recommendation: the spec's `<!-- ENTRY_END -->` (the Python-cited, prose-safe token) for new writes; the parser **also tolerates** the legacy control-char form so existing memory files keep parsing during transition. Reflection is sanitized (D5) and structurally validated before persistence.
- *Trade-off:* mid-run resolution adds latency, but only when a pending entry exists (gated) and it is required to inform `past_context` anyway.

### D3 — Alpha & benchmark data
Alpha's benchmark is fetched through the existing data-service layer (YFin/AlphaVantage) under the D4 cache contract. The benchmark symbol/universe is an open question (default candidate `SPY`); the contract only requires a real same-window benchmark return, not a specific symbol.

### D4 — Caching: one contract, one backend, no new dependency
Introduce a thin `ResultCache` contract (key canonicalization + get-or-compute + error-payload guard + category TTL) backed by the existing disk `FileCache`. All LLM and external-HTTP call sites go through it:
- **Key rules:** canonicalize symbol case; include every result-affecting input; namespace per category. Fixes the two known bad keys (research manager, research plan) and the case-inconsistent AlphaVantage keys.
- **Error guard:** a failed/rate-limited/empty payload is never persisted; only successful results are cached.
- **TTL:** time-sensitive categories (quotes) carry a configurable TTL; non-time-sensitive results are cached until manually cleared.
AlphaVantage's ad-hoc cache is replaced by `ResultCache` calls (its 8 endpoints, its dead `currDate` param, and its plaintext key layout all fold into the contract). The unlocked manual `FileCache` site (identity) moves onto the locked contract.
- *Alternatives rejected:* fix each service's cache inline (fragmentation and the missing error/TTL guard would return); add Caffeine (new dependency, and disk-backed caching is a feature here, not a bug).
- *Effect:* changing keys invalidates stale files — expected; the dirs are safe to clear.

### D5 — Sanitization: one shared utility, applied at construction sites
A shared `PromptSanitizer` (static, pure) in a common `util` package exposes `sanitizeForPrompt(String)` and neutralizes Jinja syntax (`{{ }}`/`{% %}`), HTML/script content, and control characters. It is applied at every prompt-building site (`DebateAgent` model builders, the orchestrator's plan model, the `researchManager` model, `InstrumentContextPromptContributor`) and — for the identity and memory-reflection cases — **before the disk write**, so a poisoned value cannot persist through a cache or the memory log. `DebateAgent`'s private `sanitizeValue`/`sanitizeForPrompt` delegate to it (removed, not forked).
- *Alternative rejected:* sanitize only at template-render time — rejected because the identity cache write and the memory-log write happen *before* template render and are the actual poisoning vectors.

### D6 — Scaffolding consolidation (behavior-neutral)
Extract shared behavior without changing prompts or outputs: a `ReportGenerator` template method the four analyst-report actions delegate to (common prompt-build → cache → sanitize → parse); a shared risk-debator base for the three debators (the ~48-line common debate mechanics); and a shared data-service base for the two sibling services. Locked by the existing tests asserting identical outputs; landed as pure-refactor commits separate from D1–D5.

### D7 — Config cleanup
Make the model-role → model mapping explicit and validated in `TraderAgentConfig` (log a warning when all of `cheapest`/`best`/`default-llm` collapse to one model, and support per-role overrides). This is a no-op behaviorally (same model) but makes the tiering intent explicit and overridable. The `checkpoint.enabled` default drift is deferred — see Open Questions.

### D8 — Folded-in robustness guards (surfaced by the read-only sweeps)
Three adjacent seams are folded into this change because they are small, in-cluster, and now feed the primary output:
- **Risk-debate fallback (agent-orchestration):** the transcript by construction always contains the "Aggressive (Round 1)" / "Conservative (Round N)" speaker labels, so the current substring keyword fallback classifies RISKY every time structured output fails. Decision: treat the structured risk assessment as the source of truth — on parse failure, either re-request the structured output or record an explicit undetermined/neutral level; never default RISKY from transcript keyword matching. *Why now:* D1 makes the final prompt consume `risk_level`, so a wrong fallback flows straight into the primary output.
- **Incomplete return window (decision-memory):** `fetchReturns` currently marks an entry resolved with 0% return / 0 alpha when the fetched window lacks an end-date close (holiday/weekend entry) or the data source returns an error string. Decision: validate that the window has a real end-date close before recording; on incomplete data, leave the entry pending or mark it unresolvable with a reason — never a fabricated 0. This extends D2's alpha guard to the stock-return side.
- **HITL resubmit re-injection (security):** the resubmit path re-injects raw user text as the new ticker with no format check. Decision: apply the same `^[A-Z0-9.]+$` ticker validation used by the HTMX/REST paths to the resubmit path.

**Deferred follow-ups (out of scope here):** the `TraderProposalOutput` planted BUY/SELL prose markers (needs a structured hand-off to risk/portfolio) and the `IndicatorMapper` advertised-vs-accepted vocabulary mismatch. Both are self-contained; track as separate changes.

## Risks / Trade-offs

- [Final prompt grows larger] → more tokens per final call. Inputs are already computed (no extra LLM calls); sections are kept concise; one-time recompute after key change.
- [Cache invalidation on key/separator change] → first runs after deploy recompute. Mitigation: documented; `data/llm/cache` is safe to clear; no migration.
- [Memory-format migration] → existing files may use the legacy separator. Mitigation: parser tolerates both (legacy is read-only); new writes are canonical.
- [Reflection over-scrubbing] → sanitization could strip legitimate prose. Mitigation: targeted (Jinja/HTML/control chars only), not broad PII stripping.
- [Benchmark data gaps] → some windows lack benchmark returns. Mitigation: fallback to raw return with an explicit benchmark-less marker; never silent 0.
- [Resolution mid-run latency] → added when a pending entry exists. Mitigation: gated on the condition; required to inform `past_context`.
- [Consolidation churn (D6)] → large diff. Mitigation: pure refactor, behavior-locked by existing tests, isolated commits.

## Migration Plan

No DB or schema migration; durable state is files only.
1. Deploy with the new cache keys + canonical memory separator. Stale cache files are simply unused (new keys); clear `data/llm/cache` and `data/alphavantage` if a clean slate is preferred.
2. Memory file: the parser accepts both the legacy control-char and the canonical `<!-- ENTRY_END -->` separators, so existing `~/.tradingagents/memory/trading_memory.md` keeps working; new entries use the canonical separator.
3. **Rollback:** revert the code. Cache files are additive (safe to ignore/clear) and the memory file remains readable by either parser, so rolling back leaves no orphaned state.

## Open Questions

- **`app.checkpoint.enabled` default** — the `checkpoint-resume` spec says the feature is enabled by default, but `application.yaml` sets it to `false`. Flipping it changes runtime behavior (state persistence) beyond the "behavior-neutral" scope of cluster E. Resolve before implementing D7: align to the spec (enabled) or document the override as intentional.
- **Canonical memory separator token** — confirm the exact token against the Python project's `_DECISION_RE`/`_REFLECTION_RE` (recommendation: `<!-- ENTRY_END -->`). The contract (single, prose-safe, Python-compatible, round-tripping) holds regardless.
- **Alpha benchmark symbol/universe** — confirm the benchmark (default candidate `SPY`) and whether it should be configurable per market.
