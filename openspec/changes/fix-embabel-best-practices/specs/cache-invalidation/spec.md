# Spec: Cache Invalidation in Debate Loop

## Why
`DebateLoopAgent` uses `FileCache.getOrCompute()` for each debate turn with keys like `AAPL_debate_0_bull`. The Critic agent identified that cached debate responses are stale because each iteration's history has changed — the LLM should see the updated history and generate a fresh argument. Caching prevents this.

## What Changes
- Remove `FileCache.getOrCompute()` calls from `DebateLoopAgent.debate()`
- Each debate turn should call `bullResearcher.argue()` and `bearResearcher.argue()` directly (no caching)
- The `RepeatUntilBuilder` already provides iteration isolation — caching at the per-iteration level defeats the purpose

## Acceptance Criteria
- [ ] `DebateLoopAgent.debate()` calls `bullResearcher.argue()` and `bearResearcher.argue()` directly without `FileCache.getOrCompute()`
- [ ] Each iteration generates fresh arguments based on updated history
- [ ] Debate convergence still works (Jaccard similarity threshold check unchanged)
- [ ] Build passes (`./mvnw verify`)
- [ ] Existing tests still pass (especially `DebateLoopAgentTest`)