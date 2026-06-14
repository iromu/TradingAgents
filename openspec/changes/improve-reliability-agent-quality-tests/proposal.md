## Why

The TradingAgents project has accumulated several known issues that undermine its reliability, agent quality, and engineering rigor. The debate system runs a fixed number of iterations with no convergence check, critical cache key bugs cause stale data to be served, the risk debate system is dead code, and the core agent logic has zero test coverage. These issues make the system unreliable for its primary purpose — producing trustworthy investment research — and make future refactoring risky.

## What Changes

- **Fix cache key bugs** in `AlphaVantageService.getGlobalNews()` and `getInsiderSentiment()` to include all query parameters in cache keys
- **Add RestTemplate timeouts** to prevent indefinite blocking on API calls
- **Fix FileCache race condition** in `getOrCompute()` to prevent duplicate computation under concurrent access
- **Add debate convergence check** to replace fixed 2-iteration loop with a condition that detects when the debate has stabilized or reached agreement
- **Wire up the risk debate system** (RiskManager, AggresiveDebator, ConservativeDebator, NeutralDebator prompts) or remove dead prompt files
- **Enable MarketAnalyst tool calls** by uncommenting and wiring `get_stock_data,get_indicators` tools
- **Expand test coverage** to include TraderAgent core actions, BullResearcher, BearResearcher, YFinService, VendorRouter, and integration tests for the full pipeline
- **Unify prompt file extensions** — rename all `.txt` analyst prompts to `.jinja` for consistency

## Capabilities

### New Capabilities
- `reliability-fixes`: Fix cache key bugs, add HTTP timeouts, fix FileCache race condition
- `agent-quality`: Add debate convergence, wire up risk debate, enable MarketAnalyst tools
- `test-coverage`: Expand unit and integration test suite to cover core agent logic

### Modified Capabilities
<!-- No existing capabilities in openspec/specs/ — all new -->

## Impact

- **Code**: `AlphaVantageService.java`, `FileCache.java`, `TraderAgent.java`, `BullResearcher.java`, `BearResearcher.java`, `YFinService.java`, `VendorRouter.java`
- **Prompts**: `prompts/analysts/*.txt` → `.jinja`, risk debate prompts wired or removed
- **Tests**: New test files for core agents and services
- **Dependencies**: No new external dependencies required
