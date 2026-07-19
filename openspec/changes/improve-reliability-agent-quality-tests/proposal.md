## Why

The TradingAgents project has accumulated several known issues that undermine its reliability, agent quality, and engineering rigor. The debate system runs a fixed number of iterations with no convergence check, critical cache key bugs cause stale data to be served, the risk debate system is dead code, and the core agent logic has zero test coverage. These issues make the system unreliable for its primary purpose — producing trustworthy investment research — and make future refactoring risky.

## Status

**Phase 1 (Reliability fixes): COMPLETE** — cache keys, timeouts, FileCache race condition all fixed.
**Phase 2 (Agent quality): PARTIALLY COMPLETE** — debate convergence implemented, MarketDataTools created, .jinja prompts unified. Risk debate system wired up as RiskDebateAgent.
**Phase 3 (Test coverage): IN PROGRESS** — 437 tests passing. This update expands test coverage with integration tests for agents that previously had none.

## What Changes

### Already Implemented (Phases 1-2)
- ✅ **Cache key bugs fixed** — `getGlobalNews()` includes `dateFrom`/`dateTo`, `getInsiderSentiment()` includes all params
- ✅ **RestTemplate timeouts** — `readTimeoutMs` configured with 30s default
- ✅ **FileCache race condition fixed** — per-key locking with `ConcurrentHashMap.computeIfAbsent()` double-check
- ✅ **Debate convergence** — Jaccard bigram similarity with configurable threshold (default 0.8), max iterations (default 5)
- ✅ **Risk debate system wired** — `RiskDebateAgent` with 3 debators + judge, integrated into DebateAgent pipeline
- ✅ **MarketAnalyst tools** — `MarketDataTools` with `get_stock_data` and `get_indicators`
- ✅ **Prompt file extensions unified** — all `.txt` renamed to `.jinja`

### New: Expanded Test Coverage (Phase 3)
- **Integration tests for agents with zero coverage**: PortfolioManager, InstrumentIdentityAgent, InstrumentContextPromptContributor, RiskDebateAgent
- **Integration tests for sub-process delegation**: OrchestratorAgent.executeDebate, DebateAgent.runDebate/runTrader/runRiskDebate/runPortfolioManager
- **Integration tests for DebateLoopAgent debate() with LLM calls**
- **Integration tests for MarketDataTools**
- **Unit tests for edge cases**: DebateAgent helpers (sanitizeForPrompt, extractRating, etc.), DebateLoopAgent convergence in practice

## Capabilities

### New Capabilities
- `reliability-fixes`: Fix cache key bugs, add HTTP timeouts, fix FileCache race condition
- `agent-quality`: Add debate convergence, wire up risk debate, enable MarketAnalyst tools
- `test-coverage`: Expand unit and integration test suite to cover core agent logic

### Modified Capabilities
- `test-coverage`: Expanded to include integration tests for sub-process delegation, HITL checkpoints, and agents with zero prior coverage

## Impact

- **Code**: `AlphaVantageService.java`, `FileCache.java`, `TraderAgent.java`, `BullResearcher.java`, `BearResearcher.java`, `YFinService.java`, `VendorRouter.java`
- **Prompts**: `prompts/analysts/*.txt` → `.jinja`, risk debate prompts wired or removed
- **Tests**: New test files for PortfolioManager, InstrumentIdentityAgent, RiskDebateAgent, OrchestratorAgent delegation, DebateAgent sub-processes, DebateLoopAgent LLM tests, MarketDataTools, DebateAgent helpers
- **Dependencies**: No new external dependencies required
