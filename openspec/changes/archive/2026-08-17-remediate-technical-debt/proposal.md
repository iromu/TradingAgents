## Why

The TradingAgents pipeline has accumulated "silent" technical debt: features that pass their own tests but are open-loop or ineffective end-to-end, hand-rolled parsers that misbehave on real LLM/external output, and fragmented caching that serves stale or error results. The most consequential is the system's primary output — the final `InvestmentPlan`. A read-only sweep of the codebase (Aug 2026) confirmed that the final decision step generates its prompt from debate history alone, silently dropping the risk assessment, portfolio decision, and instrument identity that earlier stages compute at real LLM cost, plus five further debt clusters. We remediate them now so the output reflects the full pipeline and the supporting features actually close their loops.

## What Changes

- **Final-step funnel (A):** The `researchManager` prompt now consumes the `RiskAssessment`, the portfolio decision, and the instrument identity — not only debate history and user feedback. Research-plan and research-manager cache keys include all result-affecting inputs, so changed input never serves a stale cached plan. The risk-debate step is made robust to structured-output failure so the risk level it feeds the prompt is not defaulted to RISKY by keyword matching on the (always "Aggressive"/"Conservative") transcript.
- **Decision-memory loop (B):** Closes four open seams — pending entries are written with matchable dates so `hasPendingEntriesFor` works; resolution is actually triggered on the next run; `generatePastContext` is injected into the final decision prompt (not only the research plan); LLM reflection is sanitized/validated before persistence. Alpha is computed against a real benchmark instead of `rawReturn`. The read/write entry format is made internally consistent. An incomplete price window is never silently recorded as a 0% return / 0 alpha.
- **Result caching (C):** A single caching contract — cache keys cover all result-affecting inputs (case-normalized), error/rate-limit payloads are never cached, and time-sensitive results carry a TTL/eviction policy. AlphaVantage's ad-hoc cache is brought under the same contract.
- **Parsing & sanitization (D):** `sanitizeValue`/`sanitizeForPrompt` move to a shared utility applied at every prompt-building site; the REST ticker path enforces the same format validation as the HTMX path; the same ticker validation is applied to the HITL resubmit path; external/user/identity text is sanitized before injection.
- **Scaffolding & config (E):** Consolidate the four analyst-report generators, three risk debators, and two sibling data services; clean up no-op config (model-role tiering that points all roles at one model; checkpoint default). *Behavior-neutral — no spec delta.*

**BREAKING:** none. The final-plan prompt gains inputs (content changes, contract does not). Cache-behavior fixes may invalidate stale cache files — expected and safe to clear.

## Capabilities

### New Capabilities
- `result-caching`: A single result-caching contract for the whole pipeline — cache keys cover all result-affecting inputs and are case-normalized; error/rate-limit payloads MUST NOT be cached; time-sensitive results carry a TTL/eviction policy; one backend for LLM and external-HTTP results.

### Modified Capabilities
- `agent-orchestration`: The final `InvestmentPlan` (produced by `researchManager`) is generated from a prompt that consumes the risk assessment, portfolio decision, and instrument identity, not only debate history and user feedback; research-plan and research-manager cache keys include all result-affecting inputs.
- `decision-memory`: Pending entries are written with matchable dates and are actually resolved on the next run; `generatePastContext` is injected into the final decision prompt; reflection is sanitized before persistence; alpha is computed against a real benchmark; the read/write entry format is made consistent.
- `security`: Sanitization is a shared utility applied at every prompt-building site; the REST ticker path enforces the same format validation as the HTMX path; external/user/identity text is sanitized before prompt injection.
- `instrument-identity`: Resolved identity metadata is sanitized before prompt injection and before it is written to the on-disk cache.

## Impact

- **Code:** `DebateAgent` (researchManager/model, 4 report generators), `OrchestratorAgent` (plan model, memory wiring), `DebateLoopAgent`, `RiskDebateAgent` + the 3 debators, `DecisionMemoryAgent`/`DecisionMemoryRepository` (date matching, resolve trigger, reflection, alpha, format), `FileCache` + `AlphaVantageService` + `YFinService` (caching contract), a new shared sanitizer and its call sites, `TradingApiController` (ticker validation), `InstrumentContextPromptContributor` (sanitization).
- **Prompts:** `managers/ResearchManager.jinja` (add risk/portfolio/identity/ticker slots); the research-plan template already carries identity.
- **Config:** `application.yaml` (model-role mapping, cache TTL policy, checkpoint default), `TraderAgentConfig`.
- **Data:** `data/llm/cache`, `data/alphavantage`, `~/.tradingagents/memory/trading_memory.md` — the entry format must stay Python-compatible (see design).
- **Tests:** Existing tests that assert the broken state (e.g. `past_memory_str == "No past memories found."`, report/plan cache-key shape) must be updated; new tests for cache-key completeness, error-payload non-caching, reflection sanitization, and final-prompt input consumption.
