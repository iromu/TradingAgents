---
title: "Risk Debate"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "src/main/java/com/embabel/gekko/agent/RiskDebateAgent.java"
  - "src/main/java/com/embabel/gekko/agent/risk/AggressiveDebator.java"
  - "src/main/java/com/embabel/gekko/agent/risk/ConservativeDebator.java"
  - "src/main/java/com/embabel/gekko/agent/risk/NeutralDebator.java"
  - "src/main/java/com/embabel/gekko/agent/RiskAssessment.java"
  - "src/main/java/com/embabel/gekko/agent/RiskLevel.java"
  - "src/main/resources/prompts/risk/"
  - "src/main/resources/prompts/managers/RiskManager.jinja"
updated_at: "2026-08-13"
---

# Risk Debate

The risk debate evaluates a trader proposal from multiple risk perspectives using `RiskDebateAgent`. It produces a `RiskAssessment` with a `RiskLevel` (RISKY, NEUTRAL, or CONSERVATIVE).

## The Three Risk Debators

| Debator | Stance |
|---------|--------|
| **Aggressive** | Argues for taking bigger positions, accepting higher risk |
| **Conservative** | Argues for caution, smaller positions, or avoiding the trade |
| **Neutral** | Provides an unbiased assessment of the risks |

## How It Works

1. `RiskDebateAgent.assessRisk()` runs 3 rounds of debate (fixed: `MAX_RISK_DEBATE_ROUNDS = 3`)
2. In each round, debators speak in round-robin order: Aggressive → Conservative → Neutral
3. Each debator sees the full history and the other two debators' current responses
4. After 3 rounds, `RiskManager.jinja` judges the debate output
5. The result is parsed into a `RiskAssessment(RiskLevel, reasoning)`

### Structured Output with Fallback

The risk judge first tries to parse structured output (`RiskAssessmentOutput` record). If that fails, it falls back to keyword-based classification:

- **RISKY:** Contains "high risk", "risky", "aggressive", "bold move", "strong buy", or "overweight"
- **CONSERVATIVE:** Contains "low risk", "conservative", "safe", "avoid", "cautious", or "underweight"
- **NEUTRAL:** Default

### Response Formatting

Debate responses are now formatted with round numbers (e.g., "Aggressive (Round 1):") for better context when the LLM judges the debate output. Null parameters are rejected early via `Objects.requireNonNull()`.

## Input

The risk debate receives:
- `ticker` — the stock symbol
- `briefs` — distilled analyst reports (fundamentals, market, news, social)
- `debateState` — the investment debate history
- `traderProposal` — the trader's transaction proposal

## Output

The risk debate produces:

| Field | Type | Description |
|-------|------|-------------|
| `level` | `RiskLevel` | RISKY, NEUTRAL, or CONSERVATIVE |
| `reasoning` | `String` | LLM's explanation, truncated to 200 chars |

## When It Runs

The risk debate runs after the investment debate and trader proposal, as part of the full research pipeline orchestrated by `DebateAgent`.

## Prompt Templates

| Agent | Prompt file |
|-------|-------------|
| Aggressive | `prompts/risk/AggressiveDebator.jinja` |
| Conservative | `prompts/risk/ConservativeDebator.jinja` |
| Neutral | `prompts/risk/NeutralDebator.jinja` |
| Manager | `prompts/managers/RiskManager.jinja` |