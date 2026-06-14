# Proposal: Refactor Agent Architecture

## Why

`TraderAgent` is a 784-line monolith that hardcodes the entire research pipeline inside a single `@Action` method (`executeFullResearch`), bypassing the Embabel planner entirely. This defeats the purpose of the agent framework — the planner cannot reorder, retry, or compose actions, and the code is unmaintainable. The same pattern repeats in `RiskDebateService`, a `@Component` with a manual for-loop instead of a proper agent.

## What Changes

- **Decompose `TraderAgent` (784 lines) into three focused agents:**
  - `OrchestratorAgent` — user input, research plan generation, HITL checkpoint, subagent invocation
  - `DebateAgent` — report generation, debate orchestration, risk debate, HITL review, final plan
  - `DebateLoopAgent` — iterative bull/bear debate (own planner)
- **Extract `RiskDebateService` → `RiskDebateAgent`** with its own planner and isolated blackboard
- **Replace manual Flux streaming** with Embabel's synchronous `createObject()` API (1 line vs 15 lines per LLM call)
- **Replace manual HITL form handling** (`FormBindingRequest`/`FormSubmission`) with `WaitFor.formSubmission()` (native Embabel HITL)
- **Remove `TraderAgent` and `RiskDebateService`** entirely — no backwards compatibility
- **Simplify controllers** — remove "travel" naming artifacts, reduce duplicate HITL resume logic

## Capabilities

### New Capabilities

- `agent-orchestration` — Multi-agent decomposition with `asSubProcess` for isolated blackboard communication between Orchestrator, Debate, DebateLoop, and RiskDebate agents
- `synchronous-llm-calls` — All LLM interactions use Embabel's `createObject()` synchronous API; no manual Flux/StreamingPromptRunner handling
- `native-hitl` — All human-in-the-loop checkpoints use `WaitFor.formSubmission()` instead of manual FormBindingRequest/FormSubmission handling

### Modified Capabilities

<!-- No existing specs in openspec/specs/ — nothing to modify -->

## Impact

- **Deleted:** `TraderAgent.java` (784 lines), `RiskDebateService.java`
- **Created:** `OrchestratorAgent.java`, `DebateAgent.java`, `DebateLoopAgent.java`, `RiskDebateAgent.java`
- **Modified:** `TradingHtmxController.java` (remove "travel" naming), `ProcessStatusController.java` (simplify HITL), `HitlService.java` (remove manual form handling), `TraderAgentConfig.java` (wire new agents), `GekkoApplication.java` (exclude old agent)
- **Unchanged:** `Analysts.java`, `MarketDataTools.java`, `FundamentalDataTools.java`, `NewsDataTools.java`, `AlphaVantageService.java`, `YFinService.java`, `FileCache.java`, `DateUtils.java`, `IndicatorMapper.java`, `VendorRouter.java`, all Jinja prompt templates
- **BREAKING:** HTTP endpoints and blackboard types may change — no backwards compatibility
