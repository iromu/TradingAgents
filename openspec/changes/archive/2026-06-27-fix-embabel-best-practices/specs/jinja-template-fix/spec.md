# Spec: Jinja Template Fix

## Why
`RiskManager.jinja` uses single-brace `{trader_decision}` instead of double-brace `{{ trader_decision }}` Jinja variable syntax. The LLM prompt renders the literal text `{trader_decision}` instead of the actual trader proposal value, degrading the risk judge's ability to refine the trader's plan.

## What Changes
- Fix `RiskManager.jinja` line 11: change `**{trader_decision}**` to `**{{ trader_decision }}**`
- Verify no other single-brace Jinja variables exist in any prompt template

## Acceptance Criteria
- [ ] `RiskManager.jinja` uses `{{ trader_decision }}` (double braces)
- [ ] All 16 `.jinja` files in `src/main/resources/prompts/` use correct Jinja syntax
- [ ] No single-brace `{variable}` patterns exist (only `{{ variable }}` and `{% tag %}`)
- [ ] The variable `trader_decision` is correctly passed in the model map from `RiskDebateAgent.judgeRisk()` (already correct — `Map.entry("trader_decision", traderProposal)`)
- [ ] Build passes (`./mvnw verify`)